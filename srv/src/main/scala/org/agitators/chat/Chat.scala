package org.agitators
package chat

import io.backchat.hookup._

import akka.actor._
import akka.routing._

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.{read, write}

object ChatProt {
  case class Message(nick: String, msg: String)
}

class Hooker(srv: ActorRef, client: HookupServerClient) extends Actor {

  import ChatProt._

  implicit val formats = DefaultFormats

  srv ! Listen(self)

  override def receive = {

    case Disconnected(_) => {
      srv ! Deafen(self)
      context.stop(self)
    }

    case m: Message => client.send(write(m))

    case JsonMessage(m) => srv ! m.extract[Message]

  }
}

class Server extends Actor with Listeners {
  import scala.collection._

  import ChatProt._

  override def receive = listenerManagement orElse {
    case m: Message => gossip(m)
  } 

}

object Chat {

  val system = ActorSystem("ChatSystem")

  val master = system.actorOf(Props(new Server()))

  val hookSrv = HookupServer(3000) {
    new HookupServer.ActorHookupServerClient with HookupServerClient {

      override protected def actorFactory = { c =>
        system.actorOf(Props(new Hooker(master, c)))
      }
    }
  }

  /*
   *  hookSrv.start
   *  ......
   *  hookSrv.end
   *  system.shutdown
   */
}
