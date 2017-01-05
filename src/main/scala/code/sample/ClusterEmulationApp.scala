package code.sample

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object ClusterEmulationApp extends App with JmxMetrics {

  implicit val system = ActorSystem("cluster-emulation-app")
  val conf = ConfigFactory.load().getConfig("cluster")

  val tickDuration = conf.as[FiniteDuration]("tick-duration")
  val nodesNumber = conf.getInt("nodes-number")

  val nodeManager = system.actorOf(Props(classOf[NodeManager], tickDuration, registry), "node-manager")
  nodeManager ! NodeManagerActions.AddNodes(nodesNumber)

  Await.result(system.whenTerminated, Duration.Inf)
}
