package nu.rinu.test.mockito

import org.mockito.Matchers.argThat
import org.mockito.ArgumentMatcher
import nu.rinu.test.Request

/**
 * mockito 用の Matcher
 *
 * @param headers 指定したヘッダーがすべて、リクエストに含まれているならマッチする
 */
class RequestOf(url: String, params: Set[(String, String)], headers: Set[(String, String)]) extends ArgumentMatcher[Request] {

  def matches(a: Any) = {
    val request = a.asInstanceOf[Request]

    request != null &&
      request.url == url &&
      matchesParams(request) &&
      matchesHeaders(request)
  }
  private def matchesParams(request: Request) = matches(params, request.params)

  private def matchesHeaders(request: Request) = matches(headers, request.headers)

  private def matches(exptected: Set[(String, String)], actual: Map[String, Seq[String]]) = {
    if (exptected.isEmpty) {
      true
    } else {
      exptected.forall(kv =>
        actual.getOrElse(kv._1, Seq()).contains(kv._2))
    }
  }
}

object RequestOf {
  def requestOf(url: String, params: Set[(String, String)] = Set(), headers: Set[(String, String)] = Set()) =
    argThat(new RequestOf(url, params, headers))
}
