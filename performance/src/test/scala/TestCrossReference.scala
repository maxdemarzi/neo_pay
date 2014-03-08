package simulations

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import akka.util.duration._
import bootstrap._
import util.parsing.json.JSONArray


class TestCrossReference extends Simulation {
  val httpConf = httpConfig
    .baseURL("http://localhost:7474")
    .acceptHeader("application/json")
    // Uncomment to see Requests
    //    .requestInfoExtractor(request => {
    //    println(request.getStringData)
    //    Nil
    //  })
    // Uncomment to see Response
    //    .responseInfoExtractor(response => {
    //    println(response.getResponseBody)
    //    Nil
    //  })
    .disableResponseChunksDiscarding

  val testfile = csv("test-data.txt").circular

  val base = scenario("Get Hello World")
    .during(30) {
    exec(
      http("Get Base Request")
        .get("/")
        .check(status.is(200))
    )
    // .pause(0 milliseconds, 1 milliseconds)
  }

  val scn = scenario("Cross Reference via Unmanaged Extension")
    .during(30) {
    feed(testfile)
    .exec(
      http("Post Cross Reference Request")
        .post("/example/service/crossreference")
        .body("""{"cc": "${cc}", "phone": "${phone}", "email": "${email}", "ip": "${ip}" }""")
        .check(status.is(200))
      )
      .pause(0 milliseconds, 1 milliseconds)
  }




  setUp(
    //base.users(16).protocolConfig(httpConf)
    scn.users(16).protocolConfig(httpConf)
  )
}
