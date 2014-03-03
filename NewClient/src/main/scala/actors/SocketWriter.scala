package actors

import akka.actor.{ActorLogging, ActorRef, Actor}
import messages.Messages.{ListenForChatMessages, WriteToSocket, WaitForACK, MessageWithByteArray}
import java.io.DataOutputStream
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await

class SocketWriter(out: DataOutputStream) extends Actor with ActorLogging {

  def receive: Receive = {
    case MessageWithByteArray(byteArray) => {
      val lst = byteArray.toList
      log.info(s"SocketWriter received MessageWithByteArray($lst)")

      val sl = context.system.actorSelection("user/mainActor/socketListener")

      out.write(byteArray)
      out.flush()

      val cmd = lst.head

      cmd match {
        // register
        case 1 => {
              sl ! WaitForACK(byteArray)
        }
        // send
        case 3 => {
          sl ! ListenForChatMessages
        }

        // logout
        case 4 => {
          log.info(s"SocketWriter matched $cmd")
          log.info(s"SocketListener is: $sl")
          sl ! WaitForACK(byteArray)
        }
      }
    }
  }

}
