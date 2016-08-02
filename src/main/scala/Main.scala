import java.net.URL

import com.twitter.finagle._
import com.twitter.finagle.http
import com.twitter.finagle.http.path.{/, Root}
import com.twitter.util.{Await, Future}
import com.twitter.finagle.http.service.RoutingService
import org.json4s.JsonAST.{JField, JInt, JObject}
import org.json4s.jackson.JsonMethods

object Main extends App {


  private val servicePort = 8080


  def response1(content: String): Seq[BigInt] = {
        val content = JsonMethods.parse(content)
        val ids = content \ "segment_efforts" \\ "segment" \\ "id"
        val result: Seq[BigInt] = for {
          JObject(segments) <- ids
          JField("id", seg) <- segments
          JInt(id) <- seg
        } yield id
        result
    }

  def response2(content: String): Option[BigInt] = {
      val json = JsonMethods.parse(content)
      val count = json \ "effort_count" match {case JInt(x) => Some(x); case _ => None}
      println("GET success: " + count.toString)
      count
  }

// TODO check empty list
  def getBusiness(activity: String): Future[BigInt] = {
    StravaWrapper.activitiesRequest(activity)
      .map(response1)
      .flatMap { ids: Seq[BigInt] =>
          val deDuplicated = ids.toSet
          val elems: Set[Future[(BigInt, BigInt)]] = for {
            segment <- deDuplicated
            contOpt = StravaWrapper.segmentsRequest(segment.toString).map(response2)
            tmp = contOpt.flatMap {
              case Some(x) => Future(x)
              case None => Future.never
            }
          } yield tmp.map(segment -> _)
        
          val xx: Future[BigInt] = Future.collect(elems.toSeq).map {
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
