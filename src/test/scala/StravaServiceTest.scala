import com.twitter.util._
import org.scalatest._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{Futures, ScalaFutures}

class StravaServiceTest extends FlatSpec with MockFactory with ScalaFutures {


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


  "StravaServiceImplTest" should "parseSegmentSeq" in {

    val tmp1 = StravaService.parseSegmentSeq(testJson1)
    val tmp2: Seq[BigInt] = Seq(6920366, 4644366, 2507082)

    assert(tmp1 == tmp2)
  }


  "StravaServiceImplTest" should "parseEffortCount" in {

    val tmp1 = StravaService.parseEffortCount(testJson2)
    val tmp2: BigInt = 100

    assert(tmp1 == tmp2)
  }


}
