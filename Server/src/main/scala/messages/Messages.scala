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
  case object UserCreated
  case class Login(name: String)
  case object Listen
  case class ForwardAll(bytes: Array[Byte])
  case class Message(bytes: Array[Byte])
}
