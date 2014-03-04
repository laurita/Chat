import akka.actor.{Props, ActorSystem}
import org.mashupbots.socko.routes._
import org.mashupbots.socko.webserver.{WebServerConfig, WebServer}
import scala.Some

object WebSocketClientApp {

  // define actor system
  val system = ActorSystem("system")

  // define routes
  def routes = Routes({

    case HttpRequest(request) => request match {

      case GET(Path("/")) =>

        system.actorOf(Props[RequestHandler]) ! request

    }

    case WebSocketHandshake(wsHandshake) => wsHandshake match {
      case Path("/websocket/") => {
        // Authorize the handshake before starting Web socket processing.
        wsHandshake.authorize(
          onComplete = Some(onWebSocketHandshakeComplete),
          onClose = Some(onWebSocketClose))
      }
    }

    case WebSocketFrame(wsFrame) => {
      // Once handshaking has taken place, we can now process frames sent from the client
      system.actorOf(Props[RequestHandler]) ! wsFrame
    }
  })

  // create web server
  val webServer = new WebServer(WebServerConfig(), routes, system)


  // start (and stop) Socko web server
  def main(args: Array[String]) {
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run() { webServer.stop() }
    })
    webServer.start()

    System.out.println("Open a few browsers and navigate to http://localhost:8888. Start chatting!")
  }

  def onWebSocketHandshakeComplete(webSocketId: String) {
    System.out.println(s"Web Socket $webSocketId connected")
  }

  def onWebSocketClose(webSocketId: String) {
    System.out.println(s"Web Socket $webSocketId closed")
  }

}
