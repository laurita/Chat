package main.scala.messages

import akka.actor.ActorRef

/**
 * Created by laura on 24/02/14.
 */
object Messages {

  case class Register(name: String)
  case object UserExists
  case class UserCreated(client: ActorRef)
  case class Login(name: String)


}
