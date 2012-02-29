package nu.rinu.test

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.io.Source
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.HTTP
import org.junit.runner.RunWith
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.FunSuite
import nu.rinu.test.Response.toResponse
import nu.rinu.test.mockito.RequestOf.requestOf
import org.apache.http.HttpResponse

case class ClientException(code: Int) extends RuntimeException

/**
 * テスト対象
 */
class ShopClient(serverUrl: String, id: String, pass: String) {
  private val httpclient = new DefaultHttpClient
  var token = ""

  def login() {
    post("/login", Map("id" -> id, "pass" -> pass.reverse)) { res =>
      if (res.getStatusLine.getStatusCode == 503) {
        body(res)
        login()
      } else {
        token = body(res)
      }
    }
  }

  def buy() = {
    login()
    post("/buy") { res =>
      val a = res.getHeaders("Update-Auth")
      if (!a.isEmpty) {
        token = a.head.getValue
      }
      body(res)
    }
  }

  def body(res: HttpResponse) = {
    val entity = res.getEntity
    val source = Source.fromInputStream(entity.getContent)
    try {
      source.getLines.mkString("")
    } finally {
      source.close
    }
  }

  private def post[T](
    url: String,
    params: Map[String, String] = Map(),
    headers: Map[String, String] = Map())(f: HttpResponse => T): T = {

    val httppost = new HttpPost(serverUrl + url)

    val params2 = params.map(x => new BasicNameValuePair(x._1, x._2)).toSeq.asJava
    httppost.setEntity(new UrlEncodedFormEntity(params2, HTTP.UTF_8));
    headers.foreach(h => httppost.addHeader(h._1, h._2))
    val res = httpclient.execute(httppost)
    try {
      f(res)
    } finally {
      httppost.abort
    }
  }
}

@RunWith(classOf[JUnitRunner])
class ExampleTest extends FunSuite with MockitoSugar with BeforeAndAfterEach with BeforeAndAfterAll {
  var handler: HttpServerHandler = _
  var server = new HttpServer(7987)

  // テスト対象
  var shop: ShopClient = _

  override def afterAll() {
    server.stop()
  }

  override def beforeEach() {
    handler = mock[HttpServerHandler] // Mockito を使用して HttpServerHandler をモック化
    server.handler = handler
    shop = new ShopClient(server.url, "id0", "pass0")
  }

  test("login すると /login へ POST する") {
    when(handler.post(requestOf("/login"))).thenReturn(200)

    // テスト対象コード
    shop.login()

    // POST /login されたことの確認 
    verify(handler).post(requestOf("/login"))
  }

  test("login すると サーバから返信されたトークンを保存する") {
    when(handler.post(requestOf("/login"))).thenReturn("token0")

    // テスト対象コード
    shop.login()

    assert(shop.token === "token0")
  }

  test("login 時、 id/pass が暗号化されて POST される") {
    when(handler.post(requestOf("/login"))).thenReturn("token0")

    // テスト対象コード
    shop.login()

    // POST /login の引数が正しいことの確認
    // 暗号化はわかりやすくするため、文字の順番を逆順にするだけ
    verify(handler).post(requestOf("/login",
      params = Set(
        "id" -> "id0",
        "pass" -> "0ssap")))
  }

  test("login 時、503 の場合はリトライする") {
    // ステータスコード 503 -> 200 の順番に返す
    when(handler.post(requestOf("/login"))).thenReturn(503, "token0")

    // テスト対象コード
    shop.login()
    // 2回目に返却されるトークンをきちんと取得できている
    shop.token === "token0"

    // 2回 POST /login されたことの確認
    verify(handler, times(2)).post(requestOf("/login"))
  }

  test("buy すると、 /login, /buy の順に POST される") {
    when(handler.post(requestOf("/login"))).thenReturn(200)
    when(handler.post(requestOf("/buy"))).thenReturn(200)

    // テスト対象コード
    shop.buy()

    val inOrder = Mockito.inOrder(handler)
    inOrder.verify(handler).post(requestOf("/login"))
    inOrder.verify(handler).post(requestOf("/buy"))
  }

  test("buy 時、レスポンスヘッダに Update-Auth が入っている場合、トークンが更新される") {
    when(handler.post(requestOf("/login"))).thenReturn(200)
    when(handler.post(requestOf("/buy"))).thenReturn(
      Response(200, headers = Set("Update-Auth" -> "token1")))

    // テスト対象コード
    shop.buy()
    
    assert(shop.token === "token1")
  }
}