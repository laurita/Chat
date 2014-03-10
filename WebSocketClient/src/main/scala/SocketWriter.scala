import akka.actor.{ActorLogging, Actor}
import scala.util.parsing.json.JSON

class Message()
case class Login(jsonStr: String) extends Message
case class ChatMessage(jsonStr: String) extends Message

class SocketWriter extends Actor with ActorLogging {

  val out = WebSocketClientApp.out
  val in = WebSocketClientApp.in

  def receive: Receive = {
    case Login(jsonStr) =>
      log.info("SocketWriter got "+ jsonStr)
      val byteArray = makeByteArrayFromJSON(jsonStr)

      log.info("made byte array "+ byteArray.toList)
      if (!byteArray.isEmpty) {
        out.write(byteArray)
        out.flush()
      }

      while (in.available() == 0) {}

      val atsByteArray = new Array[Byte](2)
      in.read(atsByteArray)

      val cmd = atsByteArray(0)
      val error = atsByteArray(1)

      (cmd, error) match {
        // successful login
        case (1, 0) =>
          sender ! true

        // unsuccessful login
        case (1, 1) =>
          sender ! false


      }

    case ChatMessage(jsonStr) =>
      log.info("SocketWriter got "+ jsonStr)

      // make byte array from JSON string
      val byteArray = makeByteArrayFromJSON(jsonStr)

      // send byte array through socket
      log.info("made byte array "+ byteArray.toList)
      if (!byteArray.isEmpty) {
        out.write(byteArray)
        out.flush()
      }

  }

  private def makeByteArrayFromJSON(jsonStr: String): Array[Byte] = {

    val commandCodes = Map(
      "login" -> 1.toByte,
      "send" -> 3.toByte,
      "logout" -> 4.toByte
    )

    val byteArray: Array[Byte] = try {
      val json: Option[Any] = JSON.parseFull(jsonStr)
      val map: Map[String,Any] = json.get.asInstanceOf[Map[String, Any]]
      log.info("map: "+ map)
      val action = map.get("action").get.asInstanceOf[String]
      log.info("action: "+ action)
      val command = commandCodes(action)
      log.info("command: "+ command)
      val params = map.get("params").get.asInstanceOf[Map[String, Any]]
      command match {
        case 1 =>
          val username = params.get("name").get.asInstanceOf[String]
          log.info("username: "+ username)
          val msgByteArray =
            Array(command) ++ intToByteArray(username.length) ++ username.getBytes("UTF-8")
          log.info("msgByteArray: "+ msgByteArray)
          msgByteArray
        case 3 =>
          val message = params.get("message").get.asInstanceOf[String]
          log.info("message: "+ message)
          val msgByteArray =
            Array(command) ++ intToByteArray(message.length) ++ message.getBytes("UTF-8")
          msgByteArray
      }
    } catch {
      case e: Exception => Array[Byte]()
    }
    byteArray
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
        case string =>
          val tpl = string.splitAt(string.length - len)
          rec(tpl._1, tpl._2 :: acc)

      }
    }
    rec(str, List())
  }


}
