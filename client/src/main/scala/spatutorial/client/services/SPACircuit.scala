package spatutorial.client.services
import autowire._
import boopickle.Default._
import diode._
import diode.data._
import diode.react.ReactConnector
import diode.util._
import spatutorial.shared.{Api, Image}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

// Actions
case object RefreshImages extends Action

case class UpdateAllImages(todos: Seq[Image]) extends Action

case class UpdateImage(item: Image) extends Action

case class DeleteImage(item: Image) extends Action

case class UpdateMotd(potResult: Pot[String] = Empty) extends PotAction[String, UpdateMotd] {
  override def next(value: Pot[String]) = UpdateMotd(value)
}

// The base model of our application
case class RootModel(images: Pot[Images], motd: Pot[String])

case class Images(items: Seq[Image]) {
  def updated(newItem: Image) = {
    items.indexWhere(_.id == newItem.id) match {
      case -1 =>
        // add new
        Images(items :+ newItem)
      case idx =>
        // replace old
        Images(items.updated(idx, newItem))
    }
  }

  def remove(item: Image) = Images(items.filterNot(_ == item))
}

/**
  * Handles actions related to todos
  *
  * @param modelRW Reader/Writer to access the model
  */
class ImageHandler[M](modelRW: ModelRW[M, Pot[Images]]) extends ActionHandler(modelRW) {
  override def handle = {
    case RefreshImages =>
      effectOnly(Effect(AjaxClient[Api].getAllImages().call().map(UpdateAllImages)))
    case UpdateAllImages(todos) =>
      // got new todos, update model
      updated(Ready(Images(todos)))
    case UpdateImage(item) =>
      // make a local update and inform server
      updated(value.map(_.updated(item)), Effect(AjaxClient[Api].updateImage(item).call().map(UpdateAllImages)))
    case DeleteImage(item) =>
      // make a local update and inform server
      updated(value.map(_.remove(item)), Effect(AjaxClient[Api].deleteImage(item.id).call().map(UpdateAllImages)))
  }
}

/**
  * Handles actions related to the Motd
  *
  * @param modelRW Reader/Writer to access the model
  */
class MotdHandler[M](modelRW: ModelRW[M, Pot[String]]) extends ActionHandler(modelRW) {
  implicit val runner = new RunAfterJS

  override def handle = {
    case action: UpdateMotd =>
      val updateF = action.effect(AjaxClient[Api].welcomeMsg("User X").call())(identity _)
      action.handleWith(this, updateF)(PotAction.handler())
  }
}

// Application circuit
object SPACircuit extends Circuit[RootModel] with ReactConnector[RootModel] {
  // initial application model
  override protected def initialModel = RootModel(Empty, Empty)
  // combine all handlers into one
  override protected val actionHandler = composeHandlers(
    new ImageHandler(zoomRW(_.images)((m, v) => m.copy(images = v))),
    new MotdHandler(zoomRW(_.motd)((m, v) => m.copy(motd = v)))
  )
}