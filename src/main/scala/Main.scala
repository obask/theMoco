import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import com.twitter.finagle._
import com.twitter.finagle.http
import com.twitter.finagle.http.path.{/, Long, Root}
import com.twitter.util.{Await, Duration, Future}
import com.twitter.finagle.http.service.RoutingService


/**
  * Entry point of service starts routing requests on port 8080.
  */

object Main extends App {

  private val OAUTH_TOKEN = sys.env.getOrElse("STRAVA_ACCESS_TOKEN",
    throw new RuntimeException("STRAVA_ACCESS_TOKEN doesn't set in environment")
  )

  lazy val DEFAULT_TIMEOUT = Duration(7, TimeUnit.SECONDS)

  private val servicePort = 8080
  private val stravaWrapper = new StravaService(OAUTH_TOKEN)
  private val controller = new MostContestedController(stravaWrapper)

  val router = RoutingService.byPathObject[http.Request] {
    case Root / "most_contested" / Long(id) => new Service[http.Request, http.Response] {
      def apply(req: http.Request): Future[http.Response] = {
        controller.proceedUserRequest(id)
      }
    }
    case _ => new Service[http.Request, http.Response] {
      def apply(request: http.Request) = {
        Future(http.Response(http.Status.NotFound))
      }
    }
  }

  val server = Http.serve(new InetSocketAddress(servicePort), router)
  Await.ready(server)

}
