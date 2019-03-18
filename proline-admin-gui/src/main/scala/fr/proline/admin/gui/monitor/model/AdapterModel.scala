package fr.proline.admin.gui.monitor.model

import scalafx.beans.property.ObjectProperty

import fr.proline.repository.ProlineDatabaseType
import fr.proline.core.orm.uds.{ Project => UdsProject }
import fr.proline.core.orm.uds.UserAccount
import fr.proline.core.orm.uds.{ ExternalDb => UdsExternalDb }
import fr.proline.admin.gui.monitor.database.ProjectsDB
import scala.util.Try
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import com.google.gson.JsonObject
import com.google.gson.JsonParser

package object AdapterModel {

  /**
   * ************************************************************** *
   * Simplified model for uds User, adapted to ScalaFx TableView *
   * ************************************************************** *
   */
  case class User(udsUserAccount: UserAccount) {
    val login = new ObjectProperty(this, "owner", udsUserAccount.getLogin)
    val id = new ObjectProperty(this, "id", udsUserAccount.getId)
    val pwdHash = new ObjectProperty(this, "pwdHash", udsUserAccount.getPasswordHash)
    val mode = new ObjectProperty(this, "mode", udsUserAccount.getCreationMode())
    val group = new ObjectProperty(this, "group", Try {
      new JsonParser().parse(udsUserAccount.getSerializedProperties).getAsJsonObject.get("user_group").getAsString
    }.getOrElse("USER"))
    val isActivated = new ObjectProperty(this, "isActivated", Try {
      if (!new JsonParser().parse(udsUserAccount.getSerializedProperties).getAsJsonObject.get("is_active").getAsBoolean) "Disabled" else "Active"
    }.getOrElse("Active"))
  }

  /**
   * ************************************************************** *
   * Simplified model for uds Project, adapted to ScalaFx TableView *
   * ************************************************************** *
   */

  case class Project(udsProject: UdsProject) {
    val id = new ObjectProperty(this, "id", udsProject.getId)
    val ownerLogin = new ObjectProperty(this, "owner", udsProject.getOwner.getLogin)
    val name = new ObjectProperty(this, "name", udsProject.getName)
    val description = new ObjectProperty(this, "description", udsProject.getDescription)
    val lcmsDbVersion = new ObjectProperty(this, "lcmsDbVersion", Try { udsProject.getExternalDatabases.find(_.getType == ProlineDatabaseType.LCMS).get.getDbVersion }.getOrElse("no.version"))
    val msiDbVersion = new ObjectProperty(this, "msiDbVersion", Try { udsProject.getExternalDatabases.find(_.getType == ProlineDatabaseType.MSI).get.getDbVersion }.getOrElse("no.version"))
    val isActivated = new ObjectProperty(this, "isActivated", Try {
      if (!new JsonParser().parse(udsProject.getSerializedProperties).getAsJsonObject.get("is_active").getAsBoolean) "Disabled" else "Active"
    }.getOrElse("Active"))
    lazy val lcmsSize = new ObjectProperty(this, "lcmsSize", ProjectsDB.computeLcmsSize(udsProject.getId))
    lazy val msiSize = new ObjectProperty(this, "msiSize", ProjectsDB.computeMsiSize(udsProject.getId))
  }

  /**
   * ************************************************************** *
   * Simplified model for uds ExternalDb, adapted to ScalaFx TableView *
   * ************************************************************** *
   */

  case class ExternalDb(externaldb: UdsExternalDb) {
    val dbId = new ObjectProperty(this, "dbId", externaldb.getId())
    val dbName = new ObjectProperty(this, "dbName", externaldb.getDbName())
    val dbVersion = new ObjectProperty(this, "dbVersion", externaldb.getDbVersion().toString)
    val dbPort = new ObjectProperty(this, "dbPort", externaldb.getPort().toString)
    val dbHost = new ObjectProperty(this, "dbHost", externaldb.getHost())
    def _host(newHost: String) {
      dbHost.value_=(newHost)
    }
  }
}