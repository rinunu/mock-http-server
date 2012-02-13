package nu.rinu.test.mockito

import org.mockito.Matchers.argThat
import org.mockito.ArgumentMatcher
import nu.rinu.test.Request

/**
 * mockito 用の Matcher
 */
class RequestOf(url: String, params: Map[String, Seq[String]] = Map()) extends ArgumentMatcher[Request] {
  def matches(a: Any) = {
    val request = a.asInstanceOf[Request]
    if (request != null)
      if (request.url == url)
        if (params.isEmpty) true
        else {
          params == request.params
        }
      else false
    else false
  }
}

object RequestOf {
  def requestOf(url: String) = argThat(new RequestOf(url))

  def requestOf(url: String, params: Map[String, Seq[String]]) = argThat(new RequestOf(url, params))
}