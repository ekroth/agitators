package org.agitators

import io.backchat.hookup._

import akka.actor._

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.{read, write}

object ChatProt {
  case class Open(client: HookupServerClient)
  case class Message(nick: String, msg: String)
  case class Close(client: HookupServerClient)
  case class Broadcast(msg: Message)
}

class Hooker(srv: ActorRef, client: HookupServerClient) extends Actor {

  import ChatProt._

  implicit val formats = DefaultFormats

  srv ! Open(client)

  def receive = {

    case Disconnected(_) => {
      srv ! Close(client)
      context.stop(self)
    }

    case Broadcast(m) => {
      val str = write(m)
      println(s"sending $str")
      client.send(str)
    }

    case JsonMessage(m) => {
      val ex = m.extract[Message]
      println(s" got $ex")
      srv ! ex
    }

  }
}

final class Server extends Actor {
  import scala.collection._

  import ChatProt._

  val actors = mutable.HashMap[HookupServerClient, ActorRef]()

  override def receive = {

    case Open(client) => {
      println(s"got new client $client")
      actors(client) = sender
    }

    case Close(client) => {
      println(s"got close $client")
      actors -= client
    }

    case a: Message => actors foreach { x =>
      x._2 ! Broadcast(a)
    }
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
   * /
}
