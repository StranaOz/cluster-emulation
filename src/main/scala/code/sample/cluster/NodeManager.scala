package code.sample.cluster

import akka.Done
import akka.actor.{Actor, ActorRef, Props}
import code.sample.cluster.NodeManagerActions.{ActiveNodes, AddNodes, ChangeTickDelay, GetActiveNodes, StopNode}
import com.codahale.metrics.MetricRegistry
import akka.pattern._
import akka.util.Timeout

import concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

object NodeManagerActions {
  case class AddNodes(count: Int)
  case class StopNode(id: Int)

  case class ChangeTickDelay(newTickDelay: FiniteDuration)

  case object GetActiveNodes
  case class ActiveNodes(nodes: Iterable[NodeInfo])
}

class NodeManager(var tickDelay: FiniteDuration, registry: MetricRegistry) extends Actor {

  implicit val to = Timeout(2 seconds)
  import context.dispatcher

  var id2ActiveNode = Map.empty[Int, ActorRef]
  var lastId = 0

  override def receive: Receive = {

    case AddNodes(count) =>
      val id2NewNode = (lastId + 1 to (lastId + count))
        .map(id => id -> context.actorOf(Props(classOf[Node], id, tickDelay, registry), s"node-$id")).toMap

      id2ActiveNode ++= id2NewNode
      id2ActiveNode.values.foreach(_ ! NodeActions.ActiveNodes(id2ActiveNode.values.toSet))
      lastId += count
      sender() ! Done

    case GetActiveNodes => Future.sequence(
      id2ActiveNode.values.map(node => (node ? NodeActions.GetNodeInfo).mapTo[NodeInfo])
    ).map(nodes => ActiveNodes(nodes.toList.sortBy(_.id))) pipeTo sender()

    case StopNode(id) => sender ! (id2ActiveNode.get(id) match {
      case None => None
      case Some(node) =>
        context.stop(node)
        id2ActiveNode = id2ActiveNode.filterKeys(_ != id)
        Some(Done)
    })

    case ChangeTickDelay(newTickDelay) =>
      tickDelay = newTickDelay
      id2ActiveNode.values.foreach(_ ! NodeActions.NewTickDelay(newTickDelay))
      sender() ! Done

  }

}
