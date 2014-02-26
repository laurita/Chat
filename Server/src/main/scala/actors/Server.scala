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

    case Register(name) => {
      log.info("server got Register("+ name +")")
      val user = regUsers.getList.find(ar => ar.path.name == name)
      user match {
        case None => {
          // register new user, send it's ref back
          println(s"User doesn't exists. Registering $name...")
          val client = context.actorOf(Props[Client], name=name)
          context.become(listening(regUsers.registered(client)))
          sender ! UserCreated(client)
        }
        case ar: Option[ActorRef] => {
          // send answer that user exists
          sender ! UserExists
        }
      }

    }
    case Login =>

    case m => {
      println(s"Server got msg: $m")
      println("Forwarding it to client...")
      context.children.find(ar => ar.path.name == "client").get forward m
    }
  }
}
