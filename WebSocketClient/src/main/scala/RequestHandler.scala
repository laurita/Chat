import akka.actor.{ActorLogging, Actor}
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import org.mashupbots.socko.events.{WebSocketFrameEvent, HttpRequestEvent}

class RequestHandler extends Actor with ActorLogging {

  override def receive: Receive = {

    case request: HttpRequestEvent => {
      // Return the HTML page to setup web sockets in the browser
      writeHTML(request)
      context.stop(self)
    }

    case event: WebSocketFrameEvent =>
      // Echo web socket text frames
      writeWebSocketResponse(event)
      context.stop(self)

    case _ => {
      log.info("received unknown message of type: ")
      context.stop(self)
    }
  }

  private def writeHTML(ctx: HttpRequestEvent) {
    // Send 100 continue if required
    if (ctx.request.is100ContinueExpected) {
      ctx.response.write100Continue()
    }

    val buf = new StringBuilder()
    buf.append("<html><head><title>Client Chat Application Using Socko with Web Sockets</title></head>\n")
    buf.append("<body>\n")
    buf.append("<script type=\"text/javascript\">\n")
    buf.append(" var socket;\n")
    buf.append(" if (!window.WebSocket) {\n")
    buf.append(" window.WebSocket = window.MozWebSocket;\n")
    buf.append(" }\n")
    buf.append(" if (window.WebSocket) {\n")
    buf.append(" socket = new WebSocket(\"ws://localhost:8888/websocket/\");\n") // Note the address must match the route
    buf.append(" socket.onmessage = function(event) { var ta = document.getElementById('responseText'); ta.value = ta.value + '\\n' + event.data };\n")
    buf.append(" socket.onopen = function(event) { var ta = document.getElementById('responseText'); ta.value = \"Web Socket opened!\"; };\n")
    buf.append(" socket.onclose = function(event) { var ta = document.getElementById('responseText'); ta.value = ta.value + \"Web Socket closed\"; };\n")
    buf.append(" } else { \n")
    buf.append(" alert(\"Your browser does not support Web Sockets.\");\n")
    buf.append(" }\n")
    buf.append(" \n")
    buf.append(" function send(message) {\n")
    buf.append(" if (!window.WebSocket) { return; }\n")
    buf.append(" if (socket.readyState == WebSocket.OPEN) {\n")
    buf.append(" socket.send(message);\n")
    buf.append(" } else {\n")
    buf.append(" alert(\"The socket is not open.\");\n")
    buf.append(" }\n")
    buf.append(" }\n")
    buf.append("</script>\n")
    buf.append("<h1>Client Chat Application Using Socko with Web Sockets</h1>\n")
    buf.append("<form onsubmit=\"return false;\">\n")
    buf.append(" <input type=\"text\" name=\"message\" value=\"Hello, World!\"/>\n")
    buf.append(" <input type=\"button\" value=\"Chat\" onclick=\"send(this.form.message.value)\" />\n")
    buf.append(" \n")
    buf.append(" <h3>Output</h3>\n")
    buf.append(" <textarea id=\"responseText\" style=\"width: 500px; height:300px;\"></textarea>\n")
    buf.append("</form>\n")
    buf.append("</body>\n")
    buf.append("</html>\n")

    ctx.response.write(buf.toString, "text/html; charset=UTF-8")
  }

  private def writeWebSocketResponse(request: WebSocketFrameEvent) {

    log.info("TextWebSocketFrame: " + request.readText)

    val dateFormatter = new SimpleDateFormat("HH:mm:ss")
    val time = new GregorianCalendar()
    val ts = dateFormatter.format(time.getTime())

    WebSocketClientApp.webServer.webSocketConnections.writeText(ts + " " + request.readText)
  }
}