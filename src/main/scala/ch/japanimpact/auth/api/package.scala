package ch.japanimpact.auth

import play.api.libs.json.{Format, Json}

/**
  * @author Louis Vialar
  */
package object api {
  implicit val requestMapper: Format[AppTicketRequest] = Json.format[AppTicketRequest]
  implicit val responseMapper: Format[AppTicketResponse] = Json.format[AppTicketResponse]
  implicit val successMapper: Format[LoginSuccess] = Json.format[LoginSuccess]

  /**
    * The format of the request sent by the client
    *
    * @param ticket       the ticket the CAS previously sent to the user
    * @param clientId     the clientId of the requesting app
    * @param clientSecret the clientSecret of the requesting app
    */
  case class AppTicketRequest(ticket: String, clientId: String, clientSecret: String)


  /**
    * The object returned when the ticket is found
    *
    * @param userId     the CAS id of user the ticket was generated for
    * @param userEmail  the email of the user the ticket was generated for
    * @param ticketType the type of ticket
    * @param groups     the set of groups (exposed to the app) the user is part of
    */
  case class AppTicketResponse(userId: Int, userEmail: String, ticketType: TicketType, groups: Set[String])

  /**
    * The object returned when the login is successful
    *
    * @param ticket the returned ticket
    */
  case class LoginSuccess(ticket: String)
}