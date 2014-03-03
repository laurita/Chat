package main.scala.actors

import akka.actor._
import main.scala.messages.Messages._
import main.scala.messages.Messages.Login
import java.io.DataOutputStream

object Server {

}

class Server extends Actor with ActorLogging {

  val commandCodes = Map(
    "login" -> 1.toByte,
    "send" -> 3.toByte,
    "logout" -> 4.toByte,
    "receive" -> 5.toByte
  )

  case class LoggedInUsers(urList: List[ActorRef]) {

    private val userList = urList

    def getList = userList

    def login(usr: ActorRef) = {
      LoggedInUsers(usr :: userList)
    }

    def logout(usr: ActorRef) = {
      val usersWithout = userList.filter(x => !x.equals(usr))
      LoggedInUsers(usersWithout)
    }
  }

  override def receive: Receive = listening(new LoggedInUsers(List[ActorRef]()))

  def listening(loggedInUsers: LoggedInUsers): Receive = {

    case CreateActor(clientSocket) => {
      log.info("Server got CreateActor message")
      val client = context.actorOf(Props(new Client(clientSocket)))
      log.info(s"$client created")
    }

    case Login(name) => {
      log.info(s"server got Login($name)")
      val user = loggedInUsers.getList.find(ar => ar.path.name == name)
      user match {
        case None => {
          // login new user, send it's ref back
          log.info(s"User doesn't exists. Logging in $name...")
          context.become(listening(loggedInUsers.login(sender)))
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

    case Logout(name: String, out: DataOutputStream) => {
      log.info(s"Server got Logout($name)")
      val newRegUsres = loggedInUsers.logout(sender)
      context.become(listening(newRegUsres))

      val ats = Array[Byte](4, 0.toByte)
      out.write(ats)
      out.flush()

      // close the socket
      out.close()
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
