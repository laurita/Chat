package main.scala.actors

import akka.actor.{ActorLogging, Actor}
import main.scala.messages.Messages._
import java.net.Socket
import java.io.{BufferedInputStream, DataInputStream, BufferedOutputStream, DataOutputStream}
import main.scala.messages.Messages.ForwardAll
import main.scala.messages.Messages.Message
import main.scala.messages.Messages.Parse

object Client{

}

class Client(clientSocket: Socket) extends Actor with ActorLogging {

  val commandCodes = Map(
    "login" -> 1.toByte,
    "send" -> 3.toByte,
    "logout" -> 4.toByte,
    "receive" -> 5.toByte
  )

  //log.info(self.path.name + " listening...")
  val out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream))
  val in = new DataInputStream(
    new BufferedInputStream(clientSocket.getInputStream))

  //log.info("Receiving input...")

  val cmd = in.readByte()
  val lenBytes = new Array[Byte](4)
  in.read(lenBytes)
  val len = Integer.parseInt(lenBytes.map(a => toBinary(a.toInt, 8)).mkString, 2)
  val bytes = new Array[Byte](len)
  in.read(bytes, 0, len)
  val lst = bytes.toList
  //log.info(s"cmdId: $cmd, length: $len, msg: $lst")

  self ! Parse(cmd, bytes)

  override def receive: Receive = notLoggedIn

  def notLoggedIn: Receive = {

    case Parse(command, message) => {

      log.info(s"$self received Parse($command, $message) when notLoggedIn")

      command match {
        // login
        case 1 => {
          log.info(s"matched command $command")
          val username = byteArrayToString(message)
          val server = context.parent
          log.info(s"sending Login($username) to $server")
          server ! Login(username)
        }

        // send
        case 3 => {
          log.info("Parse(3, ...) not implemented")
        }

        case c => {
          log.info("unimplemented command "+ c)
        }
      }

    }

    case UserCreated(name) => {

      log.info(s"client $this got UserCreated($name)")
      context.become(loggedIn(name))

      log.info("sending answer")
      val ats = Array[Byte](cmd, 0.toByte)
      out.write(ats)
      out.flush()

      self ! Listen
    }

    case UserExists => {
      // error code 1 is when user exists
      val ats = Array[Byte](cmd, 1.toByte)
      out.write(ats)
      out.flush()
    }
  }

  def loggedIn(name: String): Receive = {

    case Parse(command, message) => {

      val lst = message.toList
      log.info(s"Client $self got message Parse($command, $lst)")
      log.info("message length is "+ message.length)
      command match {
        // login
        case 1 => {
          // already logged in
          log.info("already logged in")
        }

        // send
        case 3 => {
          log.info("forwarding to others...")
          val from = name
          context.system.actorSelection("user/server") ! ForwardAll(from, message)
        }

        // logout
        case 4 => {
          log.info(s"Client sends Server Logout($name)")
          context.system.actorSelection("user/server") ! Logout(name, out)
          context.become(notLoggedIn)
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
      val lst = bytes.toList
      log.info(s"Client $self got message: $lst")
      log.info("message length is "+ bytes.length)
      log.info("Client is sending it through socket...")

      out.write(bytes)
      out.flush()
    }

  }

  private def toBinary(i: Int, digits: Int = 8) =
    String.format("%" + digits + "s", i.toBinaryString).replace(' ', '0')

  private def byteArrayToString(byteArray: Array[Byte]): String = {
    byteArray.toList.map(x => x.toChar).mkString
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
