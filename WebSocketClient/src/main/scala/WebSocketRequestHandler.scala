import java.io.{DataOutputStream, DataInputStream}
import org.mashupbots.socko.events.WebSocketFrameEvent
import akka.actor.{Props, ActorLogging, Actor}
import scala.util.parsing.json.JSON

case object ListenForConfirmationBytes
case object StartListen

class WebSocketRequestHandler(webSocketId: String, in: DataInputStream, out: DataOutputStream)
  extends Actor with ActorLogging {

  log.info("WebSocketRequestHandler created")

  val commandCodes = Map[String, Byte](
    "login" -> 1,
    "send" -> 3,
    "logout" -> 4
  )

  override def receive = notLoggedIn

  def notLoggedIn: Receive = {

    case event: WebSocketFrameEvent =>
      log.info(s"got WebSocketFrame in notLoggedIn mode")
      // parse JSON from response data
      val jsonStr = event.readText()
      val username = getNameFromJSON(jsonStr)
      val action = getActionFromJSON(jsonStr)
      log.info(s"JSON is $jsonStr")
      // make byte array message to send through TCP socket
      val bytes = Array[Byte](commandCodes(action)) ++ makeByteMessageWithLengthPad(username)
      // change receive behavior to wait for confirmation
      context.become(waitForLoginConfirmation(jsonStr))
      self ! ListenForConfirmationBytes
      // login remotely, get response
      out.write(bytes)
      out.flush()
      log.info(s"flushed "+ bytes.toList)

    case m =>
      log.info(s"got unknown message $m in notLoggedIn mode")
  }

  def waitForLoginConfirmation(jsonStr: String): Receive = {

    case ListenForConfirmationBytes =>

      if (in.available() != 0) {

        val cmd = in.readByte()
        val error = in.readByte()

        (cmd, error) match {

          // login success
          case (1, 0) =>
            log.info(s"$jsonStr logged in successfully")
            // find actor for listening TCP messages
            context.system.actorSelection(s"tcpSocketHandler$webSocketId") ! StartListen
            val name = getNameFromJSON(jsonStr)
            context.become(loggedIn(name))
            // make json string for response to browser
            val jsonResString: String = "{\"action\":\"login\", \"params\": {\"success\":\"true\"}}"
            // respond to browser about success
            WebSocketClientApp.webServer.webSocketConnections.writeText(jsonResString, webSocketId)
            log.info(s"sent back to browser $jsonResString")

          // login failed
          case (1, 1) =>
            log.info(s"login failed for $jsonStr")

          // logout success
          case (4, 0) =>
            log.info(s"logout success ($cmd, $error)")
            // make json string for response to browser
            val jsonResString: String = "{\"action\":\"logout\", \"params\": {\"success\":\"true\"}}"
            // change receive behavior
            context.become(notLoggedIn)
            // respond to browser about success
            WebSocketClientApp.webServer.webSocketConnections.writeText(jsonResString, webSocketId)
            log.info(s"sent back to browser $jsonResString")

          // logout failed
          case (4, 1) =>
            log.info(s"logout failure ($cmd, $error)")

          // unknown
          case c =>
            log.info(s"unknown (cmd, error) $c")
        }
      } else {
        self ! ListenForConfirmationBytes
      }

    case m =>
      log.info(s"got unknown message $m in waitForLoginConfirmation mode")
  }

  def loggedIn(username: String): Receive = {

    case event: WebSocketFrameEvent =>
      log.info(s"got WebSocketFrame in loggedIn mode")

      // get JSON from response data
      val jsonStr = event.readText()
      // parse JSON
      val action = getActionFromJSON(jsonStr)

      action match {
        case "logout" =>
          val name = getNameFromJSON(jsonStr)
          val nameBytes = makeByteMessageWithLengthPad(name)
          val bytes = Array[Byte](commandCodes(action)) ++ nameBytes
          context.become(waitForLoginConfirmation(jsonStr))
          self ! ListenForConfirmationBytes
          // send to socket writer
          out.write(bytes)
          out.flush()
          log.info(s"flushed message "+ bytes.toList)

        case "send" =>
          val message = getMessageFromJSON(jsonStr)
          val messageBytes = makeByteMessageWithLengthPad(message)
          val bytes = Array[Byte](commandCodes(action)) ++ messageBytes
          // send to socket writer
          out.write(bytes)
          out.flush()
          log.info(s"flushed message "+ bytes.toList)

      }

    case m =>
      log.info(s"got unknown message $m in logged in mode")
  }

  //------------------------------------------------------------------------------------------------------------------//

  private def makeByteMessageWithLengthPad(message: String): Array[Byte] = {
    intToByteArray(message.length) ++ message.getBytes("UTF-8")
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

  private def getNameFromJSON(jsonStr: String): String = {
    val json: Option[Any] = JSON.parseFull(jsonStr)
    val map: Map[String,Any] = json.get.asInstanceOf[Map[String, Any]]
    val params = map.get("params").get.asInstanceOf[Map[String, Any]]
    params.get("name").get.asInstanceOf[String]
  }

  private def getMessageFromJSON(jsonStr: String): String = {
    val json: Option[Any] = JSON.parseFull(jsonStr)
    val map: Map[String,Any] = json.get.asInstanceOf[Map[String, Any]]
    val params = map.get("params").get.asInstanceOf[Map[String, Any]]
    params.get("message").get.asInstanceOf[String]
  }

  private def getActionFromJSON(jsonStr: String): String = {
    val json: Option[Any] = JSON.parseFull(jsonStr)
    val map: Map[String,Any] = json.get.asInstanceOf[Map[String, Any]]
    map.get("action").get.asInstanceOf[String]
  }

}



