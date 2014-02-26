package actors

import akka.actor.{Props, ActorRef, ActorLogging, Actor}
import java.io.DataInputStream
import messages.Messages.{UserRegistered, ACK, WaitForACK}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await

/**
 * Created by laura on 25/02/14.
 */
object SocketListener {
  val Success: Byte = 0
}

class SocketListener(in: DataInputStream) extends Actor with ActorLogging {

  def receive: Receive = {

    case WaitForACK(byteArray) => {
      val lst = byteArray.toList
      log.info(s"SocketListener received WaitForACK($lst)")
      val len = 2
      val newByteArray = new Array[Byte](len)
      in.read(newByteArray, 0, len)
      context.become(waitForAck(byteArray))
      self ! ACK(newByteArray)
    }


  }

  def waitForAck(byteArray: Array[Byte]): Receive = {
    case ACK(newByteArray) => {
      val lst = newByteArray.toList
      log.info(s"SocketListener received ACK($lst)")
      val cmdByte = byteArray(0)
      val ackCmdByte = newByteArray(0)
      if (cmdByte == ackCmdByte && newByteArray(1) == SocketListener.Success) {
        cmdByte match {
          case 1 => {
            implicit val timeout = Timeout(3.seconds)
            val consoleListenerFuture = context.system.actorSelection(
              "user/mainActor/consoleListener").resolveOne(3.seconds)
            val consoleListenerRes = Await.result(consoleListenerFuture, 3.seconds).asInstanceOf[ActorRef]

            val username = byteArray.slice(1, byteArray.length).toString
            val registeredUser = context.system.actorOf(Props[User], name= username)
            consoleListenerRes match {
              case consoleListener: ActorRef => {
                consoleListener ! UserRegistered(registeredUser)
              }
            }
          }
        }
      }
    }
  }

}
