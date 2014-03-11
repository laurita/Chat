package actors

import akka.actor.{ActorRef, ActorLogging, Actor}
import helpers.Messages
import Messages._
import java.io.{InputStreamReader, BufferedReader}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await
import Messages.UserLoggedIn
import Messages.ConsoleListening
import Messages.Username

class ConsoleListener(stdIn: BufferedReader) extends Actor with ActorLogging {

  def receive: Receive = {
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
        case "login" => {
          println("username")
          context.become(waitUsernameToLogin)
          val username = stdIn.readLine()
          self ! Username(username)
        }
        case m => {
          log.info(s"ConsoleListener received unknown message $m in consoleListening mode")
          //context.become(consoleListening)
          self ! ConsoleListening
        }
      }
    }
  }

  def waitUsernameToLogin: Receive = {
    case Username(user) => {
      val msgCreator = context.system.actorSelection("user/mainActor/messageCreator")
      context.become(waitLoginConfirmation)
      msgCreator ! CreateMessage("login", user)
    }

  }

  def waitLoginConfirmation: Receive = {
    case UserLoggedIn(ar) => {
      log.info(s"$ar logged in")
      context.become(loggedIn(ar))
      self ! ListenForChatMessages
    }
  }

  def loggedIn(ar: ActorRef): Receive = {
    case ListenForChatMessages => {
      println("Type messages for your buddies!")
      val msgCreator = context.system.actorSelection("user/mainActor/messageCreator")

      // start listening for messages
      val sl = context.system.actorSelection("user/mainActor/socketListener")
      sl ! ListenForChatMessages

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
      println("You have been logged out! Type login to login again.")
      context.become(consoleListening)
      self ! ConsoleListening
    }
  }

  private def printWelcome() {
    println("Welcome to chat!")
    println("Type 'login'")
  }
}
