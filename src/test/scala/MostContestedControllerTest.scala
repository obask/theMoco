import com.twitter.finagle.http
import com.twitter.util._
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Promise

class MostContestedControllerTest extends FlatSpec with MockFactory with ScalaFutures {


  val activityId: Long = 625234022

  // workaround for twitter features
  def twitterToScalaFuture[T](twitterFuture: com.twitter.util.Future[T]): scala.concurrent.Future[T] = {
    val promise = Promise[T]()
    twitterFuture respond {
      case Return(a) => promise success a
      case Throw(e)  => promise failure e
    }
    promise.future
  }


  class StravaServiceHelper extends StravaService("test")

  val stravaMock = stub[StravaServiceHelper]

  "MostContestedController" should "find the max element" in {

    stravaMock.getSegmentsOfActivity _ when activityId returns Future(Seq(6920366, 4644366, 2507082): Seq[BigInt])
    stravaMock.getActivitiesEffortCount _ when (6920366: BigInt) returns Future(100: BigInt)
    stravaMock.getActivitiesEffortCount _ when (4644366: BigInt) returns Future(2: BigInt)
    stravaMock.getActivitiesEffortCount _ when (2507082: BigInt) returns Future(3: BigInt)

    val controller: MostContestedController = new MostContestedController(stravaMock)
    val tmp = controller.proceedUserRequest(activityId)
    assert(twitterToScalaFuture(tmp).futureValue.contentString == "6920366")

  }

  "MostContestedController" should "handle client error" in {

    stravaMock.getSegmentsOfActivity _ when activityId returns Future(Seq(6920366, 4644366, 2507082): Seq[BigInt])
    stravaMock.getActivitiesEffortCount _ when (6920366: BigInt) returns Future.exception(new IllegalStateException("qwerty"))
    stravaMock.getActivitiesEffortCount _ when (4644366: BigInt) returns Future(2: BigInt)
    stravaMock.getActivitiesEffortCount _ when (2507082: BigInt) returns Future(3: BigInt)

    val controller: MostContestedController = new MostContestedController(stravaMock)
    val caught = twitterToScalaFuture(controller.proceedUserRequest(activityId)).futureValue
    assert(caught.contentString contains "IllegalStateException")
    assert(caught.contentString contains "qwerty")
  }

  "MostContestedController" should "find the max element of empty list" in {

    stravaMock.getSegmentsOfActivity _ when activityId returns Future(Seq(): Seq[BigInt])

    val controller = new MostContestedController(stravaMock)
    val res = twitterToScalaFuture(controller.proceedUserRequest(activityId))
    assert(res.futureValue.status == http.Status.NotFound)

  }

  "MostContestedController" should "handle timeout error" in {

    stravaMock.getSegmentsOfActivity _ when activityId returns Future.never

    val caught = Await.result(new MostContestedController(stravaMock).proceedUserRequest(activityId))
    assert(caught.contentString contains "TimeoutException")
    assert(caught.contentString contains "7")
  }

}
