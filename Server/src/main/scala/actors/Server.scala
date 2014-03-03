package main.scala.actors

import akka.actor._
import main.scala.messages.Messages._
import main.scala.messages.Messages.Login
import main.scala.messages.Messages.Register
import scala.collection.immutable.HashMap
import scala.Option

/**
 * Created by laura on 24/02/14.
 */
object Server {




}

class Server extends Actor with ActorLogging {

  val commandCodes = Map(
    "register" -> 1.toByte,
    "login" -> 2.toByte,
    "send" -> 3.toByte,
    "logout" -> 4.toByte,
    "receive" -> 5.toByte
  )

  case class RegisteredUsers(urList: List[ActorRef]) {

    private val userList = urList

    def getList = userList

    def registered(usr: ActorRef) = {
      RegisteredUsers(usr :: userList)
    }
  }

  override def receive: Receive = listening(new RegisteredUsers(List[ActorRef]()))

  def listening(regUsers: RegisteredUsers): Receive = {

    case CreateActor(clientSocket) => {
      log.info("Server got CreateActor message")
      val client = context.actorOf(Props(new Client(clientSocket)))
      log.info(s"$client created")
    }

    case Register(name) => {
      log.info(s"server got Register($name)")
      val user = regUsers.getList.find(ar => ar.path.name == name)
      user match {
        case None => {
          // register new user, send it's ref back
          log.info(s"User doesn't exists. Registering $name...")
          context.become(listening(regUsers.registered(sender)))
          sender ! UserCreated(name)
        }
        case Some(ar) => {
          // send answer that user exists
          sender ! UserExists
        }
      }

    }

    case ForwardAll(from: String, bytes: Array[Byte]) => {

      log.info("Server got ForwardAll")
      val m = byteArrayToString(bytes)

      val msgByteArray =
        Array(commandCodes("receive")) ++
          intToByteArray(from.length) ++
          from.getBytes("UTF-8") ++
          intToByteArray(m.length) ++
          m.getBytes("UTF-8")

      val buddies = context.children.filterNot(ar => ar == sender)
      buddies.foreach(b => b ! Message(msgByteArray))
    }

    case Login => {

    }

    case m => {
      log.info(s"Server got unknown message: $m")
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
