package actors

import akka.actor.{ActorRef, Actor}
import messages.Messages.{MessageWithByteArray, CreateMessage}
import scala.collection.immutable.HashMap
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await

/**
 * Created by laura on 25/02/14.
 */
class MessageCreator extends Actor {

  val commandCodes = Map(
  "register" -> 1.toByte,
  "login" -> 2.toByte,
  "send" -> 3.toByte,
  "logout" -> 4.toByte
  )

  def receive: Receive = {
    case CreateMessage(command, message) => {

      implicit val timeout = Timeout(3.seconds)
      val socketWriterFuture = context.system.actorSelection("user/mainActor/socketWriter").resolveOne(3.seconds)
      val socketWriterRes = Await.result(socketWriterFuture, 3.seconds)


      val msgByteArray =
        Array(commandCodes(command)) ++ BigInt(message.length).toByteArray ++ message.getBytes("UTF-8")

      socketWriterRes match {
        case socketWriter: ActorRef => {
          socketWriter ! MessageWithByteArray(msgByteArray)
        }
      }

    }

  }

}
