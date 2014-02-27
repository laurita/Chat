package main.scala.actors

import akka.actor.{ActorLogging, Actor}
import main.scala.messages.Messages._
import java.net.Socket
import java.io.{BufferedInputStream, DataInputStream, BufferedOutputStream, DataOutputStream}
import akka.pattern.ask
import main.scala.messages.Messages.Parse
import main.scala.messages.Messages.Register
import scala.concurrent.Await
import akka.util.Timeout
import scala.concurrent.duration._
/**
 * Created by laura on 24/02/14.
 */
object Client{

}

class Client(clientSocket: Socket) extends Actor with ActorLogging {

  val commandCodes = Map(
    "register" -> 1.toByte,
    "login" -> 2.toByte,
    "send" -> 3.toByte,
    "logout" -> 4.toByte,
    "receive" -> 5.toByte
  )

  log.info(self.path.name + " listening...")
  val out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream))
  val in = new DataInputStream(
    new BufferedInputStream(clientSocket.getInputStream))

  log.info("Receiving input...")

  val cmd = in.readByte()
  log.info(s"cmd $cmd")
  val lenBytes = new Array[Byte](4)
  in.read(lenBytes)
  val len = Integer.parseInt(lenBytes.map(a => toBinary(a.toInt, 8)).mkString, 2)
  log.info(s"len $len")
  val bytes = new Array[Byte](len)
  in.read(bytes, 0, len)
  val lst = bytes.toList
  log.info(s"bytes $lst")
  log.info(s"cmdId: $cmd, length: $len, msg: $lst")

  self ! Parse(cmd, bytes)

  override def receive: Receive = notLoggedIn

  def notLoggedIn: Receive = {

    case Parse(command, message) => {

      command match {
        // register
        case 1 => {
          val username = byteArrayToString(message)

          implicit val timeout = Timeout(5.seconds)
          context.actorSelection("../server") ! Register(username)
        }

        // send
        case 3 => {

        }
      }

    }

    case UserCreated => {
      context.become(loggedIn)

      val ats = Array[Byte](cmd, 0.toByte)
      out.write(ats)
      out.flush()

      self ! Listen
    }

    case UserExists => {
      // error code 1 is for user exists
      val ats = Array[Byte](cmd, 1.toByte)
      out.write(ats)
      out.flush()
    }
  }

  def loggedIn: Receive = {

    case Parse(command, message) => {

      log.info(s"Client $self got message Parse($command, $message)")
      command match {
        // register
        case 1 => {
          // already registered
          log.info("logged in")
        }

        // send
        case 3 => {
          // TODO: forward message for all users
          log.info("forwarding to others...")
          context.system.actorSelection("user/server") ! ForwardAll(message)
          //context.system.actorSelection("user/*") ! byteArrayToString(message)
        }
      }

    }

    case Listen => {
      //log.info(self +" received "+ Listen)
      if (in.available() != 0) {
        val cmd = in.readByte()
        val lenBytes = new Array[Byte](4)
        in.read(lenBytes)
        val len = Integer.parseInt(lenBytes.map(a => toBinary(a.toInt, 8)).mkString, 2)
        val bytes = new Array[Byte](len)
        in.read(bytes, 0, len)
        val lst = bytes.toList
        log.info(s"cmdId: $cmd, length: $len, msg: $lst")

        self ! Parse(cmd, bytes)
      }
      self ! Listen
    }

    case Message(bytes: Array[Byte]) => {
      log.info(s"Client $self got message: $bytes")
      log.info("Client is sending it through socket...")


      val m = byteArrayToString(bytes)
      val senderName = self.path.name

      val msgByteArray =
        Array(commandCodes("receive")) ++
          intToByteArray(senderName.length) ++
          senderName.getBytes("UTF-8") ++
          intToByteArray(m.length) ++
          m.getBytes("UTF-8")

      out.write(msgByteArray)
      out.flush()
    }

  }

  private def toBinary(i: Int, digits: Int = 8) =
    String.format("%" + digits + "s", i.toBinaryString).replace(' ', '0')

  private def byteArrayToString(byteArray: Array[Byte]): String = {
    byteArray.toList.map(x => x.toChar).mkString
  }

  private def intToByteArray(x: Int): Array[Byte] = {

    val binaryStr = x.toBinaryString
    val pad = "0" * (32 - binaryStr.length)

    val fullBinStr = pad + binaryStr

    splitToStringsOfLen(fullBinStr, 8).map(x => Integer.parseInt(x, 2).toByte).toArray
  }

  private def splitToStringsOfLen(str: String, len: Int): List[String] = {
    def rec(str: String, acc: List[String]): List[String] = {
      str match {
        case "" => acc
        case string => {
          val tpl = string.splitAt(string.length - len)
          rec(tpl._1, tpl._2 :: acc)
        }
      }
    }
    rec(str, List())
  }
}
