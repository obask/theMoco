import com.twitter.finagle._
import com.twitter.finagle.http
import com.twitter.finagle.http.path.{/, Long, Root}
import com.twitter.util.{Await, Future}
import com.twitter.finagle.http.service.RoutingService


object Main extends App {

  private val OAUTH_TOKEN = sys.env.getOrElse("STRAVA_ACCESS_TOKEN",
    throw new Exception("STRAVA_ACCESS_TOKEN doesn't set in environment")
  )

  private val servicePort = 8080
  private val stravaWrapper = new StravaWrapper(OAUTH_TOKEN)
  private val controller = new MicroController(stravaWrapper)


  def userService(activity: Long) = {
      // TODO check empty list
    controller.findMostPopularRoute(activity)
      .map {
        case Some(result) =>
          val rep = http.Response(http.Status.Ok)
          rep.setContentString(result.toString)
          rep
        case None =>
          http.Response(http.Status.NotFound)
    }
  }

  val router = RoutingService.byPathObject[http.Request] {
    case Root / "most_contested" / Long(id) => new Service[http.Request, http.Response] {
      def apply(req: http.Request): Future[http.Response] = {
        userService(id)
      }
    }
    case _ => new Service[http.Request, http.Response] {
      def apply(request: http.Request) = {
        Future(http.Response(http.Status.NotFound))
      }
    }
  }

  val server = Http.serve(":" + servicePort.toString, router)
  Await.ready(server)

}
