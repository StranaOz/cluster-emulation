package code.sample.cluster

import akka.actor.{Actor, ActorRef, Terminated}
import akka.event.Logging
import code.sample.cluster.NodeActions.{GetNodeInfo, ActiveNodes, SendTick, Tick}
import com.codahale.metrics.MetricRegistry

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Random

object NodeActions {
  case object Tick
  case object SendTick
  case class ActiveNodes(nodes: Set[ActorRef])
  case object GetNodeInfo
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

class Node(id: Int, tickDelay: FiniteDuration, registry: MetricRegistry) extends Actor {
  val log = Logging(context.system, this)
  val tickMeter = registry.meter(s"${self.path.name}.ticks")

  import context.dispatcher

  context.system.scheduler.schedule(Duration.Zero, tickDelay, self, SendTick)

  var othersNodes = Set.empty[ActorRef]

  override def receive: Receive = {

    case Terminated(node) => othersNodes -= node

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