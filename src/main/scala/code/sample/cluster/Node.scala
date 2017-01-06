package code.sample.cluster

import akka.Done
import akka.actor.{Actor, ActorRef, Terminated}
import akka.event.Logging
import code.sample.cluster.NodeActions.{ActiveNodes, GetNodeInfo, NewTickDelay, SendTick, Tick}
import com.codahale.metrics.MetricRegistry

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Random

object NodeActions {
  case object Tick
  case object SendTick

  case object GetNodeInfo

  case class ActiveNodes(nodes: Set[ActorRef])
  case class NewTickDelay(tickDelay: FiniteDuration)
}

sealed trait NodeStatus
object NodeStatus {
  case object Active extends NodeStatus
}

case class NodeInfo(id: Int,
                    status: NodeStatus,
                    tickDelay: FiniteDuration,
                    oneMinuteRate: Double,
                    meanRate: Double,
                    availableNodes: Seq[String])

class Node(id: Int, var tickDelay: FiniteDuration, registry: MetricRegistry) extends Actor {
  val log = Logging(context.system, this)
  val tickMeterName = s"${self.path.name}.ticks"
  val tickMeter = registry.meter(tickMeterName)

  import context.dispatcher

  var tickSending = context.system.scheduler.schedule(Duration.Zero, tickDelay, self, SendTick)
  def restartTickSending() = {
    tickSending.cancel()
    tickSending = context.system.scheduler.schedule(Duration.Zero, tickDelay, self, SendTick)
  }

  override def postStop(): Unit = {
    registry.remove(tickMeterName)
    tickSending.cancel()
  }

  var othersNodes = Set.empty[ActorRef]

  override def receive: Receive = {

    case Terminated(node) => othersNodes -= node

    case NewTickDelay(newTickDelay) =>
      tickDelay = newTickDelay
      restartTickSending()

    case ActiveNodes(nodes) =>
      val withoutSelf = nodes - self
      withoutSelf.foreach(context.watch)
      othersNodes ++= withoutSelf

    case SendTick =>
      if (othersNodes.nonEmpty) othersNodes.toList(Random.nextInt(othersNodes.size)) ! Tick

    case Tick =>
      tickMeter.mark()
      log.info("tick received")

    case GetNodeInfo => sender() ! NodeInfo(
      id,
      NodeStatus.Active,
      tickDelay,
      tickMeter.getOneMinuteRate,
      tickMeter.getMeanRate,
      othersNodes.map(_.path.name).toList.sorted
    )

  }

}