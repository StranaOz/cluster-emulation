package code.sample.restapi

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import code.sample.cluster.ClusterManagerActions.ActiveNodes
import code.sample.cluster.{NodeInfo, NodeStatus}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat, deserializationError}

import scala.concurrent.duration.FiniteDuration

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object NodeStatusJsonFormat extends RootJsonFormat[NodeStatus] {
    val status2Str: Map[NodeStatus, String] = Map(
      NodeStatus.Active -> "active"
    )

    def write(obj: NodeStatus) = JsString(status2Str(obj))
    def read(value: JsValue) = deserializationError("not implemented")
  }

  implicit object FiniteDurationJsonFormat extends RootJsonFormat[FiniteDuration] {
    def write(obj: FiniteDuration) = JsString(obj.toString())
    def read(value: JsValue) = deserializationError("not implemented")
  }

  implicit val nodeInfoFormat = jsonFormat7(NodeInfo)
  implicit val activeNodesFormat = jsonFormat1(ActiveNodes)
}

