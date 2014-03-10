package simulations

import com.excilys.ebi.gatling.core.Predef._
import com.giltgroupe.util.gatling.websocket.Predef._
import akka.util.duration._
import bootstrap._

class TestCrossReferenceWebSocket extends Simulation {

  val testfile = csv("test-data.txt").circular

  val scn = scenario("Cross Reference via WebSocket")
    .exec(websocket("socket").open("ws://localhost:7474/websocket", "socket_open"))
    .during(30) {
      feed(testfile)
      .exec(websocket("socket")
        .sendMessage("""{"cc": "${cc}", "phone": "${phone}", "email": "${email}", "ip": "${ip}" }""",
          "socket_send"))
        .pause(0 milliseconds, 1 milliseconds)
    }
    .exec(websocket("socket").close("socket_close")
  )

  setUp(scn.users(16))
}
