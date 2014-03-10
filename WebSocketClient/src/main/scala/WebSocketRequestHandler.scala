import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import org.mashupbots.socko.events.WebSocketFrameEvent
import akka.actor.{Props, ActorLogging, Actor}
import scala.concurrent._
import duration._
import scala.util.parsing.json.JSON
import akka.pattern.ask
import akka.util.Timeout

case class Push(text: String)

/**
 * Web Socket processor for chatting
 */
class WebSocketRequestHandler(webSocketId: String) extends Actor with ActorLogging {

  log.info("WebSocketRequestHandler created")

  override def receive = notLoggedIn

  def notLoggedIn: Receive = {
    case event: WebSocketFrameEvent =>

      // get JSON from response data
      val jsonStr = event.readText()

      // login remotely, get response
      implicit val timeout = Timeout(3.seconds)
      val successF = context.system.actorOf(Props[SocketWriter]) ? Login(jsonStr)
      val success = Await.result(successF, 3 seconds).asInstanceOf[Boolean]

      // if OK, send response back to WebSocketRequestHandler
      if (success) {
        // get username in order to change receive behavior to contain it
        val json: Option[Any] = JSON.parseFull(jsonStr)
        val map: Map[String,Any] = json.get.asInstanceOf[Map[String, Any]]
        val params = map.get("params").get.asInstanceOf[Map[String, Any]]
        val username = params.get("name").get.asInstanceOf[String]
        // change receive behavior
        context.become(loggedIn(username))
        // make json string for response to browser
        val jsonResString: String = "{\"action\":\"login\", \"params\": {\"success\":\"true\"}}"
        // respond to browser about success
        WebSocketClientApp.webServer.webSocketConnections.writeText(jsonResString, webSocketId)
      }

  }

  def loggedIn(username: String): Receive = {

    /*
    case Push(text) =>
      log.info("Push")
      pushTwice(webSocketId, text)
    */

    case event: WebSocketFrameEvent =>
      // TODO: send to backend server
      // get JSON from response data
      val jsonStr = event.readText()

      // create new JSON String that contains username
      /*
      val json: Option[Any] = JSON.parseFull(jsonStr)
      val map: Map[String,Any] = json.get.asInstanceOf[Map[String, Any]]
      val params = map.get("params").get.asInstanceOf[Map[String, Any]]
      val newMap = map + ("params" -> (params + ("name" -> username)))
      log.info("newMap with username: "+ newMap)
      val newJSON = new JSONObject(newMap)
      val newJSONStr = newJSON.toString()
      */
      // send JSON String with username to socket writer
      //context.system.actorOf(Props[SocketWriter]) ! ChatMessage(newJSONStr)

      context.system.actorOf(Props[SocketWriter]) ! ChatMessage(jsonStr)
    // Echo web socket text frames
    //writeWebSocketResponse(event)
    //context.stop(self)
  }

  /**
   * push messages every second
   */
  /*
  private def pushTwice(webSocketId: String, text: String) {
    val cancellable = context.system.scheduler.schedule(0 milliseconds,
      50 milliseconds)(ChatApp.webServer.webSocketConnections.writeText(text, webSocketId))
  }
  */

  /**
   * Echo the details of the web socket frame that we just received; but in upper case.
   */
  def writeWebSocketResponse(event: WebSocketFrameEvent) {
    log.info("TextWebSocketFrame: " + event.readText)

    val dateFormatter = new SimpleDateFormat("HH:mm:ss")
    val time = new GregorianCalendar()
    val ts = dateFormatter.format(time.getTime)

    WebSocketClientApp.webServer.webSocketConnections.writeText(ts + " " + event.readText)
  }

  private def getUsernameFromJSON(event: WebSocketFrameEvent): String = {
    val username: String = try {
      val jsonString = event.readText()
      val json: Option[Any] = JSON.parseFull(jsonString)
      val map: Map[String,Any] = json.get.asInstanceOf[Map[String, Any]]
      map.get("username").toString
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw e
    }
    username
  }

}



