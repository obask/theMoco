import com.twitter.finagle.http
import com.twitter.util.Future

import scala.util.{Failure, Try}
import com.twitter.finagle.util.DefaultTimer

/**
  * A controller with single handler proceedUserRequest
  * returns most contested segment for any user activity.
  * If user has no access to activity segments returns 404 NotFound.
  */


class MostContestedController(stravaWrapper: StravaService) {


  def proceedUserRequest(activity: Long): Future[http.Response] = {
    findMostPopularRoute(activity)
      .within(DefaultTimer.twitter, Main.DEFAULT_TIMEOUT)
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
          tmp = stravaWrapper.getSegmentEffortCount(segment)
        } yield tmp.map(segment -> _)
      }
      .flatMap { elems: Seq[Future[(BigInt, BigInt)]] =>
          Future.collect(elems)
      }
      .map { ll: Seq[(BigInt, BigInt)] =>
            // max of tuple list by segment efforts _2 and return segmentId _1
            Try(ll.maxBy(_._2)._1).toOption
      }
  }


}
