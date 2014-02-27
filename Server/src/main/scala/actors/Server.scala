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
      val client = context.system.actorOf(Props(new Client(clientSocket)))
    }

    case Register(name) => {
      log.info("server got Register("+ name +")")
      val user = regUsers.getList.find(ar => ar.path.name == name)
      user match {
        case None => {
          // register new user, send it's ref back
          log.info(s"User doesn't exists. Registering $name...")
          context.become(listening(regUsers.registered(sender)))
          sender ! UserCreated
        }
        case Some(ar) => {
          // send answer that user exists
          sender ! UserExists
        }
      }

    }

    case ForwardAll(bytes: Array[Byte]) => {
      val buddies = context.children.filterNot(ar => ar == sender)
      buddies.foreach(b => b ! Message(bytes))
    }

    case Login => {

    }

    case m => {
      log.info(s"Server got unknown message: $m")
    }
  }
}
