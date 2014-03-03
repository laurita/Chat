package actors

import akka.actor.{ActorLogging, ActorRef, Actor}
import messages.Messages.{ListenForChatMessages, WriteToSocket, WaitForACK, MessageWithByteArray}
import java.io.DataOutputStream
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await

/**
 * Created by laura on 25/02/14.
 */
class SocketWriter(out: DataOutputStream) extends Actor with ActorLogging {

  def receive: Receive = {
    case MessageWithByteArray(byteArray) => {
      val lst = byteArray.toList
      log.info(s"SocketWriter received MessageWithByteArray($lst)")
      implicit val timeout = Timeout(3.seconds)
      val socketListenerFuture = context.system.actorSelection("user/mainActor/socketListener").resolveOne(3.seconds)
      val socketListenerRes = Await.result(socketListenerFuture, 3.seconds).asInstanceOf[ActorRef]

      out.write(byteArray)
      out.flush()

      val cmd = lst.head

      cmd match {
        // register
        case 1 => {
          socketListenerRes match {
            case sl: ActorRef => {
              sl ! WaitForACK(byteArray)
            }
          }
        }
        // send
        case 3 => {
          socketListenerRes match {
            case sl: ActorRef => {
              sl ! ListenForChatMessages
            }
          }
        }
      }
    }
  }

}
