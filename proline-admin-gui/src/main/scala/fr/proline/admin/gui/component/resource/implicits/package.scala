package fr.proline.admin.gui.component.resource

import scalafx.beans.property.ObjectProperty
import scalafx.concurrent.Service
import scalafx.scene.control.Button
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import scala.util.{ Try, Success, Failure }

import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.UserAccount
import fr.proline.core.orm.uds.ExternalDb
import fr.proline.repository.ProlineDatabaseType
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.repository.ConnectionMode
import fr.proline.repository.DriverType

import com.google.gson.JsonObject
import com.google.gson.JsonParser

package object implicits {

  /**
   * ************************************************************** *
   * Simplified model for uds Project, adapted to ScalaFx TableView *
   * ************************************************************** *
   */
  implicit class ProjectView(udsProject: Project) extends Ordered[ProjectView] {
    val id = new ObjectProperty(this, "id", udsProject.getId)
    val ownerLogin = new ObjectProperty(this, "owner", udsProject.getOwner.getLogin)
    val name = new ObjectProperty(this, "name", udsProject.getName)
    val databases = new ObjectProperty(this, "databases", s"lcms_db_project_${udsProject.getId} - msi_db_project_${udsProject.getId}")
    val description = new ObjectProperty(this, "description", udsProject.getDescription)
    val lcmsDbVersion = new ObjectProperty(this, "lcmsDbVersion", Try { udsProject.getExternalDatabases.find(_.getType == ProlineDatabaseType.LCMS).get.getDbVersion }.getOrElse("no.version"))
    val msiDbVersion = new ObjectProperty(this, "msiDbVersion", Try { udsProject.getExternalDatabases.find(_.getType == ProlineDatabaseType.MSI).get.getDbVersion }.getOrElse("no.version"))
    val isActivated = new ObjectProperty(this, "isActivated", Try {
      if (!new JsonParser().parse(udsProject.getSerializedProperties).getAsJsonObject.get("is_active").getAsBoolean) "Disabled" else "Active"
    }.getOrElse("Active"))
    lazy val lcmsSize = new ObjectProperty(this, "lcmsSize", UdsRepository.computeLcmsSize(udsProject.getId))
    lazy val msiSize = new ObjectProperty(this, "msiSize", UdsRepository.computeMsiSize(udsProject.getId))

    def compare(that: ProjectView) = {
      if (this.id.value == that.id.value)
        0
      else if (this.id.value > that.id.value) 1 else -1
    }
  }

  /**
   * ************************************************************** *
   * Simplified model for uds User, adapted to ScalaFx TableView *
   * ************************************************************** *
   */
  implicit class UserView(udsUserAccount: UserAccount) extends Ordered[UserView] {
    val login = new ObjectProperty(this, "owner", udsUserAccount.getLogin)
    val id = new ObjectProperty(this, "id", udsUserAccount.getId)
    val pwdHash = new ObjectProperty(this, "pwdHash", udsUserAccount.getPasswordHash)
    val mode = new ObjectProperty(this, "mode", udsUserAccount.getCreationMode())
    val group = new ObjectProperty(this, "group", Try { new JsonParser().parse(udsUserAccount.getSerializedProperties).getAsJsonObject.get("user_group").getAsString }.getOrElse("USER"))
    val isActivated = new ObjectProperty(this, "isActivated", Try {
      if (!new JsonParser().parse(udsUserAccount.getSerializedProperties).getAsJsonObject.get("is_active").getAsBoolean) "Disabled" else "Active"
    }.getOrElse("Active"))
    def compare(that: UserView) = {
      if (this.id.value == that.id.value)
        0
      else if (this.id.value > that.id.value) 1 else -1
    }
  }

  /**
   * ************************************************************** *
   * Simplified model for externalDb  to ScalaFx TableView *
   * ************************************************************** *
   */
  implicit class ExternalDbView(externaldb: ExternalDb) {

    val dbId = new ObjectProperty(this, "dbId", externaldb.getId())
    val dbName = new ObjectProperty(this, "dbName", externaldb.getDbName())
    val dbVersion = new ObjectProperty(this, "dbVersion", externaldb.getDbVersion().toString)
    val dbPort = new ObjectProperty(this, "dbPort", externaldb.getPort().toString)
    val dbHost = new ObjectProperty(this, "dbHost", externaldb.getHost())
    def _host(newHost: String) {
      dbHost.value_=(newHost)
    }
  }

  /**
   * ************************************************************** *
   * Simplified model for Task  to ScalaFx TableView *
   * ************************************************************** *
   */
  implicit class TaskView(service: String) {
    val id = new ObjectProperty(this, "id", service)
    val state = new ObjectProperty(this, "state", service)
  }
}
