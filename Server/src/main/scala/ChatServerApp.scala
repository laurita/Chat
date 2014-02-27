package main.scala

import java.net._
import java.io._
import akka.actor.{ActorRef, Props, ActorSystem}
import akka.pattern.ask
import scala.concurrent.duration._
import main.scala.actors.{Client, Server}
import main.scala.messages.Messages.{CreateActor, Parse, UserCreated, Register}
import scala.concurrent.Await
import akka.util.Timeout

object ChatServerApp extends App {

  if (args.length != 1) {
    println("Usage: java ChatServerApp <port number>")
    System.exit(1)
  }

  val portNumber = args(0)
  val Success: Byte = 0

  println("Started ChatServerApp application")

  try {

    println("Creating actor system...")
    val serverSystem = ActorSystem("system")

    println("Creating server actor...")
    val serverActor = serverSystem.actorOf(Props[Server], name="server")

    val serverSocket = new ServerSocket(Integer.parseInt(portNumber))

    println("Receiving input...")

    while (true) {
      val clientSocket = serverSocket.accept()

      serverActor ! CreateActor(clientSocket)

      //println("client is "+ client.path.name)
    }

  } catch {
    case e: IOException => {
      println(
        s"Exception caught when trying to listen on port $portNumber or listening for a connection")
      println(e.getMessage)
      e.printStackTrace()
    }
  }

  @throws(classOf[IOException])
  private def writeBytes(outputStream: DataOutputStream, cmd: Byte, errorCode: Byte) {
    println("in writeBytes")
    outputStream.writeByte(cmd)
    outputStream.writeByte(errorCode)
    println("writing "+ outputStream.size() +" bytes...")
    outputStream.flush()
  }

  def closeAll(clientSocket: Socket, serverSocket: ServerSocket) {
    try {
      clientSocket.close()
      serverSocket.close()
      println("Trying to close sockets...")
    } catch {
      case e: IOException => {
        println("Unable to close sockets")
      }
    }
  }


}
