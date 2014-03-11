package actors

import akka.actor.{Actor, ActorLogging}

class User extends Actor with ActorLogging {

  def receive: Receive = {
    case _ =>
  }

}
