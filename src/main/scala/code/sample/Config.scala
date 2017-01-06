package code.sample

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._

import scala.concurrent.duration.FiniteDuration

trait Config {

  val conf = ConfigFactory.load()

  val tickDuration = conf.as[FiniteDuration]("cluster.tick-duration")
  val nodesNumber = conf.getInt("cluster.nodes-number")

  val host = conf.getString("restapi.host")
  val port = conf.getInt("restapi.port")

}
