package nu.rinu.test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.mockito.Matchers._
import org.mockito.Matchers.{ eq => _eq }
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import scala.io.Source
import org.scalatest.BeforeAndAfter
import org.scalatest.BeforeAndAfterEach
import org.apache.http.client.HttpClient
import org.mockito.ArgumentMatcher
import javax.servlet.http.HttpServletRequest
import org.hamcrest.Description
import org.apache.http.client.methods.HttpPost
import org.apache.http.params.HttpParams
import org.apache.http.params.BasicHttpParams
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.protocol.HTTP
import nu.rinu.test.mockito.RequestOf._
import scala.collection.JavaConverters._
import org.apache.http.Header

@RunWith(classOf[JUnitRunner])
class HttpServerTest extends FunSuite with MockitoSugar with BeforeAndAfterEach {

  def get(url: String) = {
    val httpget = new HttpGet(server.url + url)
    val response = httpclient.execute(httpget)
    val entity = response.getEntity
    val source = Source.fromInputStream(entity.getContent)
    source.getLines.mkString("")
  }

  def post(url: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map()) = {
    val httppost = new HttpPost(server.url + url)

    val params2 = params.map(x => new BasicNameValuePair(x._1, x._2)).toSeq.asJava
    httppost.setEntity(new UrlEncodedFormEntity(params2, HTTP.UTF_8));
    headers.foreach(h => httppost.addHeader(h._1, h._2))

    val response = httpclient.execute(httppost)
    val entity = response.getEntity
    val source = Source.fromInputStream(entity.getContent)
    source.getLines.mkString("")
  }

  var serverHandler: HttpServerHandler = null
  var server: HttpServer = null
  var httpclient: HttpClient = null

  override protected def beforeEach() {
    serverHandler = mock[HttpServerHandler]
    server = new HttpServer(7987)
    server.handler = serverHandler

    httpclient = new DefaultHttpClient
  }

  override protected def afterEach() {
    server.stop()
  }

  def fixture = new {
  }

  test("get を stub/verify できる") {
    when(serverHandler.get(requestOf("/test/test2"))).thenReturn("testResult")

    // client code
    assert(get("/test/test2") === "testResult")

    verify(serverHandler).get(requestOf("/test/test2"))
  }

  test("パラメータを stub/verify できる") {
    when(serverHandler.get(requestOf("/test", params = Map("k1" -> Seq("v1"))))).thenReturn("result1")
    when(serverHandler.get(requestOf("/test", params = Map("k1" -> Seq("v2"))))).thenReturn("result2")

    // client code
    assert(get("/test?k1=v1") === "result1")
    assert(get("/test?k1=v2") === "result2")

    verify(serverHandler).get(requestOf("/test", params = Map("k1" -> Seq("v1"))))
    verify(serverHandler).get(requestOf("/test", params = Map("k1" -> Seq("v2"))))
  }

  test("post を stub/verify できる") {
    when(serverHandler.post(requestOf("/test"))).thenReturn("result1")

    // client code
    assert(post("/test", Map()) === "result1")

    verify(serverHandler).post(requestOf("/test"))
  }

  test("post のパラメータを stub/verify できる") {
    when(serverHandler.post(requestOf("/test", Map("k1" -> Seq("v1"))))).thenReturn("result1")
    when(serverHandler.post(requestOf("/test", Map("k1" -> Seq("v2"))))).thenReturn("result2")

    // client code
    assert(post("/test", Map("k1" -> "v1")) === "result1")
    assert(post("/test", Map("k1" -> "v2")) === "result2")
  }

  test("複数回のリクエストを stub/verify できる") {
    when(serverHandler.get(requestOf("/test1"))).thenReturn("result1")
    when(serverHandler.get(requestOf("/test2"))).thenReturn("result2")

    assert(get("/test1") === "result1")
    verify(serverHandler).get(requestOf("/test1"))

    assert(get("/test2") === "result2")
    verify(serverHandler).get(requestOf("/test2"))
  }

  test("status code を返せる") {
    when(serverHandler.get(requestOf("/test/test2"))).thenReturn(500)

    // client code
    val httpget = new HttpGet(server.url + "/test/test2")
    val response = httpclient.execute(httpget)
    assert(response.getStatusLine.getStatusCode === 500)

    verify(serverHandler).get(requestOf("/test/test2"))
  }

  test("request header を stub/verify できる") {
    when(serverHandler.post(requestOf("/test", headers = Map("H1" -> Seq("V2"))))).thenReturn("result2")
    when(serverHandler.post(requestOf("/test", headers = Map("H1" -> Seq("V1"))))).thenReturn("result1")

    // client code
    assert(post("/test", headers = Map("H1" -> "V1")) === "result1")
    assert(post("/test", headers = Map("H1" -> "V2")) === "result2")
    assert(post("/test", headers = Map("H1" -> "V2")) === "result2")
  }

  def toMap(headers: Array[Header]) =
    headers.map(a => (a.getName, a.getValue)).toMap

  test("response header を stub できる") {
    when(serverHandler.post(requestOf("/test"))).thenReturn(Response(body = "result2", headers = Map("H1" -> Seq("V1"), "H2" -> Seq("V2"))))

    // client code
    val httppost = new HttpPost(server.url + "/test")

    val response = httpclient.execute(httppost)
    val entity = response.getEntity
    val headers = toMap(response.getAllHeaders)

    assert(headers.exists(_ == ("H1", "V1")), "H1 を含む")
    assert(headers.exists(_ == ("H2", "V2")), "H2 を含む")
    assert(!headers.exists(_ == ("H3", "V3")), "H3 を含まない")
  }

  test("pending header => headers") {
    pending
  }

  test("param の部分マッチ") {
    pending
  }

}