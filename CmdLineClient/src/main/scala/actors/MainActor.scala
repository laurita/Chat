package actors

import akka.actor.{ActorLogging, Props, Actor}
import helpers.Messages._
import java.io.{BufferedReader, DataOutputStream, DataInputStream}

class MainActor(in: DataInputStream, out: DataOutputStream, stdIn: BufferedReader) extends Actor with ActorLogging {

  def receive = beforeStart

  def beforeStart: Receive = {

    case MainStart =>
      log.info("got MainStart")

      createActors()
      context.become(started)
      val cl = context.system.actorSelection("user/mainActor/consoleListener")
      cl ! ConsoleListen

    case m =>
      log.info(s"got unknown message $m in beforeStart mode")
  }

  def started: Receive = {
    case _ => log.info("shouldn't receive any messages in started mode")
  }

  // creates all required actors
  private def createActors() {
    context.actorOf(Props(new ConsoleListener(stdIn)), name="consoleListener")
    context.actorOf(Props(new SocketListener(in)), name="socketListener")
    context.actorOf(Props(new SocketWriter(out)), name="socketWriter")
    context.actorOf(Props[MessageCreator], name="messageCreator")
    log.info("actors created")
  }
}
