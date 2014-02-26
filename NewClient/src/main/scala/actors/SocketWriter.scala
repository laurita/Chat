package actors

import akka.actor.{ActorLogging, ActorRef, Actor}
import messages.Messages.{WriteToSocket, WaitForACK, MessageWithByteArray}
import java.io.DataOutputStream
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await

/**
 * Created by laura on 25/02/14.
 */
class SocketWriter(out: DataOutputStream) extends Actor with ActorLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  def receive: Receive = {
    case MessageWithByteArray(byteArray) => {
      val lst = byteArray.toList
      log.info(s"SocketWriter received MessageWithBytArray($lst)")
      implicit val timeout = Timeout(3.seconds)
      val socketListenerFuture = context.system.actorSelection("user/mainActor/socketListener").resolveOne(3.seconds)
      val socketListenerRes = Await.result(socketListenerFuture, 3.seconds).asInstanceOf[ActorRef]

      //context.system.scheduler.scheduleOnce(1.second, self, WriteToSocket(byteArray))

      //self ! WriteToSocket(byteArray)


      // TODO: WHY THE HELL IT CANNOT FLUSH?????
      println(out +" size: "+ out.size())
      out.write(byteArray)
      println(out +" size: "+ out.size())
      out.flush()
      println(out +" size: "+ out.size())

      socketListenerRes match {
        case sl: ActorRef => {
          //val cmdByte = byteArray(0)
          sl ! WaitForACK(byteArray)
        }
      }

    }
/*
    case WriteToSocket(byteArray) => {
      val lst = byteArray.toList
      log.info(s"SocketWriter received WriteToSocket($lst)")
      out.write(byteArray)
      out.flush()
    }
*/
  }

}
