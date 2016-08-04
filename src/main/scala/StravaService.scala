import java.net.URL
import java.nio.file.Paths

import com.twitter.finagle.{Address, Service, builder, http}
import com.twitter.util.Future
import org.json4s.JsonAST.{JField, JInt, JObject}
import org.json4s.jackson.JsonMethods

/**
  * A simple wrapper for Strava API.
  * It allows clients to load segments list for each activity
  * and get effort_count parameter for any segment.
  */

class StravaService(oauthToken: String) {

  def getSegmentsOfActivity(activity: Long): Future[Seq[BigInt]] = {
    activitiesRequest(activity)
      .map(StravaService.parseSegmentSeq)
  }

  def getSegmentEffortCount(segment: BigInt): Future[BigInt] = {
    segmentsRequest(segment)
      .map(StravaService.parseEffortCount)
  }

  private val stravaHost = "www.strava.com"
  private val stravaPort = 443

  private val client: Service[http.Request, http.Response] = builder.ClientBuilder()
    .codec(http.Http())
    .addrs(Address(stravaHost, stravaPort))
    .tls(stravaHost)
    .hostConnectionLimit(64)
    .build()

  private def makeStravaRequest(resourceUrl: String, id: String): http.Request = http.RequestBuilder()
      .url(new URL("https", stravaHost, stravaPort, Paths.get(resourceUrl, id).toString))
      .setHeader("Authorization", "Bearer " + oauthToken)
      .buildGet()

  private def activitiesRequest(activity: Long): Future[String] = {
    client(makeStravaRequest("/api/v3/activities/", activity.toString))
      .map { _.getContentString }
  }

  private def segmentsRequest(segment: BigInt): Future[String] = {
    client(makeStravaRequest("/api/v3/segments/" , segment.toString))
      .map { _.getContentString }
  }


}

object StravaService {

  def parseSegmentSeq(content: String): Seq[BigInt] = {
    val json = JsonMethods.parse(content)
    val ids = json \ "segment_efforts" \\ "segment" \\ "id"
    for {
      JObject(segments) <- ids
      JField("id", seg) <- segments
      JInt(id) <- seg
    }
      yield id
  }

  def parseEffortCount(content: String): BigInt = {
    val json = JsonMethods.parse(content)
    json \ "effort_count" match {
      case JInt(x) => x
      case _ => throw new Exception("parseEffortCount error: effort_count not found")
    }
  }

}
