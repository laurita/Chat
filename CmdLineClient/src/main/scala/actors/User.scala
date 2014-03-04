package actors

import akka.actor.{Actor, ActorLogging}

/**
 * Created by laura on 25/02/14.
 */
class User extends Actor with ActorLogging {

  def receive: Receive = {
    case _ =>
  }

}
