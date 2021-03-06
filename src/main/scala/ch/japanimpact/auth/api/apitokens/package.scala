package ch.japanimpact.auth.api

import play.api.libs.json.{Format, JsResult, JsSuccess, JsValue, Json, Reads}

package object apitokens {

  abstract class Principal(id: Int, val name: String) {
    def toSubject = s"$name|$id"
  }

  case class User(id: Int) extends Principal(id, "user")

  case class App(id: Int) extends Principal(id, "app")

  object Principal {
    val fromName: String => (Int => Principal) = Map(
      "user" -> User.apply,
      "app" -> App.apply
    )
  }

  case class AuthentifiedPrincipal(principal: Principal, scopes: Set[String]) {
    def hasScope(scope: String) = scopes.hasScope(scope)
  }

  implicit class ScopeSet(scopes: Set[String]) {
    def hasScope(scope: String) = {
      if (scopes(scope)) true
      else {
        // Check for * sub scopes

        // example: for scope "uploads/staffs/add"
        scope.split("/") // split all parts: [uploads, staffs, add]
          .map(part => part + "/") // add a slash at the end: [uploads/, staffs/, add/]
          .scanLeft("")(_ + _) // cumulative apply: [, uploads/, uploads/staffs/, uploads/staffs/add/]
          .dropRight(1) // we don't care about the full path
          .map(path => path + "*") // [*, uploads/*, uploads/staffs/*]
          .exists(scopes) // find if one of these scopes is allowed
      }
    }
  }

  sealed trait TokenRequest

  /**
    * A request to obtain a token for an user
    *
    * @param ticket    the CAS service ticket to trade for a token
    * @param scopes    the scopes in which the token should be valid
    * @param audiences the target services that can accept this token
    * @param duration  the maximal duration for this token, in seconds
    */
  case class UserTokenRequest(ticket: String, scopes: Set[String], audiences: Set[String], duration: Long) extends TokenRequest

  /**
    * A request to obtain a token for your app
    *
    * @param scopes    the scopes in which the token should be valid
    * @param audiences the target services that can accept this token
    * @param duration  the maximal duration for this token, in seconds
    */
  case class AppTokenRequest(scopes: Set[String], audiences: Set[String], duration: Long) extends TokenRequest

  implicit val userTokenRequestMapper: Format[UserTokenRequest] = Json.format[UserTokenRequest]
  implicit val appTokenRequestMapper: Format[AppTokenRequest] = Json.format[AppTokenRequest]


  /**
    * A successful response to a token request
    *
    * @param token     the returned API token
    * @param scopes    the scopes in which this token will be valid
    *                  it may be different from the scopes in the request, if some of them were not allowed for the user/app
    * @param audiences the audiences in which the token will be valid
    * @param duration  the time during which this token will be valid, in seconds
    */
  case class TokenResponse(token: String, scopes: Set[String], audiences: Set[String], duration: Long)

  /**
    * An unsuccessful response to a token request
    *
    * @param error   the name of the error
    * @param message the message associated with the error
    */
  case class ErrorResponse(error: String, message: String)

  implicit val tokenResponseMapper: Format[TokenResponse] = Json.format[TokenResponse]
  implicit val errorResponseMapper: Format[ErrorResponse] = Json.format[ErrorResponse]

  implicit val tokenParser = new Reads[Either[ErrorResponse, TokenResponse]] {
    override def reads(json: JsValue): JsResult[Either[ErrorResponse, TokenResponse]] = {
      tokenResponseMapper.reads(json) match {
        case s: JsSuccess[TokenResponse] => s.map(Right.apply)
        case _ => errorResponseMapper.reads(json).map(Left.apply)
      }
    }
  }
}
