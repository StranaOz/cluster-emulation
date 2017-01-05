package code.sample

import akka.actor.ActorRef
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps

object RestApi {

  implicit val timeout = Timeout(2 seconds)

  def apply(balanceManger: ActorRef, products: List[Product]) = {
    val balanceRout = path("balance") {
      get {
        complete("")
      } ~ post {
        formFields('add.as[Long]) { case amount =>
          validate(amount > 0, "You can add only positive numbers to the balance") {
            complete("")
          }
        }
      } ~ delete {
        complete("")
      }
    }

    val productsRout = path("products") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, products.mkString("\n")))
      }
    }

    val ordersRout = path("orders") {
      post {
        formFields('productId.as[Long]) { case id =>
          complete("")
        }
      }
    }

    balanceRout ~ productsRout ~ ordersRout
  }

}
