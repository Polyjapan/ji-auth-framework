package ch.japanimpact.auth.api

import javax.inject.{Inject, Singleton}
import pdi.jwt.{JwtAlgorithm, JwtJson, JwtOptions}
import play.api.Configuration
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.Try

@Singleton
class TokenValidationService @Inject()(implicit executionContext: ExecutionContext, config: Configuration) {
  type SessionID = String

  private lazy val pubKeyPath: String = config.get[String]("jwt.publicKeyPath")
  private lazy val publicKey = {
    val src = if (pubKeyPath.startsWith("https://")) {
      Source.fromURL(pubKeyPath)
    } else {
      Source.fromFile(pubKeyPath)
    }

    val k = src.mkString
    src.close()
    k
  }

  def decodeToken(token: String): Try[AuthorizedUser] =
    JwtJson.decode(token, publicKey, JwtAlgorithm.allECDSA())
      .flatMap(claim => Try {
        val content = Json.parse(claim.content)
        AuthorizedUser(claim.subject.map(_.toInt).get, (content \ "grp").as[Set[String]])
      })


  def validateToken(token: String): Option[AuthorizedUser] =
    decodeToken(token).toOption
}