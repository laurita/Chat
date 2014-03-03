package actors

import akka.actor.{ActorRef, ActorLogging, Actor}
import messages.Messages._
import java.io.{InputStreamReader, BufferedReader}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await
import messages.Messages.UserRegistered
import messages.Messages.ConsoleListening
import messages.Messages.Username

/**
 * Created by laura on 25/02/14.
 */

class ConsoleListener(stdIn: BufferedReader) extends Actor with ActorLogging {

  def receive: Receive = consoleDeaf

  def consoleDeaf: Receive = {
    case ConsoleListen => {
      log.info("ConsoleListener got ConsoleListen")
      context.become(consoleListening)
      self ! ConsoleListening
    }
  }

  def consoleListening: Receive = {
    case ConsoleListening => {
      log.info("ConsoleListener got ConsoleListening")
      printWelcome()
      val input = stdIn.readLine()
      input match {
        case "register" => {
          println("username")
          context.become(waitUsernameToRegister)
          val username = stdIn.readLine()
          self ! Username(username)
        }
        case "login" => {
          println("username")
          context.become(waitUsernameToLogin)
          val username = stdIn.readLine()
          self ! Username(username)
        }
        case m => {
          log.info(s"unknown message $m")
          context.become(consoleListening)
        }
      }
    }
  }

  def waitUsernameToRegister: Receive = {
    case Username(user) => {
      implicit val timeout = Timeout(3.seconds)
      val msgCreatorFuture = context.system.actorSelection("user/mainActor/messageCreator").resolveOne(3.seconds)
      val msgCreator = Await.result(msgCreatorFuture, 3.seconds)
      msgCreator ! CreateMessage("register", user)
    }
    case UserRegistered(ar) => {
      log.info("actor "+ ar + " registered")
      context.become(registered(ar))
      self ! ListenForChatMessages
    }
  }

  def registered(ar: ActorRef): Receive = {
    case ListenForChatMessages => {
      println("type the messages for your buddies!")
      val msgCreator = context.system.actorSelection("user/mainActor/messageCreator")

      var line = stdIn.readLine()
      while (line != "logout") {
        msgCreator ! CreateMessage("send", line)
        line = stdIn.readLine()
      }

      if (line == "logout") {
        msgCreator ! CreateMessage("logout", ar.path.name)
      }
    }

    case UserLoggedOut(ar: ActorRef) => {
      println("You have been logged out! Type register to login again.")
      context.become(consoleListening)
      self ! ConsoleListening
    }
  }

  def waitUsernameToLogin: Receive = {
    case Username(user) => {

      implicit val timeout = Timeout(3.seconds)
      val msgCreatorFuture = context.system.actorSelection("user/mainActor/messageCreator").resolveOne(3.seconds)
      val msgCreator = Await.result(msgCreatorFuture, 3.seconds)
      msgCreator ! CreateMessage("register", user)

    }
  }

  private def printWelcome() {
    println("Welcome to chat!")
    println("Type 'register'")
  }
}
