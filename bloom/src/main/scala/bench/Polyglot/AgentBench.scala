package bench.Polyglot

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import akka.Done
import org.graalvm.polyglot._
import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.util.Timeout
import scala.util.{Try, Success, Failure}
import scala.language.postfixOps
import akka.util.Timeout
import scala.concurrent.{Await, ExecutionContext, Future}
import akka.actor.typed.scaladsl.AskPattern._
import scala.collection.mutable.ArrayBuffer
import org.graalvm.polyglot.io.ByteSequence
import java.nio.file.Files
import java.nio.file.Path
import java.io.File

object Agent {
  sealed trait Event
  case class Ready(replyTo: ActorRef[Request]) extends Event
  case object Completed extends Event
  sealed trait Request
  final case class Execute(times: Int) extends Request { assert(times > 0) }
  private case object Execute extends Request

  private val engine = Engine
    .newBuilder()
    .option("engine.Mode", "throughput")
    .build()

  def apply(
      replyTo: ActorRef[Event],
      source: Source
  ): Behavior[Request] =
    Behaviors.setup(ctx =>
      val polyCtxBuilder = Context
        .newBuilder()
        .engine(engine)

      val polyCtx = polyCtxBuilder
        .allowExperimentalOptions(true)
        .option("js.esm-eval-returns-exports", "true")
        .allowIO(true)
        .build()

      val sourceAst = polyCtx.parse(source)
      val exports = sourceAst.execute()

      val executable =
        if source.getLanguage() == "js" then exports.getMember("foo");
        else polyCtx.getBindings("wasm").getMember("main").getMember("foo");

      replyTo ! Ready(ctx.self)

      new Agent(ctx, replyTo, executable).ready
    )
}
class Agent(
    ctx: ActorContext[Agent.Request],
    replyTo: ActorRef[Agent.Event],
    executable: Value
) {
  import Agent._

  private def ready: Behavior[Request] = {
    Behaviors.receiveMessagePartial { case Execute(times) =>
      ctx.self ! Execute
      active(times)
    }
  }

  private def active(left: Int): Behavior[Request] =
    Behaviors.receiveMessagePartial { case Execute =>
      if (left > 0)
        executable.execute()
        ctx.self ! Execute
        active(left - 1)
      else
        replyTo ! Completed; ready
    }
}

object AgentBench {
  sealed trait Request
  case object Shutdown extends Request
  final case class Execute(times: Int) extends Request { assert(times > 0) }
  private final case class AdaptedAgentEvent(res: Agent.Event) extends Request

  def printProgress(
      numOfAgent: Int,
      totalTimesExecuted: Int,
      startNanoTime: Long
  ): Unit = {
    val durationMicros = (System.nanoTime() - startNanoTime) / 1000

    println(
      f"  $totalTimesExecuted times executed by $numOfAgent Agent took ${durationMicros} us, " +
        f"${totalTimesExecuted.toDouble / durationMicros}%,.2f M event-reaction/sec"
    )
  }

  def apply(
      source: Source,
      numOfAgent: Int
  ): Behavior[Request] =
    Behaviors.withStash(100) { buffer =>
      Behaviors.setup[Request] { ctx =>
        val agentEventAdapter: ActorRef[Agent.Event] =
          ctx.messageAdapter(AdaptedAgentEvent.apply)

        (1 to numOfAgent).foreach(n =>
          ctx.spawnAnonymous(Agent(agentEventAdapter, source))
        )

        new AgentBench(ctx, buffer, numOfAgent).starting
      }
    }

  def bench(
      source: Source,
      numOfAgent: Int,
      numOfRequest: Int,
      numOfWarmup: Int
  ): Future[Done] = {
    if numOfRequest % numOfAgent != 0 then
      throw Error("numOfRequest should be evenly distributed among numOfAgent")

    println(source.getName)

    val system = ActorSystem(AgentBench(source, numOfAgent), "bench")

    (1 to numOfWarmup).foreach(n => system ! Execute(numOfRequest))

    system ! Shutdown

    val timeout = 30.minutes
    given askTimeout: Timeout = Timeout(timeout)

    Await.ready(system.whenTerminated, timeout)
  }
}
private class AgentBench(
    ctx: ActorContext[AgentBench.Request],
    buffer: StashBuffer[AgentBench.Request],
    numOfAgent: Int
) {
  import AgentBench._

  private val readyAgents = ArrayBuffer[ActorRef[Agent.Request]]()
  private def starting: Behavior[Request] =
    Behaviors.receiveMessage {
      case AdaptedAgentEvent(Agent.Ready(ref)) =>
        readyAgents += ref
        if readyAgents.length == numOfAgent then buffer.unstashAll(ready)
        else Behaviors.same
      case other => buffer.stash(other); Behaviors.same
    }

  private def ready: Behavior[Request] =
    Behaviors.receiveMessagePartial {
      case Execute(times) =>
        readyAgents.foreach(agent =>
          agent ! new Agent.Execute(times / numOfAgent)
        )
        waitForCompletion(System.nanoTime(), times)
      case Shutdown => Behaviors.stopped
    }

  private def waitForCompletion(
      startedAt: Long,
      totalTimesExecute: Int
  ): Behavior[Request] =
    var completedAgents = 0
    Behaviors.receiveMessage {
      case AdaptedAgentEvent(Agent.Completed) =>
        completedAgents += 1
        if completedAgents == numOfAgent then
          printProgress(numOfAgent, totalTimesExecute, startedAt)
          buffer.unstashAll(ready)
        else Behaviors.same
      case other => buffer.stash(other); Behaviors.same
    }
}
