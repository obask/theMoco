import java.util.concurrent.TimeUnit

import com.twitter.util.Future
import org.json4s.JsonAST.{JField, JInt, JObject}
import org.json4s.jackson.JsonMethods

import scala.util.{Failure, Try}
import com.twitter.util.Duration
import com.twitter.finagle.util.DefaultTimer

class MicroController(stravaWrapper: StravaWrapper) {

  val DEFAULT_TIMEOUT = Duration(7, TimeUnit.SECONDS)

  def parseSegmentSeq(content: String): Seq[BigInt] = {
    val json = JsonMethods.parse(content)
    val ids = json \ "segment_efforts" \\ "segment" \\ "id"
    val result: Seq[BigInt] = for {
      JObject(segments) <- ids
      JField("id", seg) <- segments
      JInt(id) <- seg
    } yield id
    result
  }

  def parseEffortCount(content: String): Option[BigInt] = {
    val json = JsonMethods.parse(content)
    //    FIXME future conversion
    json \ "effort_count" match {case JInt(x) => Some(x); case _ => None}
  }

  def findMostPopularRoute(activity: Long): Future[Option[BigInt]] = {
    stravaWrapper.activitiesRequest(activity)
      .map(parseSegmentSeq)
      .flatMap { ids: Seq[BigInt] =>
        val deDuplicated = ids.toSet
        val elems: Set[Future[(BigInt, BigInt)]] = for {
          segment <- deDuplicated
          contOpt = stravaWrapper.segmentsRequest(segment).map(parseEffortCount)
          tmp: Future[BigInt] = contOpt.flatMap {
            case Some(x) => Future(x)
            case None => throw new Exception("client returns bad json")
          }
        } yield tmp.map(segment -> _)

        val xx: Future[Option[BigInt]] = Future.collect(elems.toSeq).map {
          ll: Seq[(BigInt, BigInt)] =>
            // max by segment efforts _2 and return id _1
            Try(ll.maxBy(_._2)._1).toOption
        }
        xx
      }
      .within(DefaultTimer.twitter, DEFAULT_TIMEOUT)
  }


}
