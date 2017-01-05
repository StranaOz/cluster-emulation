package code.sample

import akka.actor.{Actor, ActorRef, Terminated}
import akka.event.Logging
import code.sample.NodeActions.{NewNodes, SendTick, Tick}
import com.codahale.metrics.MetricRegistry

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Random

object NodeActions {
  case object Tick
  case object SendTick
  case class NewNodes(nodes: Set[ActorRef])
}

class Node(sendEvery: FiniteDuration, registry: MetricRegistry) extends Actor {
  val log = Logging(context.system, this)
  val tickMeter = registry.meter(s"${self.path.name}.ticks")

  import context.dispatcher

  context.system.scheduler.schedule(Duration.Zero, sendEvery, self, SendTick)

  var othersNodes = Set.empty[ActorRef]

  override def receive: Receive = {

    case Terminated(node) => othersNodes -= node

    case NewNodes(nodes) =>
      val withoutSelf = nodes - self
      withoutSelf.foreach(context.watch)
      othersNodes ++= withoutSelf

    case SendTick =>
      if (othersNodes.nonEmpty) othersNodes.toList(Random.nextInt(othersNodes.size)) ! Tick

    case Tick =>
      tickMeter.mark()
      log.info("tick received")

  }

}