package actors

import akka.actor.{ActorLogging, Props, Actor}
import messages.Messages.{MainStarted, MainStart}
import java.io.{BufferedReader, DataOutputStream, DataInputStream}
import java.net.Socket

/**
 * Created by laura on 25/02/14.
 */

class MainActor(in: DataInputStream, out: DataOutputStream, stdIn: BufferedReader) extends Actor with ActorLogging {

  def receive = beforeStart

  def beforeStart: Receive = {
    case MainStart => {
      log.info("Start message received")
      createActors()
      context.become(started)
      sender ! MainStarted()
    }
    case m =>  {
      log.info(s"unknown message received: $m")
    }
  }

  def started: Receive = {

    case _ =>
  }

  def createActors() {
    val consoleListener = context.actorOf(Props(new ConsoleListener(stdIn)), name="consoleListener")
    val socketListener = context.actorOf(Props(new SocketListener(in)), name="socketListener")
    val socketWriter = context.actorOf(Props(new SocketWriter(out)), name="socketWriter")
    val messageCreator = context.actorOf(Props[MessageCreator], name="messageCreator")

    log.info("actors created")
  }
}
