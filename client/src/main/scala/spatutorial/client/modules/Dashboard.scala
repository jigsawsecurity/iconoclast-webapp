package spatutorial.client.modules

import diode.data.Pot
import diode.react._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import spatutorial.client.SPAMain.{Loc, TodoLoc}
import spatutorial.client.components._

import scala.language.existentials
import scala.util.Random

object Dashboard {

  case class Props(router: RouterCtl[Loc], proxy: ModelProxy[Pot[String]])

  case class State(motdWrapper: ReactConnectProxy[Pot[String]])

  // create dummy data for the chart
  val cp = Chart.ChartProps(
    "Estimated Age Distribution",
    Chart.BarChart,
    ChartData(
      (7 to 30) map (_.toString),
      Seq(ChartDataset(Iterator.continually(Random.nextDouble() * 80).take(24).toSeq, "Images / Age Distribution"))
    )
  )

  // create the React component for Dashboard
  private val component = ReactComponentB[Props]("Dashboard")
    // create and store the connect proxy in state for later use
    .initialState_P(props => State(props.proxy.connect(m => m)))
    .renderPS { (_, props, state) =>
      <.div(
        // header, MessageOfTheDay and chart components
        <.h2("Dashboard"),
        state.motdWrapper(Motd(_)),
        Chart(cp),
        // create a link to the To Do view
        <.div(props.router.link(TodoLoc)("Review analyzed images >>"))
      )
    }
    .build

  def apply(router: RouterCtl[Loc], proxy: ModelProxy[Pot[String]]) = component(Props(router, proxy))
}
