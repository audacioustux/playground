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
@Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS, batchSize = 1)
@Measurement(
  iterations = 2,
  time = 10,
  timeUnit = TimeUnit.SECONDS,
  batchSize = 1
)
class AgentBenchJMH {
  @Benchmark
  @OperationsPerInvocation(1000)
  def jsAgentBench(): Unit = {
    val source = Source
      .newBuilder("js", "export const foo = () => 0;", "noop.js")
      .mimeType("application/javascript+module")
      .build()

    bench(
      source,
      numOfAgent = 1_000,
      numOfRequest = 1_000,
      numOfWarmup = 20
    )
  }

  @Benchmark
  @OperationsPerInvocation(1000)
  def wasmAgentBench(): Unit = {
    var noop = Files.readAllBytes(
      Path.of("/workspaces/playground/wasm-latency/noop.wasm")
    );

    val source =
      Source.newBuilder("wasm", ByteSequence.create(noop), "noop.wasm").build()

    bench(
      source,
      numOfAgent = 1_000,
      numOfRequest = 1_000,
      numOfWarmup = 20
    )
  }
}
