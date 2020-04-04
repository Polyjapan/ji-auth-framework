package ch.japanimpact.auth.api

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
    def hasScope(scope: String) = {
      if (scopes(scope)) true
      else {
        // Check for * sub scopes

        // example: for scope "uploads.staffs.add"
        scope.split("\\.") // split all parts: [uploads, staffs, add]
          .map(part => part + ".") // add a dot at the end: [uploads., staffs., add.]
          .scanLeft("")(_ + _) // cumulative apply: [, uploads., uploads.staffs., uploads.staffs.add/]
          .dropRight(1) // we don't care about the full path
          .map(path => path + "*") // [*, uploads.*, uploads.staffs.*]
          .exists(scopes) // find if one of these scopes is allowed
      }
    }
  }

}
