import akka.actor.ActorSystem
import akka.actor.Props
import java.io._
import java.net._
import main.scala.actors.ConsoleActor

/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Modified Java client-server application example: client side.
 */

object ClientApp {

  def main(args: Array[String]) {

    if (args.length != 2) {
      println(
        "Usage: java ClientApp <host name> <port number>")
      System.exit(1)
    }

    var state = "unregistered"

    val hostName = args(0)
    val portNumber = args(1).toInt

    /*
    println("Creating console actor system...")
    val consoleSystem = ActorSystem("consoleSystem")

    println("Creating console actor...")
    val consoleActor = serverSystem.actorOf(Props[ConsoleActor], name="consoleActor")
    */

    try {
      val socket = new Socket(hostName, portNumber)

      val out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream))
      val in = new DataInputStream(new BufferedInputStream(socket.getInputStream))

      val stdIn =
        new BufferedReader(new InputStreamReader(System.in))

      state match {

        case "unregistered" => {

          println("This is chat. Usage:")
          println("Type\n'register' to register\n'login' to login")

          val cmd: Byte = getCommand(stdIn)

          cmd match {
            case 1 => {
              val username = getUsername(stdIn).getBytes("UTF-8")

              //listenForMessages(in)

              writeBytes(out, cmd, username)


              listenForMessages(in)

            }
          }
        }

        case "registered" => {

        }
      }

      socket.close()
      println("Client socket closing...")

    } catch {

      case e: UnknownHostException =>
        println(s"Don't know about host $hostName")
        System.exit(1)

      case e: IOException =>
        println(s"Couldn't get I/O for the connection to $hostName")
        System.exit(1)
    }
  }

  def listenForMessages(in: DataInputStream) {

    println("listening for messages")
    var bytesReceived = readBytes(in)
    println("got "+ bytesReceived)
    bytesReceived match {
      case Array(1, 0) => {
        println("got "+ Array(1, 0))
        listenForMessages(in)
      }
      case Array(cmd, error) => {
        throw new Exception(s"error code: $error")
      }
    }
  }

  @throws(classOf[IOException])
  private def readBytes(inputStream: DataInputStream): Array[Byte] = {
    println("in readBytes")
    try {
      val length = 2
      val bytes = new Array[Byte](length)
      println("before read")
      val read = inputStream.read(bytes, 0, length)
      println(s"read $read bytes")
      println("bytes "+ bytes)
      bytes
    }
    catch {
      case npe: NullPointerException =>
        throw new EOFException("Connection closed.")
    }
  }

  @throws(classOf[IOException])
  private def writeBytes(outputStream: DataOutputStream, cmd: Byte, msgBytes: Array[Byte]) {
    val length = msgBytes.length
    outputStream.writeByte(cmd)
    outputStream.writeInt(length)
    outputStream.write(msgBytes, 0, length)
    outputStream.flush()
  }

  def getUsername(stdIn: BufferedReader): String = {
    stdIn.readLine()
  }

  def getCommand(stdIn: BufferedReader): Byte = {
    val userInput = stdIn.readLine()
    userInput match {
      case "register" => {
        println("username")
        1
      }
      case "login" => {
        println("username")
        2
      }
      /*
      case _ => {
        throw new Exception("No such command")
        println("Usage: blah blah")
      }
      */
    }
  }

}
