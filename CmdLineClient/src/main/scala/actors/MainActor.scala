package actors

import akka.actor.{ActorLogging, Props, Actor}
import helpers.Messages
import Messages.{ConsoleListen, MainStart}
import java.io.{BufferedReader, DataOutputStream, DataInputStream}

class MainActor(in: DataInputStream, out: DataOutputStream, stdIn: BufferedReader) extends Actor with ActorLogging {

  def receive = beforeStart

  def beforeStart: Receive = {
    case MainStart => {
      log.info("Start message received")
      createActors()
      context.become(started)
      val cl = context.system.actorSelection("user/mainActor/consoleListener")
      cl ! ConsoleListen
    }
    case m =>  {
      log.info(s"MainActor received unknown message $m in beforeStart mode")
    }
  }

  def started: Receive = {

    case _ => log.info("MainActor shouldn't receive any messages in started mode")
  }

  def createActors() {
    val consoleListener = context.actorOf(Props(new ConsoleListener(stdIn)), name="consoleListener")
    val socketListener = context.actorOf(Props(new SocketListener(in)), name="socketListener")
    val socketWriter = context.actorOf(Props(new SocketWriter(out)), name="socketWriter")
    val messageCreator = context.actorOf(Props[MessageCreator], name="messageCreator")

    log.info("actors created")
  }
}
