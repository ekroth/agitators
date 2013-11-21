package org.agitators
package pong

import io.backchat.hookup._

import akka.actor._
import akka.routing._
import akka.event.Logging

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.{read, write}

object PongProt {
  case class Rect(p: Vec2 = Vec2(), w: Int = 0, h: Int = 0) {

    def x1 = p.x
    def x2 = p.x + w
    def y1 = p.y
    def y2 = p.y + h

    def <#>(o: Rect): Boolean = {
      x1 < o.x2 && x2 > o.x1 &&
      y1 < o.y2 && y2 > o.y1
    }
  }

  case class Vec2(x: Int = 0, y: Int = 0) {
    def +(o: Vec2) = Vec2(x + o.x, y + o.y)
    def -(o: Vec2) = Vec2(x - o.x, y - o.y)
    def ix = copy(x = -x)
    def iy = copy(y = -y)
  }

  case class Ball(box: Rect = Rect(), v: Vec2 = Vec2()) {
    def moved = copy(box = box.copy(p = box.p + v))
  }

  case class Tile(box: Rect)
  case class World(plane: Rect, ball: Ball, p1: Tile, p2: Tile)
  case class Move(dx: Int, dy: Int)
  case class Connect()
  case class Disconnect()
  case class ClientWorld(b: Vec2, p1: Vec2, p2: Vec2)
}

class Client(srv: ActorRef, client: HookupServerClient) extends Actor with ActorLogging {

  import PongProt._

  implicit val formats = DefaultFormats

  srv ! Listen(self)
  srv ! Connect()

  override def receive = {

    case Disconnected(_) => {
      srv ! Deafen(self)
      srv ! Disconnect()
      context.stop(self)
    }

    case w@World(_, b, p1, p2) =>  { 
      client.send(write(ClientWorld(b.box.p, p1.box.p, p2.box.p)))
      log.info("sent pkg!")
    }

    case JsonMessage(m) => {
      log.info("got msg!")
      srv ! m.extract[Move]
    }

  }
}

class Server extends Actor with Listeners with ActorLogging {
  import scala.collection._
  import scala.concurrent.duration._

  import PongProt._

  val updateDelay = 20
  var lastUpdate: Long = 0

  val winit = World(
    Rect(Vec2(0, 0), 400, 300), 
    Ball(Rect(Vec2(200, 150), 30, 30), Vec2(-10, 10)),
    Tile(Rect(Vec2(10, 150), 30, 70)),
    Tile(Rect(Vec2(360, 150), 30, 70)))

  var w = winit

  var p1: Option[ActorRef] = None
  var p2: Option[ActorRef] = None

  context.setReceiveTimeout(20 millisecond)

  override def receive = listenerManagement orElse {
    case Connect() => {
      log.info("got connection")
      if (p1.isEmpty) p1 = Some(sender)
      else if (p2.isEmpty) p2 = Some(sender)
    }

    case Disconnect() => {
      log.info("got disconnect")
      val rep: ActorRef => Option[ActorRef] = { r =>
        if (r == sender) None
        else Some(r)
      }

      p1 = p1 flatMap rep
      p2 = p2 flatMap rep
    }

    case Move(x, y) => {
      p1 foreach { r =>
        if (r == sender) {
          val dp = w.p1.box.p + Vec2(x, y)
          val t = Tile(w.p1.box.copy(p = dp))
          w = w.copy(p1 = t)
        }
      }

      p2 foreach { r =>
        if (r == sender) {
          val dp = w.p2.box.p + Vec2(x, y)
          val t = Tile(w.p2.box.copy(p = dp))
          w = w.copy(p2 = t)
        }
      }

      update()
    }

    case ReceiveTimeout => {
      update()
    }
  } 

  def update() {
    if (System.currentTimeMillis - lastUpdate >= updateDelay) {
      lastUpdate = System.currentTimeMillis
      val dp = w.ball match {
        case b if (b.box.x2 + b.box.w <= w.plane.x1 && b.v.x < 0) || (b.box.x1 - b.box.w >= w.plane.x2 && b.v.x > 0) => winit.ball
        case b if (b.box.y1 <= w.plane.y1 && b.v.y < 0) || (b.box.y2 >= w.plane.y2 && b.v.y > 0) => b.copy(v = b.v.iy)
        case b if (b.box <#> w.p1.box && b.v.x < 0)     || (b.box <#> w.p2.box && b.v.x > 0)     => b.copy(v = b.v.ix)
        case b => b
      }

      w = w.copy(ball = dp.moved)
      gossip(w)
    }
  }
}

object Pong {

  val system = ActorSystem("ChatSystem")
  val master = system.actorOf(Props(new Server()))

  val websockets = HookupServer(3000) {
    new HookupServer.ActorHookupServerClient with HookupServerClient {

      override protected def actorFactory = { c =>
        system.actorOf(Props(new Client(master, c)))
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
