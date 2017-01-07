package code.sample

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import code.sample.cluster.{ClusterManager, ClusterManagerActions}
import code.sample.restapi.RestApi

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object ClusterEmulationApp extends App with JmxMetrics with Config {

  implicit val system = ActorSystem("cluster-emulation-app")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val clusterManager = system.actorOf(Props(classOf[ClusterManager], tickDuration, registry), "cluster-manager")
  clusterManager ! ClusterManagerActions.AddNodes(nodesNumber)

  Http().bindAndHandle(RestApi(clusterManager), host, port)

  Await.result(system.whenTerminated, Duration.Inf)
}
