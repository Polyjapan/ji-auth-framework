package ch.japanimpact.auth.api

import ch.japanimpact.auth.api.AuthApi.{AppTicketRequest, AppTicketResponse}
import ch.japanimpact.auth.api.constants.GeneralErrorCodes
import ch.japanimpact.auth.api.constants.GeneralErrorCodes.{ErrorCode, RequestError}
import play.api.libs.json.{Format, Json}
import play.api.libs.ws._

import scala.concurrent.{ExecutionContext, Future}


/**
  * @author Louis Vialar
  */
class AuthApi(val ws: WSClient, val apiBase: String, val apiClientId: String, val apiClientSecret: String) {

  private val reg = "^[0-9a-zA-Z_=-]{60,120}$".r

  def isValidTicket(ticket: String): Boolean =
    reg.findFirstIn(ticket).nonEmpty

  implicit class AuthWSRequest(rq: WSRequest) {
    def authentified: WSRequest = rq.addHttpHeaders(
      "X-Client-Id" -> apiClientId,
      "X-Client-Secret" -> apiClientSecret,
    )
  }

  def getAppTicket(ticket: String)(implicit ec: ExecutionContext): Future[Either[AppTicketResponse, ErrorCode]] = {
    if (!isValidTicket(ticket))
      throw new IllegalArgumentException("invalid ticket")

    ws.url(apiBase + "/api/ticket/" + ticket)
      .authentified
      .get()
      .map(r => {
        try {
          if (r.status != 200)
            Right(ErrorCode(r.json.as[RequestError].errorCode, "get_ticket"))
          else Left(r.json.as[AppTicketResponse])
        } catch {
          case e: Exception =>
            e.printStackTrace()
            Right(GeneralErrorCodes.UnknownError)
        }
      })
  }

  def addUserToGroup(group: String, userId: Int)(implicit ec: ExecutionContext): Future[Option[ErrorCode]] = {
    ws.url(apiBase + "/api/groups/" + group + "/members")
      .authentified
      .post(Json.toJson("userId" -> userId))
      .map(r => {
        try {
          if (r.status == 400)
            Some(ErrorCode(r.json.as[RequestError].errorCode, "group_add_user"))
          else None // No error, done
        } catch {
          case e: Exception =>
            e.printStackTrace()
            Some(GeneralErrorCodes.UnknownError)
        }
      })
  }

  def removeUserFromGroup(group: String, userId: Int)(implicit ec: ExecutionContext): Future[Option[ErrorCode]] = {
    ws.url(apiBase + "/api/groups/" + group + "/members/" + userId)
      .authentified
      .delete()
      .map(r => {
        try {
          if (r.status == 400)
            Some(ErrorCode(r.json.as[RequestError].errorCode, "group_remove_user"))
          else None // No error, done
        } catch {
          case e: Exception =>
            e.printStackTrace()
            Some(GeneralErrorCodes.UnknownError)
        }
      })
  }
}

object AuthApi {


  implicit val requestMapper: Format[AppTicketRequest] = Json.format[AppTicketRequest]
  implicit val responseMapper: Format[AppTicketResponse] = Json.format[AppTicketResponse]


  class ApiRequest(val endpoint: String, val method: String)

  /**
    * The format of the request sent by the client
    *
    * @param ticket       the ticket the CAS previously sent to the user
    * @param clientId     the clientId of the requesting app
    * @param clientSecret the clientSecret of the requesting app
    */
  case class AppTicketRequest(ticket: String, clientId: String, clientSecret: String) extends ApiRequest("api/get_ticket", "POST")


  /**
    * The object returned when the ticket is found
    *
    * @param userId     the CAS id of user the ticket was generated for
    * @param userEmail  the email of the user the ticket was generated for
    * @param ticketType the type of ticket
    * @param groups     the set of groups (exposed to the app) the user is part of
    */
  case class AppTicketResponse(userId: Int, userEmail: String, ticketType: TicketType, groups: Set[String])

}
