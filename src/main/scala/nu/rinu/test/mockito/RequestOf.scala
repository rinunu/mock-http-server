package nu.rinu.test.mockito

import org.mockito.Matchers.argThat
import org.mockito.ArgumentMatcher
import nu.rinu.test.Request

/**
 * mockito 用の Matcher
 *
 * @param header 指定したヘッダーがすべて、リクエストに含まれているならマッチする
 */
class RequestOf(url: String, params: Map[String, Seq[String]] = Map(), header: Map[String, Seq[String]] = Map()) extends ArgumentMatcher[Request] {
  def matches(a: Any) = {
    val request = a.asInstanceOf[Request]

    request != null &&
      request.url == url &&
      matchesParams(request) &&
      matchesHeader(request)
  }
  private def matchesParams(request: Request) =
    if (params.isEmpty) {
      true
    } else {
      params == request.params
    }

  private def matchesHeader(request: Request) =
    if (header.isEmpty) {
      true
    } else {
      header.forall(kv => request.header(kv._1) == kv._2)
    }

}

object RequestOf {
  def requestOf(url: String, params: Map[String, Seq[String]] = Map(), header: Map[String, Seq[String]] = Map()) = argThat(new RequestOf(url, params, header))
}
