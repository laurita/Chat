package main.scala.messages

import akka.actor.ActorRef
import java.net.Socket

/**
 * Created by laura on 24/02/14.
 */
object Messages {

  case class CreateActor(socket: Socket)
  case class Parse(cmd: Byte, msg: Array[Byte])
  case class Register(name: String)
  case object UserExists
  case class UserCreated(name: String)
  case class Login(name: String)
  case object Listen
  case class ForwardAll(from: String, bytes: Array[Byte])
  case class Message(bytes: Array[Byte])
}
