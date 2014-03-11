package actors

import akka.actor.{ActorLogging, Actor}
import helpers.Messages._
import java.io.DataOutputStream

class SocketWriter(out: DataOutputStream) extends Actor with ActorLogging {

  def receive: Receive = {

    case MessageWithByteArray(byteArray) =>
      log.info(s"SocketWriter received MessageWithByteArray("+ byteArray.toList +")")

      val sl = context.system.actorSelection("user/mainActor/socketListener")

      out.write(byteArray)
      out.flush()

      val cmd = byteArray(0)

      cmd match {
        // login
        case 1 =>
              sl ! WaitForACK(byteArray)

        // send
        case 3 =>
          sl ! ListenForChatMessages

        // logout
        case 4 =>
          log.info(s"SocketWriter matched $cmd")
          log.info(s"SocketListener is: $sl")
          sl ! WaitForACK(byteArray)

        // unknown
        case c =>
          log.info(s"unknown command $c")
      }
  }

}
