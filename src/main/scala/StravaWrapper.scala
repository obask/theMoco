import java.net.URL
import java.nio.file.Paths

import com.twitter.finagle.{Service, builder, http}

/**
  * Created by oleg on 8/2/16.
  */
object StravaWrapper {

  private val OAUTH_TOKEN = sys.env.getOrElse("STRAVA_ACCESS_TOKEN",
    throw new Exception("STRAVA_ACCESS_TOKEN doesn't set in environment")
  )

  private val stravaHost = "www.strava.com"
  private val stravaPort = 443

  def failureAction(err: Throwable) = {
    println(s"strava api error: " + err.getMessage)
  }

  val client: Service[http.Request, http.Response] = builder.ClientBuilder()
    .codec(http.Http())
    .hosts(stravaHost + ":" + stravaPort.toString)
    .tls(stravaHost)
    .hostConnectionLimit(64)
    .build()

  def makeStravaRequest(resourceUrl: String, id: String) = http.RequestBuilder()
      .url(new URL("https", stravaHost, stravaPort, Paths.get(resourceUrl, id).toString))
      .setHeader("Authorization", "Bearer " + OAUTH_TOKEN)
      .buildGet()



  def activitiesRequest(activity: String) = {
    client(makeStravaRequest("/api/v3/activities/", activity))
      .onFailure { failureAction }
      .map { _.getContentString }
  }

  def segmentsRequest(segment: String) = {
    client(makeStravaRequest("/api/v3/segments/" , segment))
      .onFailure { failureAction }
      .map { _.getContentString }
  }




}
