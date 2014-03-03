import actors.MainActor
import akka.actor._
import akka.util.Timeout
import java.io._
import java.net.Socket
import messages.Messages.{ConsoleListen, MainStarted, MainStart}
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.ask

/**
 * Created by laura on 25/02/14.
 */
object NewClientApp {

  def main(args: Array[String]) {

    if (args.length != 2) {
      println("Usage: java NewClientApp <host name> <port number>")
      System.exit(1)
    }

    val host = args(0)
    val port = args(1).toInt

    val socket = new Socket(host, port)

    val out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream))
    val in = new DataInputStream(new BufferedInputStream(socket.getInputStream))
    val stdIn = new BufferedReader(new InputStreamReader(System.in))


    val system = ActorSystem("system")
    val mainActor = system.actorOf(Props(new MainActor(in, out, stdIn)), name="mainActor")

    implicit val timeout = Timeout(3 seconds)
    val startFuture = mainActor ? MainStart
    val startResult = Await.result(startFuture, 3.seconds).asInstanceOf[MainStarted]

    startResult match {
      case MainStarted() => {
        implicit val timeout = Timeout(3.seconds)
        val consoleListenerFuture = system.actorSelection("user/mainActor/consoleListener").resolveOne()
        val consoleListenerRes = Await.result(consoleListenerFuture, 3.seconds).asInstanceOf[ActorRef]

        consoleListenerRes ! ConsoleListen

        printWelcome()
      }
    }

  }

  def printWelcome() {
    println("Welcome to chat!")
    println("Type 'register'")
  }

}
