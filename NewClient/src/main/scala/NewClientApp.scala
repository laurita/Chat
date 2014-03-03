import actors.MainActor
import akka.actor._
import java.io._
import java.net.Socket
import messages.Messages.MainStart

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

    mainActor ! MainStart
  }

}
