import java.net.URL

import com.twitter.finagle._
import com.twitter.finagle.http
import com.twitter.finagle.http.path.{/, Root}
import com.twitter.util.{Await, Future}
import com.twitter.finagle.http.service.RoutingService
import org.json4s.JsonAST.{JField, JInt, JObject}
import org.json4s.jackson.JsonMethods

object Main extends App {

  private val stravaHost = "www.strava.com"
  private val stravaPort = 443

  private val servicePort = 8080

  private val OAUTH_TOKEN = sys.env.getOrElse("STRAVA_ACCESS_TOKEN",
    throw new Exception("STRAVA_ACCESS_TOKEN doesn't set in environment")
  )


  val client: Service[http.Request, http.Response] = builder.ClientBuilder()
      .codec(http.Http())
      .hosts(stravaHost + ":" + stravaPort.toString)
      .tls(stravaHost)
      .hostConnectionLimit(64)
      .build()

  def request1(activity: String) = http.RequestBuilder()
    .url(new URL("https", stravaHost, stravaPort, s"/api/v3/activities/" + activity))
    .setHeader("Authorization", "Bearer " + OAUTH_TOKEN)
    .buildGet()

  def request2(segment: String) = http.RequestBuilder()
    .url(new URL("https", stravaHost, stravaPort, s"/api/v3/segments/" + segment))
    .setHeader("Authorization", "Bearer " + OAUTH_TOKEN)
    .buildGet()

  def response1(activity: String): Future[Seq[BigInt]] = client(request1(activity))
    .onFailure { failureAction }
    .map {
      req: http.Response =>
        val content = JsonMethods.parse(req.contentString)
        val ids = content \ "segment_efforts" \\ "segment" \\ "id"
        val result: Seq[BigInt] = for {
          JObject(segments) <- ids
          JField("id", seg) <- segments
          JInt(id) <- seg
        } yield id
        result
    }


  def failureAction(err: Throwable) = {
    println(s"strava api error: " + err.getMessage)
  }

//  FIXME remove option here
  def response2(id: String): Future[Option[BigInt]] = client(request2(id))
    .onFailure { failureAction }
    .map {
      req: http.Response =>
        val json = JsonMethods.parse(req.contentString)
        val count = json \ "effort_count" match {case JInt(x) => Some(x); case _ => None}
        println("GET success: " + count.toString)
        count
    }

// TODO check empty list
  def getBusiness(activity: String): Future[BigInt] = {
    response1(activity) flatMap {
      ids: Seq[BigInt] =>
        val deDuplicated = ids.toSet
        val tmp = for (segment <- deDuplicated)
          yield response2(segment.toString) map {x => segment -> x.get }
        val xx: Future[BigInt] = Future.collect(tmp.toSeq).map {
          ll: Seq[(BigInt, BigInt)] =>
            val tmp: (BigInt, BigInt) = ll.maxBy(_._2)
            tmp._1
        }
        xx
    }
  }

  def userService(message: String) = new Service[http.Request, http.Response] {
    def apply(req: http.Request): Future[http.Response] = {
      // TODO check empty list
      getBusiness(message).map { result =>
        val rep = http.Response(http.Version.Http11, http.Status.Ok)
        rep.setContentString(result.toString)
        rep
      }
    }
  }

  val blackHole = new Service[http.Request, http.Response] {
    def apply(request: http.Request) = {
      Future(http.Response(request.version, http.Status.NotFound))
    }
  }

  val router = RoutingService.byPathObject[http.Request] {
    case Root / "user" / message => userService(message)
    case _ => blackHole
  }

  val server = Http.serve(":" + servicePort.toString, router)
  Await.ready(server)

}
