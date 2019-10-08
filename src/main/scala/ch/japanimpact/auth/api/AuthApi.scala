package ch.japanimpact.auth.api

import ch.japanimpact.auth.api.constants.GeneralErrorCodes
import ch.japanimpact.auth.api.constants.GeneralErrorCodes.{ErrorCode, RequestError}
import play.api.Configuration
import play.api.libs.json.{Json, Reads}
import play.api.libs.ws._

import scala.concurrent.{ExecutionContext, Future}


/**
  * @author Louis Vialar
  */
class AuthApi(val ws: WSClient, val apiBase: String, val apiClientId: String, val apiClientSecret: String)(implicit ec: ExecutionContext) {

  private val reg = "^[0-9a-zA-Z_=-]{60,120}$".r

  def isValidTicket(ticket: String): Boolean =
    reg.findFirstIn(ticket).nonEmpty

  implicit class AuthWSRequest(rq: WSRequest) {
    def authentified: WSRequest = rq.addHttpHeaders(
      "X-Client-Id" -> apiClientId,
      "X-Client-Secret" -> apiClientSecret,
    )
  }

  private def mapResponse[ReturnType](endpoint: String, produceSuccess: WSResponse => ReturnType, produceError: ErrorCode => ReturnType)(r: WSResponse) = {
    try {
      if (r.status != 200)
        produceError(ErrorCode(r.json.as[RequestError].errorCode, endpoint))
      else produceSuccess(r)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        produceError(GeneralErrorCodes.UnknownError)
    }
  }

  private def mapResponseToEither[SuccessType](endpoint: String)(implicit format: Reads[SuccessType]): WSResponse => Either[SuccessType, ErrorCode] = {
    mapResponse[Either[SuccessType, ErrorCode]](endpoint, r => Left(r.json.as[SuccessType]), e => Right(e))
  }

  /**
    * Get the content of a given ticket
    *
    * @param ticket the ticket to retrieve
    * @return either the ticket content or an error
    *         if successful, an [[AppTicketResponse]] is returned.
    *         if the credentials are invalid, a [[GeneralErrorCodes.InvalidAppSecret]] error is returned
    *         if the ticket doesn't exist, a [[GeneralErrorCodes.InvalidTicket]] error is returned
    *         a [[GeneralErrorCodes.UnknownError]] might be produced if something happens
    */
  def getAppTicket(ticket: String): Future[Either[AppTicketResponse, ErrorCode]] = {
    if (!isValidTicket(ticket))
      throw new IllegalArgumentException("invalid ticket")

    ws.url(apiBase + "/api/ticket/" + ticket)
      .authentified
      .get()
      .map(mapResponseToEither[AppTicketResponse]("get_ticket"))
  }

  /**
    * Gets the profile of a user. If the user didn't migrate his account to add his profile information, this will not return.
    *
    * @param userId the id of the user to return
    * @return either the profile or an error
    *         if the credentials are invalid, a [[GeneralErrorCodes.InvalidAppSecret]] error is returned
    *         if the user doesn't exist, a [[GeneralErrorCodes.UserNotFound]] error is returned
    *         a [[GeneralErrorCodes.UnknownError]] might be produced if something happens
    */
  def getAppTicket(userId: Int): Future[Either[UserProfile, ErrorCode]] = {
    ws.url(apiBase + "/api/user/" + userId)
      .authentified
      .get()
      .map(mapResponseToEither[UserProfile]("get_ticket"))
  }

  /**
    * Add an user to a group. The group must be a group in which the app owner has a write access.
    *
    * @param group  the identifier of the group
    * @param userId the user to add to the group
    * @return an option, holding nothing if everything went fine, or an error otherwise.
    *         if the credentials are invalid, a [[GeneralErrorCodes.InvalidAppSecret]] error is returned
    *         if the group doesn't exist, a [[GeneralErrorCodes.GroupNotFound]] error is returned
    *         if you can't add members to the group, a [[GeneralErrorCodes.MissingPermission]] error is returned
    *         a [[GeneralErrorCodes.UnknownError]] might be produced if something happens
    */
  def addUserToGroup(group: String, userId: Int): Future[Option[ErrorCode]] = {
    ws.url(apiBase + "/api/groups/" + group + "/members")
      .authentified
      .post(Json.toJson("userId" -> userId))
      .map(mapResponse("group_add_user", _ => None, e => Some(e)))
  }

  /**
    * Remove an user from a group. The group must be a group in which the app owner has a write access. The app owner
    * must also have the permissions to remove this user. (If the user is the group owner, this operation will always
    * fail. Same thing if the user is a group admin but the app owner isn't)
    *
    * @param group  the identifier of the group
    * @param userId the user to add to the group
    * @return an option, holding nothing if everything went fine, or an error otherwise.
    *         if the credentials are invalid, a [[GeneralErrorCodes.InvalidAppSecret]] error is returned
    *         if the group doesn't exist, a [[GeneralErrorCodes.GroupNotFound]] error is returned
    *         if the user doesn't exist or is not a member of the group, a [[GeneralErrorCodes.UserNotFound]] error is returned
    *         if you don't have the permission to remove this user, a [[GeneralErrorCodes.MissingPermission]] error is returned
    *         a [[GeneralErrorCodes.UnknownError]] might be produced if something happens
    */
  def removeUserFromGroup(group: String, userId: Int): Future[Option[ErrorCode]] = {
    ws.url(apiBase + "/api/groups/" + group + "/members/" + userId)
      .authentified
      .delete()
      .map(mapResponse("group_add_user", _ => None, e => Some(e)))
  }

  /**
    * Login as an app.
    *
    * @param appClientId the app you want to login against
    * @return a ticket that can be used to login on the given app, or an error
    *         if the credentials are invalid, a [[GeneralErrorCodes.InvalidAppSecret]] error is returned
    *         if the requested appClientId matches no app, a [[GeneralErrorCodes.UnknownApp]] error is returned
    *         a [[GeneralErrorCodes.UnknownError]] might be produced if something happens
    */
  def login(appClientId: String): Future[Either[LoginSuccess, ErrorCode]] = {
    ws.url(apiBase + "/api/app_login/" + appClientId)
      .authentified
      .get()
      .map(mapResponseToEither[LoginSuccess]("*"))
  }
}

object AuthApi {
  def apply(client: WSClient)(implicit executionContext: ExecutionContext, config: Configuration): AuthApi = {
    val clientId: String = config.get[String]("jiauth.clientId")
    val clientSecret: String = config.get[String]("jiauth.clientSecret")
    val apiRoot: String = config.get[String]("jiauth.baseUrl")

    new AuthApi(client, apiRoot, clientId, clientSecret)
  }
}