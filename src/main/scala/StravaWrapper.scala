import java.net.URL
import java.nio.file.Paths


import com.twitter.finagle.{Service, builder, http}

class StravaWrapper(oauthToken: String) {

  private val stravaHost = "www.strava.com"
  private val stravaPort = 443

  def failureAction(err: Throwable) = {
    println("strava api error: " + err.getMessage)
  }

  val client: Service[http.Request, http.Response] = builder.ClientBuilder()
    .codec(http.Http())
    .hosts(stravaHost + ":" + stravaPort.toString)
    .tls(stravaHost)
    .hostConnectionLimit(64)
    .build()

  def makeStravaRequest(resourceUrl: String, id: String) = http.RequestBuilder()
      .url(new URL("https", stravaHost, stravaPort, Paths.get(resourceUrl, id).toString))
      .setHeader("Authorization", "Bearer " + oauthToken)
      .buildGet()

  def activitiesRequest(activity: Long) = {
    client(makeStravaRequest("/api/v3/activities/", activity.toString))
      .onFailure { failureAction }
      .map { _.getContentString }
  }

  def segmentsRequest(segment: BigInt) = {
    client(makeStravaRequest("/api/v3/segments/" , segment.toString))
      .onFailure { failureAction }
      .map { _.getContentString }
  }


}
