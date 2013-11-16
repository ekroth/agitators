package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._

case class Bunny(x: Int, y: Int, w: Float)

object Application extends Controller {

  val bunnyForm = Form(
    mapping(
      "x" -> number,
      "y" -> number,
      "w" -> of[Float]
    )(Bunny.apply)(Bunny.unapply)
  )


  def index = register


  def teddies(i: Int) = Action {
    Ok(views.html.teddies(i))
  }


  def register = Action {
    Ok(views.html.register(bunnyForm))
  }

  def submit = Action { implicit request =>
    val bunny = bunnyForm.bindFromRequest.get
    Ok(s"Bunny: $bunny")
  }
}
