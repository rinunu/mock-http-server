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

@RunWith(classOf[JUnitRunner])
class HttpServerTest extends FunSuite with MockitoSugar with BeforeAndAfterEach {

  def get(url: String) = {
    val httpget = new HttpGet(server.url + url)
    val response = httpclient.execute(httpget)
    val entity = response.getEntity
    val source = Source.fromInputStream(entity.getContent)
    source.getLines.mkString("")
  }

  def post(url: String, params: Map[String, String]) = {
    val httppost = new HttpPost(server.url + url)

    import scala.collection.JavaConverters._
    val params2 = params.map(x => new BasicNameValuePair(x._1, x._2)).toSeq.asJava
    httppost.setEntity(new UrlEncodedFormEntity(params2, HTTP.UTF_8));

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

  test("param の部分マッチ") {
    pending
  }

}