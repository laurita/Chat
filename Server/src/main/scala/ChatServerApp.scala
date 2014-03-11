package main.scala

import java.net._
import java.io._
import akka.actor.{Props, ActorSystem}
import main.scala.actors.Server
import main.scala.messages.Messages.CreateActor

object ChatServerApp extends App {

  if (args.length != 1) {
    println("Usage: java ChatServerApp <port number>")
    System.exit(1)
  }

  val portNumber = args(0)
  val Success: Byte = 0

  println("Started ChatServerApp application")

  try {

    val serverSystem = ActorSystem("system")
    val serverActor = serverSystem.actorOf(Props[Server], name="server")
    val serverSocket = new ServerSocket(Integer.parseInt(portNumber))

    println("Receiving input through socket...")

    while (true) {
      val clientSocket = serverSocket.accept()
      serverActor ! CreateActor(clientSocket)
    }

  } catch {
    case e: IOException =>
      println(
        s"Exception caught when trying to listen on port $portNumber or listening for a connection")
      println(e.getMessage)
      e.printStackTrace()
  }

  // closes given sockets
  def closeAll(clientSocket: Socket, serverSocket: ServerSocket) {
    try {
      clientSocket.close()
      serverSocket.close()
      println("Trying to close sockets...")
    } catch {
      case e: IOException =>
        println("Unable to close sockets")
    }
  }
}
