package actors

import akka.actor.{ActorLogging, Actor}
import helpers.Messages._
import helpers.Parsing.intToByteArray

class MessageCreator extends Actor with ActorLogging {

  val commandCodes = Map(
  "login" -> 1.toByte,
  "send" -> 3.toByte,
  "logout" -> 4.toByte
  )

  def receive: Receive = {
    case CreateMessage(command, message) => {
      log.info(s"got CreateMessage($command, $message)")

      val socketWriter = context.system.actorSelection("user/mainActor/socketWriter")
      val msgByteArray =
        Array(commandCodes(command)) ++ intToByteArray(message.length) ++ message.getBytes("UTF-8")

      socketWriter ! MessageWithByteArray(msgByteArray)
    }
  }

}
