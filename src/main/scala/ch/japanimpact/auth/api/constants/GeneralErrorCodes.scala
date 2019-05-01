package ch.japanimpact.auth.api.constants

import play.api.libs.json.{Format, Json}

/**
  * @author Louis Vialar
  */
object GeneralErrorCodes {
  private val GeneralErrorCodeSelector = "*"

  /**
    * The json returned by the server in case of error
    */
  class RequestError(val errorCode: Int)

  private object RequestError {
    def apply(errorCode: Int): RequestError = new RequestError(errorCode)

    def unapply(arg: RequestError): Option[Int] = Some(arg.errorCode)
  }

  class ErrorCode(errorCode: Int, val endpoint: String) extends RequestError(errorCode)

  class GeneralErrorCode(code: Int) extends ErrorCode(code, GeneralErrorCodeSelector)

  object ErrorCode {
    private val errors = List(UnknownError, MissingData, UnknownApp, InvalidAppSecret, InvalidCaptcha)

    private val register: Map[String, Map[Int, ErrorCode]] = errors.groupBy(_.endpoint).mapValues(_.map(e => (e.errorCode, e)).toMap)

    def apply(code: Int, endpoint: String): ErrorCode = {
      val map = register(endpoint) ++ register(GeneralErrorCodeSelector)

      map.withDefaultValue(UnknownError)(code)
    }
  }

  case object UnknownError extends GeneralErrorCode(100)

  /**
    * Some data in the request is missing or invalid
    */
  case object MissingData extends GeneralErrorCode(101)

  /**
    * The requested app was not found
    */
  case object UnknownApp extends GeneralErrorCode(102)

  /**
    * The requested app was not found or the App Secret was incorrect
    */
  case object InvalidAppSecret extends GeneralErrorCode(103)

  /**
    * The captcha is invalid
    */
  case object InvalidCaptcha extends GeneralErrorCode(104)

  case object InvalidTicket extends ErrorCode(201, "get_ticket")

  implicit val errorFormat: Format[RequestError] = Json.format[RequestError]
}
