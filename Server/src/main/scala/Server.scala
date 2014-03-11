import akka.actor._
import Messages._
import Parsing._
import ChatServerApp._
import java.io.DataOutputStream

object Server {

}

class Server extends Actor with ActorLogging {

  case class LoggedInUsers(usrList: List[ActorRef]) {

    private val userList = usrList

    def getList = userList

    def login(usr: ActorRef) = {
      LoggedInUsers(usr :: userList)
    }

    def logout(usr: ActorRef) = {
      val usersWithout = userList.filter(x => !x.equals(usr))
      LoggedInUsers(usersWithout)
    }
  }

  override def receive: Receive = listening(new LoggedInUsers(List[ActorRef]()))

  def listening(loggedInUsers: LoggedInUsers): Receive = {

    case CreateActor(clientSocket) =>
      log.info("got CreateActor message")

      // create client actor, pass a newly opened socket
      context.actorOf(Props(new Client(clientSocket)))

    case Login(name) =>
      log.info(s"got Login($name)")

      // try to get user with same name
      val user = loggedInUsers.getList.find(ar => ar.path.name == name)

      user match {

        // if no user with such name
        case None =>
          // login new user, send it's ref back
          log.info(s"User doesn't exists. Logging in $name...")
          context.become(listening(loggedInUsers.login(sender)))
          sender ! UserCreated(name)

        // if such user found
        case Some(ar) =>
          // send answer that user exists
          sender ! UserExists
      }

    case ForwardAll(from: String, bytes: Array[Byte]) =>
      log.info("Server got ForwardAll")

      val m = byteArrayToString(bytes)
      val msgByteArray =
        Array(commandCodes("receive")) ++
        intToByteArray(from.length) ++ from.getBytes("UTF-8") ++
        intToByteArray(m.length) ++ m.getBytes("UTF-8")

      // get all logged in users and forward message to them
      val buddies = context.children.filterNot(ar => ar == sender)
      buddies.foreach(b => b ! Message(msgByteArray))

    case Logout(name: String, out: DataOutputStream) =>
      log.info(s"Server got Logout($name)")

      val newRegUsers = loggedInUsers.logout(sender)
      context.become(listening(newRegUsers))
      val ats = Array[Byte](4, 0.toByte)
      // write answer to socket
      out.write(ats)
      out.flush()
      // close the socket
      out.close()

    case m =>
      log.info(s"got unknown message: $m")
  }
}
