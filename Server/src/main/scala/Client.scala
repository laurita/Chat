import akka.actor.{ActorLogging, Actor}
import Messages._
import Parsing._
import java.net.Socket
import java.io.{BufferedInputStream, DataInputStream, BufferedOutputStream, DataOutputStream}

object Client{

}

class Client(clientSocket: Socket) extends Actor with ActorLogging {
  log.info("created")

  // create data input and output streams on socket waiting for login connections
  val out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream))
  val in = new DataInputStream(
    new BufferedInputStream(clientSocket.getInputStream))

  // get command and message in bytes
  val cmd = in.readByte()
  val loginName = readMessage(in)

  // send a Parse message containing command and message to self
  // self is in notLoggedIn mode
  self ! Parse(cmd, loginName)

  // first, receive behavior is notLoggedIn
  override def receive: Receive = notLoggedIn


  def notLoggedIn: Receive = {

    case Parse(command, message) =>
      log.info(s"got Parse($command, "+ message.toList +") when notLoggedIn")

      command match {
        // login
        case 1 =>
          val username = byteArrayToString(message)
          val server = context.parent
          server ! Login(username)

        // send
        case 3 =>
          log.info("Parse(3, ...) not implemented")

        // unknown
        case c =>
          log.info("unimplemented command "+ c)
      }

    case UserCreated(name) =>
      log.info(s"got UserCreated($name)")
      context.become(loggedIn(name))
      // error code 0 is when OK
      val ats = Array[Byte](cmd, 0.toByte)
      out.write(ats)
      out.flush()
      self ! Listen

    case UserExists =>
      // error code 1 is when user exists
      val ats = Array[Byte](cmd, 1.toByte)
      out.write(ats)
      out.flush()

    // unknown
    case m =>
      log.info(s"got unknown message $m")

  }

  def loggedIn(name: String): Receive = {

    case Parse(command, message) =>
      log.info(s"got message Parse($command, "+ message.toList +")")

      command match {
        // login
        case 1 =>
          // already logged in
          log.info("already logged in")

        // send
        case 3 =>
          log.info("forwarding to others...")
          context.system.actorSelection("user/server") ! ForwardAll(name, message)

        // logout
        case 4 =>
          log.info(s"sends Server Logout($name)")
          context.system.actorSelection("user/server") ! Logout(name, out)
          context.become(notLoggedIn)

        // unknown
        case c =>
          log.info(s"unknown command $c")
      }

    case Listen =>
      if (in.available() != 0) {
        val cmd = in.readByte()
        val bytes = readMessage(in)
        self ! Parse(cmd, bytes)
      }
      self ! Listen

    case Message(bytes: Array[Byte]) =>
      log.info("got message: "+ bytes.toList)
      // send to backend through TCP socket
      out.write(bytes)
      out.flush()

    // unknown
    case m =>
      log.info(s"got unknown message $m")
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
}
