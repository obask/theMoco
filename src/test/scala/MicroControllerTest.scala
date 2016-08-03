import com.twitter.util.{Await, Future, TimeoutException}
import org.scalatest.FlatSpec
import org.scalamock.scalatest.MockFactory
import com.twitter.finagle.http


class MicroControllerTest extends FlatSpec with MockFactory {


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

  class MockableClass extends StravaWrapper("test")
  val stravaMock = stub[MockableClass]

  "MicroController" should "find the max element" in {

    stravaMock.activitiesRequest _ when activityId returns Future(testJson1)
    stravaMock.segmentsRequest _ when (6920366: BigInt) returns Future(testJson2)
    stravaMock.segmentsRequest _ when (4644366: BigInt) returns Future(testJson3)
    stravaMock.segmentsRequest _ when (2507082: BigInt) returns Future(testJson4)

    val controller: MicroController = new MicroController(stravaMock)
    val res = Await.result(controller.findMostPopularRoute(activityId))
    assert(res.contains(6920366: BigInt))
  }

  "MicroController" should "handle client error" in {

    stravaMock.activitiesRequest _ when activityId returns Future(testJson1)
    stravaMock.segmentsRequest _ when (6920366: BigInt) returns Future("{\"effort_count\": \"dsad\"}")
    stravaMock.segmentsRequest _ when (4644366: BigInt) returns Future(testJson3)
    stravaMock.segmentsRequest _ when (2507082: BigInt) returns Future(testJson4)

    val controller: MicroController = new MicroController(stravaMock)
    val caught = intercept[Exception] {
      Await.result(controller.findMostPopularRoute(activityId))
    }
    assert(!caught.getMessage.isEmpty)
  }

  "MicroController" should "find the max element of empty list" in {

    stravaMock.activitiesRequest _ when activityId returns Future("{\"segment_efforts\": []}")

    val controller = new MicroController(stravaMock)
    val res = Await.result(controller.findMostPopularRoute(activityId))
    assert(res.isEmpty)
  }

  "MicroController" should "handle timout error" in {

    stravaMock.activitiesRequest _ when activityId returns Future.never

    val caught = intercept[TimeoutException] {
      Await.result(new MicroController(stravaMock).findMostPopularRoute(activityId))
    }
    assert(!caught.getMessage.isEmpty)
  }

}
