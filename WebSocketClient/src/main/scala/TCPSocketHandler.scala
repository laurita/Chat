import akka.actor.{ActorLogging, Actor}
import java.io.DataInputStream
import Parsing._
import scala.util.parsing.json.JSONObject

class TCPSocketHandler(in: DataInputStream, webSocketId: String) extends Actor with ActorLogging {
  override def receive: Actor.Receive = {

    case StartListen =>

      if (in.available() != 0) {

        // action byte
        // TODO: check more later
        in.readByte()
        val username = byteArrayToString(readMessage(in))
        val message = byteArrayToString(readMessage(in))

        val jsonString = makeJSONString(username, message)

        WebSocketClientApp.webServer.webSocketConnections.writeText(jsonString ,webSocketId)

      }
      self ! StartListen

  }

  // reads 4 bytes from given input stream to get length N of message
  // returns next N bytes (message)
  private def readMessage(in: DataInputStream): Array[Byte] = {
    val lenBytes = new Array[Byte](4)
    in.read(lenBytes)
    val len = Integer.parseInt(lenBytes.map(a => toBinary(a.toInt, 8)).mkString, 2)
    val bytes = new Array[Byte](len)
    in.read(bytes, 0, len)
    bytes
  }

  def makeJSONString(username: String, message: String): String = {
    val map = Map[String, Any](
      "action" -> "send",
      "params" -> JSONObject(
        Map[String, String]("name" -> username, "message" -> message))
    )
    JSONObject(map).toString()
  }

}
