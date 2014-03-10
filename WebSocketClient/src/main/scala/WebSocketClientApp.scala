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

  val socket = new Socket(host, port)

  val out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream))
  val in = new DataInputStream(new BufferedInputStream(socket.getInputStream))
  val stdIn = new BufferedReader(new InputStreamReader(System.in))

  val actorSystem = ActorSystem("ChatExampleActorSystem")

  actorSystem.actorOf(Props(new SocketListener(in)), name=s"socketListener") ! ListenForChatMessages

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

      log.info("got WebSocketFrame "+ wsFrame)

      val webSocketId = wsFrame.webSocketId
      val wsrh = actorSystem.actorSelection(s"user/socketRequestHandler$webSocketId")

      log.info("found WebSocketRequestHandler: "+ wsrh)
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
    actorSystem.actorOf(Props(new WebSocketRequestHandler(webSocketId)), name=s"socketRequestHandler$webSocketId")
    System.out.println(s"Web Socket $webSocketId connected")
  }

  def onWebSocketClose(webSocketId: String) {
    System.out.println(s"Web Socket $webSocketId closed")
    actorSystem.actorSelection(s"user/$webSocketId") ! PoisonPill
  }

}