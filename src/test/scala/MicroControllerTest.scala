import com.twitter.util._
import org.scalatest._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{Futures, ScalaFutures}

import scala.concurrent.Promise

class MicroControllerTest extends FlatSpec with MockFactory with ScalaFutures {


  val testJson1 = """
      {
      	"id": 625234022,
      	"segment_efforts": [{
      		"segment": {
      			"id": 6920366
      		}
      	}, {
      		"segment": {
      			"id": 4644366
      		}
      	}, {
      		"segment": {
      			"id": 2507082
      		}
      	}]
      }
                  """

  val testJson2 = """
      {
      	"id": 6920366,
      	"effort_count": 100
      }
                  """

  val testJson3 = """
      {
      	"id": 4644366,
      	"effort_count": 2
      }
                  """

  val testJson4 = """
      {
      	"id": 2507082,
      	"effort_count": 3
      }
                  """
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

  class MockableClass extends StravaWrapper("test")
  val stravaMock = stub[MockableClass]

  "MicroController" should "find the max element" in {

    stravaMock.activitiesRequest _ when activityId returns Future(testJson1)
    stravaMock.segmentsRequest _ when (6920366: BigInt) returns Future(testJson2)
    stravaMock.segmentsRequest _ when (4644366: BigInt) returns Future(testJson3)
    stravaMock.segmentsRequest _ when (2507082: BigInt) returns Future(testJson4)

    val controller: MicroController = new MicroController(stravaMock)
    val tmp = controller.findMostPopularRoute(activityId)
    assert(twitterToScalaFuture(tmp).futureValue.contains(6920366: BigInt))

  }

  "MicroController" should "handle client error" in {

    stravaMock.activitiesRequest _ when activityId returns Future(testJson1)
    stravaMock.segmentsRequest _ when (6920366: BigInt) returns Future("{\"effort_count\": \"dsad\"}")
    stravaMock.segmentsRequest _ when (4644366: BigInt) returns Future(testJson3)
    stravaMock.segmentsRequest _ when (2507082: BigInt) returns Future(testJson4)

    val controller: MicroController = new MicroController(stravaMock)
    val caught = intercept[Exception] {
      twitterToScalaFuture(controller.findMostPopularRoute(activityId)).futureValue
    }
    assert(!caught.getMessage.isEmpty)
  }

  "MicroController" should "find the max element of empty list" in {

    stravaMock.activitiesRequest _ when activityId returns Future("{\"segment_efforts\": []}")

    val controller = new MicroController(stravaMock)
    val res = twitterToScalaFuture(controller.findMostPopularRoute(activityId))
    assert(res.futureValue.isEmpty)

  }

  "MicroController" should "handle timeout error" in {

    stravaMock.activitiesRequest _ when activityId returns Future.never

    val caught = intercept[TimeoutException] {
      Await.result(new MicroController(stravaMock).findMostPopularRoute(activityId))
    }
    assert(!caught.getMessage.isEmpty)
  }

}
