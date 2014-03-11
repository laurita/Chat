import akka.actor.{Props, ActorLogging, Actor}
import java.io.DataInputStream
import scala.util.parsing.json.JSONObject

object SocketListener {
  val Success: Byte = 0
}

case object ListenForChatMessages

class SocketListener(in: DataInputStream) extends Actor with ActorLogging {

  log.info("SocketListener created")

  def receive = started()

  def started(): Receive = {

    case ListenForChatMessages => {
      //log.info("got ListenForChatMessages")
      if (in.available() != 0) {
        val cmd = in.readByte()
        val senderLenBytes = new Array[Byte](4)
        in.read(senderLenBytes)
        val senderLen = Integer.parseInt(senderLenBytes.map(a => toBinary(a.toInt, 8)).mkString, 2)
        val senderBytes = new Array[Byte](senderLen)
        in.read(senderBytes, 0, senderLen)
        val senderLst = senderBytes.toList
        val lenBytes = new Array[Byte](4)
        in.read(lenBytes)
        val len = Integer.parseInt(lenBytes.map(a => toBinary(a.toInt, 8)).mkString, 2)
        val bytes = new Array[Byte](len)
        in.read(bytes, 0, len)
        val lst = bytes.toList
        log.info(s"cmdId: $cmd, senderLen: $senderLen, sender: $senderLst, length: $len, msg: $lst")

        val senderName = byteArrayToString(senderBytes)
        val message = byteArrayToString(bytes)
        val jsonObject = JSONObject(
          Map[String, Any]("action" -> "send", "params" ->
            JSONObject(Map[String, Any]("name" -> senderName, "message" -> message))))

        val jsonStr = jsonObject.toString()
        log.info("json string: "+ jsonStr)
        log.info("web socket connections: "+ WebSocketClientApp.webServer.webSocketConnections)
        WebSocketClientApp.webServer.webSocketConnections.writeText(jsonStr)
      }

      self ! ListenForChatMessages
    }
  }

  private def byteArrayToString(byteArray: Array[Byte]): String = {
    byteArray.toList.map(x => x.toChar).mkString
  }

  private def toBinary(i: Int, digits: Int = 8) =
    String.format("%" + digits + "s", i.toBinaryString).replace(' ', '0')
}
