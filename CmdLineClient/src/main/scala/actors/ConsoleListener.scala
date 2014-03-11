package actors

import akka.actor.{ActorRef, ActorLogging, Actor}
import helpers.Messages._
import java.io.BufferedReader

class ConsoleListener(stdIn: BufferedReader) extends Actor with ActorLogging {

  // state 0: when console has not started listening for input
  // waits for message to start
  def receive: Receive = {

    case ConsoleListen =>
      log.info("got ConsoleListen")
      context.become(consoleListening)
      self ! ConsoleListeningForCommand
  }

  // state 1.1: when console is listening for command from command line
  def consoleListening: Receive = {

    case ConsoleListeningForCommand =>
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
          self ! ConsoleListeningForCommand
      }

    case m =>
      log.info(s"got unknown message $m in consoleListening mode")
  }

  // state 1.2: when console is waiting for username from command line
  def waitUsernameToLogin: Receive = {

    case Username(user) =>
      val msgCreator = context.system.actorSelection("user/mainActor/messageCreator")
      context.become(waitLoginConfirmation)
      msgCreator ! CreateMessage("login", user)

    case m =>
      log.info(s"got unknown message $m in waitUsernameToLogin mode")
  }

  // state 2: when console is waiting for login confirmation from backend
  // (intermediate actors involved)
  def waitLoginConfirmation: Receive = {

    case UserLoggedIn(ar) =>
      log.info(s"$ar logged in")
      context.become(loggedIn(ar))
      self ! ListenForChatMessages

    case m =>
      log.info(s"got unknown message $m in waitLoginConfirmation mode")
  }

  // state 3: when user is logged in and console is waiting for chat messages
  def loggedIn(ar: ActorRef): Receive = {

    case ListenForChatMessages =>
      println("Type messages for your buddies!")
      val msgCreator = context.system.actorSelection("user/mainActor/messageCreator")

      // create socket listener actor
      // send message to socket listener to start listening for messages
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
      self ! ConsoleListeningForCommand

    case m =>
      log.info(s"got unknown message $m in loggedIn mode")
  }

  private def printWelcome() {
    println("Welcome to chat!")
    println("Type 'login'")
  }
}
