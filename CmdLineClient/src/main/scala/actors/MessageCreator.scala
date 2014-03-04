package actors

import akka.actor.{ActorLogging, ActorRef, Actor}
import messages.Messages.{MessageWithByteArray, CreateMessage}
import akka.util.Timeout
import scala.concurrent.duration._

class MessageCreator extends Actor with ActorLogging {

  val commandCodes = Map(
  "login" -> 1.toByte,
  "send" -> 3.toByte,
  "logout" -> 4.toByte
  )

  def receive: Receive = {
    case CreateMessage(command, message) => {

      log.info(s"MessageCreator received CreateMessage($command, $message)")
      implicit val timeout = Timeout(3.seconds)
      val socketWriter = context.system.actorSelection("user/mainActor/socketWriter")

      val msgByteArray =
        Array(commandCodes(command)) ++ intToByteArray(message.length) ++ message.getBytes("UTF-8")

      // TODO: continue here
      socketWriter ! MessageWithByteArray(msgByteArray)

    }
  }

  private def intToByteArray(x: Int): Array[Byte] = {

    val binaryStr = x.toBinaryString
    val pad = "0" * (32 - binaryStr.length)

    val fullBinStr = pad + binaryStr

    splitToStringsOfLen(fullBinStr, 8).map(x => Integer.parseInt(x, 2).toByte).toArray
  }

  private def splitToStringsOfLen(str: String, len: Int): List[String] = {
    def rec(str: String, acc: List[String]): List[String] = {
      str match {
        case "" => acc
          case string => {
            val tpl = string.splitAt(string.length - len)
            rec(tpl._1, tpl._2 :: acc)
          }
        }
      }
    rec(str, List())
   }

}