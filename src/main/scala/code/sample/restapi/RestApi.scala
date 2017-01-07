package code.sample.restapi

import akka.Done
import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.pattern._
import akka.util.Timeout
import code.sample.cluster.ClusterManagerActions
import spray.json.JsValue

import scala.concurrent.duration._
import scala.language.postfixOps

object RestApi extends JsonSupport {

  implicit val timeout = Timeout(2 seconds)

  def apply(clusterManger: ActorRef) = {
    val nodesRout = path("cluster" / "nodes") {
      get {
        onSuccess(clusterManger ? ClusterManagerActions.GetActiveNodes) {
          case nodes: ClusterManagerActions.ActiveNodes => complete(nodes)
        }
      } ~ post {
        formFields('count.as[Int]) { case count =>
          validate(count > 0, "You can add only positive numbers of nodes to the cluster") {
            onSuccess(clusterManger ? ClusterManagerActions.AddNodes(count)) {
              case Done => complete(StatusCodes.NoContent)
            }
          }
        }
      }
    } ~ path("cluster" / "nodes" / IntNumber) { id =>
      delete {
        onSuccess(clusterManger ? ClusterManagerActions.StopNode(id)) {
          case None => complete(StatusCodes.NotFound)
          case Some(Done) => complete(StatusCodes.NoContent)
        }
      }
    }

    val clusterRout = path("cluster") {
      patch {
        entity(as[JsValue]) { json =>
          val milliseconds = json.asJsObject.fields("tick-duration").convertTo[Long]
          validate(milliseconds > 0, "tick-duration must be positive") {
            onSuccess(clusterManger ? ClusterManagerActions.ChangeTickDelay(FiniteDuration(milliseconds, MILLISECONDS))) {
              case Done => complete(StatusCodes.NoContent)
            }
          }
        }
      }
    }

    nodesRout ~ clusterRout
  }

}
