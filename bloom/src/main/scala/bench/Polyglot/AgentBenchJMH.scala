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
  def wasmAgentBench(): Unit = {
    var noop = Files.readAllBytes(
      Path.of("../wasm-latency/noop.wasm")
    );

    val source = Source.newBuilder("wasm", ByteSequence.create(noop), "noop.wasm").build()

    bench(
      source,
      numOfAgent = 1,
      numOfRequest = 1_000,
      numOfWarmup = 20
    )
  }

  @Benchmark
  def jsAgentBench(): Unit = {
    val source = Source
      .newBuilder("js", "export const foo = (n) => n + 1;", "noop.js")
      .mimeType("application/javascript+module")
      .build()

    bench(
      source,
      numOfAgent = 1,
      numOfRequest = 1_000,
      numOfWarmup = 20
    )
  }
}
