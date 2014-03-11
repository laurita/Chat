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

    case CreateMessage(command, message) =>
      log.info(s"got CreateMessage($command, $message)")

      // find socket writer
      val socketWriter = context.system.actorSelection("user/mainActor/socketWriter")
      // create byte message
      val msgByteArray =
        Array(commandCodes(command)) ++ intToByteArray(message.length) ++ message.getBytes("UTF-8")
      // send message to socket writer
      socketWriter ! MessageWithByteArray(msgByteArray)

    case m =>
      log.info(s"got unknown message $m")
  }

}
