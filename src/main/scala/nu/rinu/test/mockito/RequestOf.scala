package nu.rinu.test.mockito

import org.mockito.Matchers.argThat
import org.mockito.ArgumentMatcher
import nu.rinu.test.Request

/**
 * mockito 用の Matcher
 *
 * @param headers 指定したヘッダーがすべて、リクエストに含まれているならマッチする
 */
class RequestOf(url: String, params: Map[String, Seq[String]] = Map(), headers: Map[String, Seq[String]] = Map()) extends ArgumentMatcher[Request] {
  def matches(a: Any) = {
    val request = a.asInstanceOf[Request]

    request != null &&
      request.url == url &&
      matchesParams(request) &&
      matchesHeaders(request)
  }
  private def matchesParams(request: Request) =
    if (params.isEmpty) {
      true
    } else {
      params == request.params
    }

  private def matchesHeaders(request: Request) =
    if (headers.isEmpty) {
      true
    } else {
      headers.forall(kv => request.headers(kv._1) == kv._2)
    }

}

object RequestOf {
  def requestOf(url: String, params: Map[String, Seq[String]] = Map(), headers: Map[String, Seq[String]] = Map()) = argThat(new RequestOf(url, params, headers))
}
