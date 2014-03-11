package actors

import akka.actor.{ActorRef, ActorLogging, Actor}
import helpers.Messages._
import java.io.BufferedReader

class ConsoleListener(stdIn: BufferedReader) extends Actor with ActorLogging {

  def receive: Receive = {

    case ConsoleListen =>
      log.info("got ConsoleListen")
      context.become(consoleListening)
      self ! ConsoleListening
  }

  def consoleListening: Receive = {

    case ConsoleListening =>
      log.info("got ConsoleListening")

      printWelcome()
      val input = stdIn.readLine()

      input match {

        case "login" =>
          println("username")
          context.become(waitUsernameToLogin)
          val username = stdIn.readLine()
          self ! Username(username)

        case c =>
          log.info(s"got unknown command $c from cmd line in consoleListening mode")
          self ! ConsoleListening
      }

    case m =>
      log.info(s"got unknown message $m in consoleListening mode")
  }

  def waitUsernameToLogin: Receive = {

    case Username(user) =>
      val msgCreator = context.system.actorSelection("user/mainActor/messageCreator")
      context.become(waitLoginConfirmation)
      msgCreator ! CreateMessage("login", user)

    case m =>
      log.info(s"got unknown message $m in waitUsernameToLogin mode")
  }

  def waitLoginConfirmation: Receive = {

    case UserLoggedIn(ar) =>
      log.info(s"$ar logged in")
      context.become(loggedIn(ar))
      self ! ListenForChatMessages

    case m =>
      log.info(s"got unknown message $m in waitLoginConfirmation mode")
  }

  def loggedIn(ar: ActorRef): Receive = {

    case ListenForChatMessages =>
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

    case UserLoggedOut(ar: ActorRef) =>
      println("You have been logged out! Type login to login again.")
      context.become(consoleListening)
      self ! ConsoleListening

    case m =>
      log.info(s"got unknown message $m in loggedIn mode")
  }

  private def printWelcome() {
    println("Welcome to chat!")
    println("Type 'login'")
  }
}
