package bench.Polyglot

import scala.language.postfixOps
import org.openjdk.jmh.annotations._
import java.util.concurrent.TimeUnit
import org.graalvm.polyglot._
import org.graalvm.polyglot.io.ByteSequence
import java.nio.file.Files
import java.nio.file.Path
import AgentBench._

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@Fork(1)
@Threads(1)
@Warmup(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS, batchSize = 1)
@Measurement(
  iterations = 10,
  time = 15,
  timeUnit = TimeUnit.SECONDS,
  batchSize = 1
)
class AgentBenchJMH {
  private var noop = Files.readAllBytes(
    Path.of("/workspaces/playground/wasm-latency/noop.wasm")
  );

  val sources: Seq[Source] = Seq(
    // Source
    //   .newBuilder("js", "export const foo = () => 0;", "noop.js")
    //   .mimeType("application/javascript+module")
    //   .build(),
    Source.newBuilder("wasm", ByteSequence.create(noop), "noop.wasm").build()
  )

  @Benchmark
  @OperationsPerInvocation(1_000 * 10)
  def agentBench(): Unit = {
    sources.foreach(source =>
      bench(
        source,
        numOfAgent = 1_000,
        numOfRequest = 1_000,
        numOfWarmup = 10
      )
    )
  }
}
