package ch.japanimpact.auth.api.constants

import play.api.libs.json.{Format, Json}

/**
  * @author Louis Vialar
  */
object GeneralErrorCodes {
  type ErrorCode = Int

  val UnknownError = 100

  /**
    * Some data in the request is missing or invalid
    */
  val MissingData = 101

  /**
    * The requested app was not found
    */
  val UnknownApp = 102

  /**
    * The requested app was not found or the App Secret was incorrect
    */
  val InvalidAppSecret = 103

  /**
    * The json returned by the server in case of error
    */
  case class RequestError(errorCode: Int)

  implicit val errorFormat: Format[RequestError] = Json.format[RequestError]
}
