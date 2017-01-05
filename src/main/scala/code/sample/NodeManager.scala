package code.sample

import akka.actor.{Actor, ActorRef, Props}
import code.sample.NodeManagerActions.{AddNodes, GetActiveNodes}
import com.codahale.metrics.MetricRegistry

import scala.concurrent.duration.FiniteDuration

object NodeManagerActions {
  case class AddNodes(count: Int)
  case class StopNode(id: Int)

  case object GetActiveNodes
  case class ActiveNodes(nodes: Iterable[ActorRef])
}

class NodeManager(sendTickEvery: FiniteDuration, registry: MetricRegistry) extends Actor {

  var id2ActiveNode = Map.empty[Int, ActorRef]
  var lastId = 1

  override def receive: Receive = {

    case AddNodes(count) =>
      val id2NewNode = (lastId to (lastId + count))
        .map(id => id -> context.actorOf(Props(classOf[Node], sendTickEvery, registry), s"node-$id")).toMap

      id2ActiveNode ++= id2NewNode
      id2ActiveNode.values.foreach(_ ! NodeActions.NewNodes(id2NewNode.values.toSet))
      lastId += count

    case GetActiveNodes => sender() ! id2ActiveNode.values
  }
}
