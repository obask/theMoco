import java.util.concurrent.TimeUnit

import com.twitter.finagle.http
import com.twitter.util.Future

import scala.util.{Failure, Try}
import com.twitter.finagle.util.DefaultTimer

class MostContestedController(stravaWrapper: StravaService) {


  def proceedUserRequest(activity: Long): Future[http.Response] = {
    findMostPopularRoute(activity)
      .map {
        case Some(result) =>
          val rep = http.Response(http.Status.Ok)
          rep.setContentString(result.toString)
          rep
        case None =>
          http.Response(http.Status.NotFound)
      }
      .handle { case err: Throwable =>
        val rep = http.Response(http.Status.InternalServerError)
        rep.setContentString(err.toString)
        rep
      }
  }

  private def findMostPopularRoute(activity: Long): Future[Option[BigInt]] = {
    stravaWrapper.getSegmentsOfActivity(activity)
      .map { ids: Seq[BigInt] =>
        for {
          segment <- ids.distinct
          tmp = stravaWrapper.getActivitiesEffortCount(segment)
        } yield tmp.map(segment -> _)
      }
      .flatMap { elems: Seq[Future[(BigInt, BigInt)]] =>
          Future.collect(elems)
      }
      .map { ll: Seq[(BigInt, BigInt)] =>
            // max of tuple list by segment efforts _2 and return segmentId _1
            Try(ll.maxBy(_._2)._1).toOption
      }
      .within(DefaultTimer.twitter, Main.DEFAULT_TIMEOUT)
  }


}
