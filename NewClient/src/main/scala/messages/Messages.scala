package messages

import akka.actor.ActorRef

object Messages {
  case object MainStart
  case object ConsoleListen
  case object ConsoleListening
  case class Username(user: String)
  case class CreateMessage(command: String, username: String)
  case class UserLoggedIn(ar: ActorRef)
  case class UserLoggedOut(ar: ActorRef)
  case class MessageWithByteArray(msg: Array[Byte]) {
    private val message = msg
    def getArray = message
  }
  case class WriteToSocket(byteArray: Array[Byte])
  case class WaitForACK(byteArray: Array[Byte])
  case class ACK(byteArray: Array[Byte])
  case object ListenForChatMessages
}
