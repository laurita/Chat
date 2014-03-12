import java.io._
import java.net.Socket
import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.routes._
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import akka.actor._
import scala.Some
import scala.Some

object WebSocketClientApp extends Logger {

  val host = "localhost"
  val port = 4567

  // create akka actor system
  val actorSystem = ActorSystem("ChatExampleActorSystem")

  // create routes
  val routes = Routes({

    case HttpRequest(httpRequest) => httpRequest match {
      case GET(Path("/")) =>
        // Return HTML page to establish web socket
        actorSystem.actorOf(Props[HTTPRequestHandler]) ! httpRequest

      case _ =>
        // If favicon.ico, just return a 404 because we don't have that file
        httpRequest.response.write(HttpResponseStatus.NOT_FOUND)

    }

    case WebSocketHandshake(wsHandshake) => wsHandshake match {
      case Path("/websocket/") =>

        wsHandshake.authorize(
          onComplete = Some(onWebSocketHandshakeComplete),
          onClose = Some(onWebSocketClose))
    }

    case WebSocketFrame(wsFrame) =>
      val webSocketId = wsFrame.webSocketId
      val wsrh = actorSystem.actorSelection(s"user/socketRequestHandler$webSocketId")
      wsrh ! wsFrame
  })

  val webServer = new WebServer(WebServerConfig(), routes, actorSystem)

  def main(args: Array[String]) {
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run() { webServer.stop() }
    })
    webServer.start()

    System.out.println("Open a few browsers and navigate to http://localhost:8888/. Start chatting!")
  }

  def onWebSocketHandshakeComplete(webSocketId: String) {
    // 1. open new TCP connection to backend
    val socket = new Socket(host, port)
    val out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream))
    val in = new DataInputStream(new BufferedInputStream(socket.getInputStream))
    // 2. create WebSocketRequestHandler and open new TCP socket
    actorSystem.actorOf(Props(new WebSocketRequestHandler(webSocketId, in, out)),
      name=s"socketRequestHandler$webSocketId")
    System.out.println(s"Web Socket $webSocketId connected")
  }

  def onWebSocketClose(webSocketId: String) {
    System.out.println(s"Web Socket $webSocketId closed")
    actorSystem.actorSelection(s"user/$webSocketId") ! PoisonPill
  }

}