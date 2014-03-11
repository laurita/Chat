package actors

import akka.actor.{Props, ActorLogging, Actor}
import java.io.DataInputStream
import helpers.Messages._
import helpers.Parsing._

object SocketListener {
  val Success: Byte = 0
}

class SocketListener(in: DataInputStream) extends Actor with ActorLogging {

  def receive = started()

  def started(): Receive = {

    case WaitForACK(bytes) =>
      log.info(s"got WaitForACK("+ bytes.toList +")")

      val newByteArray = new Array[Byte](2)
      in.read(newByteArray, 0, 2)
      context.become(waitForAck(bytes))
      self ! ACK(newByteArray)

    case m =>
      log.info(s"got unknown message $m in started mode")
  }

  def waitForAck(byteArray: Array[Byte]): Receive = {

    case ACK(newByteArray) =>
      log.info(s"SocketListener received ACK("+ newByteArray.toList +")")

      val cmdByte = byteArray(0)
      val ackCmdByte = newByteArray(0)
      if (cmdByte == ackCmdByte && newByteArray(1) == SocketListener.Success) {

        cmdByte match {
          // login
          case 1 =>
            val cl = context.system.actorSelection("user/mainActor/consoleListener")
            val username = byteArrayToString(byteArray.slice(5, byteArray.length))
            val user = context.system.actorOf(Props[User], name= username)

            log.info(s"user $user created")

            cl ! UserLoggedIn(user)
            context.become(waitingForChatMessages)

          // logout
          case 4 =>
            // stop the actor system
            context.system.shutdown()

          case c =>
            log.info(s"unknown command $c")
        }
      }

    case m =>
      log.info(s"got unknown message $m in waitForACK mode")
  }

  def waitingForChatMessages: Receive = {

    case ListenForChatMessages =>

      if (in.available() != 0) {
        // the command byte
        in.readByte()
        val senderName = byteArrayToString(readMessage(in))
        val message = byteArrayToString(readMessage(in))
        println(s"$senderName: $message")
      }
      self ! ListenForChatMessages

    case WaitForACK(bytes) =>
      log.info(s"got WaitForACK("+ bytes.toList +")")

      val newByteArray = new Array[Byte](2)
      in.read(newByteArray, 0, 2)
      context.become(waitForAck(bytes))
      self ! ACK(newByteArray)

    case m =>
      log.info(s"got unknown message $m in waitingForChatMessages mode")
  }

  // reads 4 bytes from given input stream to get length N of message
  // returns next N bytes (message)
  private def readMessage(in: DataInputStream): Array[Byte] = {
    val lenBytes = new Array[Byte](4)
    in.read(lenBytes)
    val len = Integer.parseInt(lenBytes.map(a => toBinary(a.toInt, 8)).mkString, 2)
    val bytes = new Array[Byte](len)
    in.read(bytes, 0, len)
    bytes
  }

}
