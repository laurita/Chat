package main.scala.actors

import akka.actor.Actor

/**
 * Created by laura on 24/02/14.
 */
object Client{

}

class Client extends Actor {

  override def receive: Receive = {

    case m => {
      println(s"Client got msg: $m")
    }
  }
}
