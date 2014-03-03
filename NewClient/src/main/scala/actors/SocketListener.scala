package actors

import akka.actor.{Props, ActorLogging, Actor}
import java.io.DataInputStream
import messages.Messages._
import messages.Messages.UserLoggedIn
import messages.Messages.ACK
import messages.Messages.WaitForACK

object SocketListener {
  val Success: Byte = 0
}

class SocketListener(in: DataInputStream) extends Actor with ActorLogging {

  def receive = started()

  def started(): Receive = {

    case WaitForACK(bytes) => {
      val lst = bytes.toList
      log.info(s"SocketListener received WaitForACK($lst)")
      val len = 2
      val newByteArray = new Array[Byte](len)
      in.read(newByteArray, 0, len)
      context.become(waitForAck(bytes))
      self ! ACK(newByteArray)
    }

    case m => {
      log.info(s"SocketListener got unknown message $m in started mode")
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
          // login
          case 1 => {
            val cl = context.system.actorSelection("user/mainActor/consoleListener")

            val username = byteArrayToString(byteArray.slice(5, byteArray.length))
            val user = context.system.actorOf(Props[User], name= username)

            log.info(s"user $user created")

            cl ! UserLoggedIn(user)
            context.become(waitingForChatMessages)
          }

          // logout
          case 4 => {
            // stop the actor system
            context.system.shutdown()
          }
        }
      }


    }

    case m => {
      log.info(s"SocketListener got unknown message $m in waitForACK mode")
    }
  }

  def waitingForChatMessages: Receive = {
    case ListenForChatMessages => {
      if (in.available() != 0) {
        val cmd = in.readByte()
        val senderLenBytes = new Array[Byte](4)
        in.read(senderLenBytes)
        val senderLen = Integer.parseInt(senderLenBytes.map(a => toBinary(a.toInt, 8)).mkString, 2)
        val senderBytes = new Array[Byte](senderLen)
        in.read(senderBytes, 0, senderLen)
        val senderLst = senderBytes.toList
        val lenBytes = new Array[Byte](4)
        in.read(lenBytes)
        val len = Integer.parseInt(lenBytes.map(a => toBinary(a.toInt, 8)).mkString, 2)
        val bytes = new Array[Byte](len)
        in.read(bytes, 0, len)
        val lst = bytes.toList
        log.info(s"cmdId: $cmd, senderLen: $senderLen, sender: $senderLst, length: $len, msg: $lst")

        val senderName = byteArrayToString(senderBytes)
        val message = byteArrayToString(bytes)
        println(s"$senderName: $message")
      }

      self ! ListenForChatMessages
    }


    case WaitForACK(bytes) => {
      val lst = bytes.toList
      log.info(s"SocketListener received WaitForACK($lst)")
      val len = 2
      val newByteArray = new Array[Byte](len)
      in.read(newByteArray, 0, len)
      context.become(waitForAck(bytes))
      self ! ACK(newByteArray)
    }

    case m => {
      log.info(s"SocketListener got unknown message $m in waitingForChatMessages mode")
    }
  }

  private def byteArrayToString(byteArray: Array[Byte]): String = {
    byteArray.toList.map(x => x.toChar).mkString
  }

  private def toBinary(i: Int, digits: Int = 8) =
    String.format("%" + digits + "s", i.toBinaryString).replace(' ', '0')

}
