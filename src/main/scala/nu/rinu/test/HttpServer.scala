package nu.rinu.test

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.{ Request => JRequest }
import org.apache.http.HttpRequest
import scala.collection.JavaConverters._

/**
 * モック用途を想定した HttpServer
 *
 * == 設計方針 ==
 *  - なるべくシンプルに
 *  - Scala との親和性を高く
 *  - mock ライブラリには依存しない
 *  - mockito のような verify ができる
 *    - mockito 等とうまく連携できるといい
 *  - ナマの HTTP も確認できる
 *  - テスト中は起動しっぱなしにできる
 *
 *  - TODO response を delay できるとうれしい？
 */
class HttpServer(port: Int, var handler: HttpServerHandler = null) {
  val impl = new Server(port)
  impl.setStopAtShutdown(true)

  def url = "http://localhost:" + port

  private implicit def toScala(from: java.util.Map[String, Array[String]]): Map[String, Seq[String]] = {
    // TODO これもっとシンプルにできないかなぁ
    from.asScala.toMap.map(a => (a._1, a._2.toSeq))
  }

  impl.setHandler(new AbstractHandler {
    override def handle(target: String, baseRequest: JRequest, request: HttpServletRequest, response: HttpServletResponse) {
      val (method, f) = request.getMethod match {
        case "GET" => (Method.Get, handler.get _)
        case "POST" => (Method.Post, handler.post _)
      }
      val req = new Request(method, request.getRequestURI, request.getParameterMap)
      val res = f(req)

      if (res != null) {
        response.getWriter.append(res.body)
        response.setStatus(res.statusCode)

        baseRequest.setHandled(true)
      }
    }
  })
  impl.start

  def stop() {
    impl.stop
    impl.join
  }
}

object Method extends Enumeration {
  val Get = Value("get")
  val Post = Value("post")
  val Put = Value("put")
  val Delete = Value("delete")
}

case class Response(statusCode: Int, body: String, header: Map[String, String] = Map()) {
}

/**
 * 設計
 * HttpServletRequest は verify のタイミングでアクセス出来なかったため、 immutable な独自のオブジェクトとする
 */
case class Request(method: Method.Value, url: String, params: Map[String, Seq[String]] = Map(), header: Map[String, String] = Map()) {
}

object Response {
  implicit def toResponse(body: String) = Response(200, body)
  implicit def toResponse(statusCode: Int) = Response(statusCode, "dummy")
}

/**
 */
trait HttpServerHandler {
  def get(request: Request): Response
  def post(request: Request): Response
}
