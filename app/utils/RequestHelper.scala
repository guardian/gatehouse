package utils

import play.api.http.HeaderNames.X_FORWARDED_FOR
import play.api.mvc.RequestHeader

object RequestHelper {

  def origin(request: RequestHeader): String = request.headers.get(X_FORWARDED_FOR).getOrElse(request.remoteAddress)
}
