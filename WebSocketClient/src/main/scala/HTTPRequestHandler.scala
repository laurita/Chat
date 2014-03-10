import org.mashupbots.socko.events.HttpRequestEvent
import akka.actor.Actor
import akka.event.Logging

class HTTPRequestHandler extends Actor {
  val log = Logging(context.system, this)

  def receive: Receive =  {
    case event: HttpRequestEvent =>
      // return login HTML page to setup web sockets in the browser
      val htmlText = buildLoginPageHTML()
      writeHTML(event, htmlText)
      context.stop(self)

    case _ =>
      log.info("received unknown message of type: ")
      context.stop(self)

  }

  private def writeHTML(ctx: HttpRequestEvent, htmlText: String) {
    // Send 100 continue if required
    if (ctx.request.is100ContinueExpected) {
      ctx.response.write100Continue()
    }

    ctx.response.write(htmlText, "text/html; charset=UTF-8")
  }

  private def buildLoginPageHTML(): String = {
    val source = scala.io.Source.fromFile("assets/index.html")
    val lines = source.mkString
    source.close()
    lines

  }


}