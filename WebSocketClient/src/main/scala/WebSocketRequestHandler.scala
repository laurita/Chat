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

class WebSocketRequestHandler(webSocketId: String) extends Actor with ActorLogging {

  log.info("WebSocketRequestHandler created")

  override def receive = notLoggedIn

  def notLoggedIn: Receive = {
    case event: WebSocketFrameEvent =>

      // get JSON from response data
      val jsonStr = event.readText()

      // login remotely, get response
      implicit val timeout = Timeout(10.seconds)
      val successF = context.system.actorOf(Props[SocketWriter]) ? Login(jsonStr)
      val success = Await.result(successF, 10 seconds).asInstanceOf[Boolean]

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
        // TODO:
        WebSocketClientApp.webServer.webSocketConnections.writeText(jsonResString, webSocketId)
      }

  }

  def loggedIn(username: String): Receive = {

    case event: WebSocketFrameEvent =>
      // TODO: send to backend server
      // get JSON from response data
      val jsonStr = event.readText()

      context.system.actorOf(Props[SocketWriter]) ! ChatMessage(jsonStr)
  }

  def writeWebSocketResponse(event: WebSocketFrameEvent) {
    log.info("TextWebSocketFrame: " + event.readText)

    val dateFormatter = new SimpleDateFormat("HH:mm:ss")
    val time = new GregorianCalendar()
    val ts = dateFormatter.format(time.getTime)

    WebSocketClientApp.webServer.webSocketConnections.writeText(ts + " " + event.readText)
  }

}



