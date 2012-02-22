package nu.rinu.test

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.{ Request => JRequest }
import scala.collection.JavaConverters._
import org.slf4j.LoggerFactory

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
 */
class HttpServer(port: Int, var handler: HttpServerHandler = null) {
  val logger = LoggerFactory.getLogger(classOf[HttpServer])

  val impl = new Server(port)
  impl.setStopAtShutdown(true)

  def url = "http://localhost:" + port

  logger.debug("start: " + url)

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

      logger.debug("request: " + request.getRequestURI)
      val req = new Request(method, request.getRequestURI, request.getParameterMap, headers = headers(request))
      val res = f(req)

      if (res != null) {
        logger.debug("response: %d".format(res.statusCode))

        response.getWriter.append(res.body)
        response.setStatus(res.statusCode)
        for {
          header <- res.headers
          value <- header._2
        } {
          response.addHeader(header._1, value)
        }

        baseRequest.setHandled(true)
      } else {
        logger.debug("response: 404")
      }
    }
  })
  impl.start

  def stop() {
    impl.stop
    impl.join
  }

  private def headers(request: HttpServletRequest): Map[String, Seq[String]] = {
    val tuples = for (name <- request.getHeaderNames.asScala) yield {
      (name,
        for (value <- request.getHeaders(name).asScala.toSeq) yield value)
    }
    tuples.toMap
  }
}

object Method extends Enumeration {
  val Get = Value("get")
  val Post = Value("post")
  val Put = Value("put")
  val Delete = Value("delete")
}

/**
 * 設計
 * HttpServletRequest は verify のタイミングでアクセス出来なかったため、 immutable な独自のオブジェクトとする
 */
case class Request(method: Method.Value, url: String, params: Map[String, Seq[String]] = Map(), headers: Map[String, Seq[String]] = Map()) {
}

case class Response(statusCode: Int = 200, body: String = "", headers: Map[String, Seq[String]] = Map()) {
}

object Response {
  implicit def toResponse(body: String) = Response(200, body)
  implicit def toResponse(statusCode: Int) = Response(statusCode, "dummy")
}

/**
 * HTTP リクエストを実際に処理する
 *
 * モックにすることを想定している
 */
trait HttpServerHandler {
  def get(request: Request): Response
  def post(request: Request): Response
}
