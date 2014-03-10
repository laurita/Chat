import org.mashupbots.socko.events.HttpRequestEvent
import akka.actor.Actor
import akka.event.Logging

/**
 * Web Socket processor for chatting
 */
class HTTPRequestHandler extends Actor {
  val log = Logging(context.system, this)

  /**
   * Process incoming events
   */
  def receive: Receive =  {
    case event: HttpRequestEvent =>
      // return login HTML page to setup web sockets in the browser
      val htmlText = buildLoginPageHTML()
      writeHTML(event, htmlText)
      context.stop(self)

    /*
    case event: HttpRequestEvent =>
      // Return the HTML page to setup web sockets in the browser
      val htmlText = buildChatPageHTML()
      writeHTML(event, htmlText)
      context.stop(self)
    */

    case _ =>
      log.info("received unknown message of type: ")
      context.stop(self)

  }

  /**
   * Write HTML page to setup a web socket on the browser
   */
  private def writeHTML(ctx: HttpRequestEvent, htmlText: String) {
    // Send 100 continue if required
    if (ctx.request.is100ContinueExpected) {
      ctx.response.write100Continue()
    }

    ctx.response.write(htmlText, "text/html; charset=UTF-8")
  }

  /*
  private def buildChatPageHTML(): String = {
    val buf = new StringBuilder()
    buf.append("<html><head><title>Socko Web Socket Example</title></head>\n")
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
    buf.append("<h1>Socko Web Socket Chat Example</h1>\n")
    buf.append("<form onsubmit=\"return false;\">\n")
    buf.append(" <input type=\"text\" name=\"message\" value=\"Hello, World!\"/>\n")
    buf.append(" <input type=\"button\" value=\"Chat\" onclick=\"send(this.form.message.value)\" />\n")
    buf.append(" \n")
    buf.append(" <h3>Output</h3>\n")
    buf.append(" <textarea id=\"responseText\" style=\"width: 500px; height:300px;\"></textarea>\n")
    buf.append("</form>\n")
    buf.append("</body>\n")
    buf.append("</html>\n")

    buf.toString()
  }
  */

  private def buildLoginPageHTML(): String = {
    val source = scala.io.Source.fromFile("assets/index.html")
    val lines = source.mkString
    source.close()
    lines

  }


}