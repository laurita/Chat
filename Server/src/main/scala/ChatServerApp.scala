/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package main.scala

import java.net._
import java.io._
import akka.actor.{ActorRef, Props, ActorSystem}
import akka.pattern.ask
import scala.concurrent.duration._
import main.scala.actors.Server
import main.scala.messages.Messages.{UserCreated, Register}
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

    val serverSocket = new ServerSocket(Integer.parseInt(portNumber))
    val clientSocket = serverSocket.accept()

    println("Creating actor sy  stem...")
    val serverSystem = ActorSystem("system")

    println("Creating server actor...")
    val serverActor = serverSystem.actorOf(Props[Server], name="server")

    listenForMessages(serverSocket, clientSocket, serverActor)

  } catch {
    case e: IOException => {
      println(
        s"Exception caught when trying to listen on port $portNumber or listening for a connection")
      println(e.getMessage)
      e.printStackTrace()
    }
  }

  /*
   * Command Ids:
   * 1 - Register
   * 2 - Login
   * 3 - SendMessage
   * 4 - Logout
   */
  def listenForMessages(serverSocket: ServerSocket, clientSocket: Socket, serverActor: ActorRef) {

    val out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream))
    val in = new DataInputStream(
      new BufferedInputStream(clientSocket.getInputStream))

    println("Receiving input...")

    val cmdId = in.readByte()
    println(s"cmdByte: $cmdId")

    val len = in.readInt()
    println(s"length: $len")
    val msgByteArray = new Array[Byte](len)
    in.readFully(msgByteArray, 0, len)
    val msg = new String(msgByteArray, "UTF-8")
    println(s"msg: $msg")
    println(s"cmdId: $cmdId, length: $len, msg: $msgByteArray")

    cmdId match {
      // register a new user
      case 1 => {
        println("sending Register("+ msg +") to serverActor...")
        implicit val timeout = Timeout(3 seconds)
        val future = serverActor ? Register(msg)
        val userRes = Await.result(future, 3.seconds).asInstanceOf[UserCreated]

        userRes match {
          case UserCreated(ar) => {
            println(s"got UserCreated($ar)")
            // TODO: finish
            println("calling writeBytes...")
            Thread.sleep(1000)
            writeBytes(out, cmdId, Success)
            //out.println("Client successfully created")
            //listenForMessages(in, serverActor, out)
          }
        }
      }
      case 2 => {

      }
      case 3 => {

      }
      case 4 => {
        println("calling closeAll...")
        closeAll(clientSocket, serverSocket)
      }
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
