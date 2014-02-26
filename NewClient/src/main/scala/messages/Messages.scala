package messages

import java.io.{BufferedReader, DataInputStream}
import akka.actor.ActorRef

/**
 * Created by laura on 25/02/14.
 */
object Messages {
  case object MainStart
  case class MainStarted()
  case object ConsoleListen
  case object ConsoleListening
  case class Username(user: String)
  case class CreateMessage(command: String, username: String)
  case class UserRegistered(ar: ActorRef)
  case class MessageWithByteArray(msg: Array[Byte]) {
    private val message = msg
    def getArray = message
  }
  case class WriteToSocket(byteArray: Array[Byte])
  case class WaitForACK(byteArray: Array[Byte])
  case class ACK(byteArray: Array[Byte])
  case object ListenForChatMessages
}
