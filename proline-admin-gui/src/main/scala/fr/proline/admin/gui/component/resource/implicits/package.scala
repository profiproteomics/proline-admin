package fr.proline.admin.gui.component.resource

import scalafx.beans.property.ObjectProperty
import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.UserAccount
import fr.proline.core.orm.uds.ExternalDb
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import fr.proline.repository.ProlineDatabaseType
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.repository.ProlineDatabaseType
import fr.proline.repository.ConnectionMode
import fr.proline.repository.DriverType
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import scalafx.concurrent.Service
import javafx.beans.property.{ BooleanProperty, StringProperty }
import java.lang.Boolean

package object implicits {

  /**
   * ************************************************************** *
   * Simplified model for uds Project, adapted to ScalaFx TableView *
   * ************************************************************** *
   */
  class ProjectView(udsProject: Project) {
    //class ProjectView(owner:UserAccount, project: Project, schemaVersion: String, dbSize:Double) {
    val id = new ObjectProperty(this, "id", udsProject.getId)
    val ownerLogin = new ObjectProperty(this, "owner", udsProject.getOwner.getLogin)
    val name = new ObjectProperty(this, "name", udsProject.getName)
    val description = new ObjectProperty(this, "description", udsProject.getDescription)
    val version = new ObjectProperty(this, "version", dbVersions(udsProject.getExternalDatabases))
    val size = new ObjectProperty(this, "size", UdsRepository.calculateSize(udsProject.getId))

    // schema version
    def dbVersions(extDbs: java.util.Set[ExternalDb]): String = {
      val vesrion = new StringBuilder()
      var msiVersion = ""
      var lcmsVersion = ""
      extDbs.toList.foreach { externalDb =>
        if (externalDb.getType() == ProlineDatabaseType.LCMS) lcmsVersion = externalDb.getDbVersion()
        else if (externalDb.getType() == ProlineDatabaseType.MSI) msiVersion = externalDb.getDbVersion()
      }
      vesrion.append(msiVersion).append(" - ").append(lcmsVersion)
      vesrion.toString
    }
  }

  /**
   * ************************************************************** *
   * Simplified model for uds User, adapted to ScalaFx TableView *
   * ************************************************************** *
   */
  implicit class UserView(udsUserAccount: UserAccount) {
    var userGroups = ""
    var userIsActives = "Active"
    val login = new ObjectProperty(this, "owner", udsUserAccount.getLogin)
    val id = new ObjectProperty(this, "id", udsUserAccount.getId)
    val pwdHash = new ObjectProperty(this, "pwdHash", udsUserAccount.getPasswordHash)
    val mode = new ObjectProperty(this, "mode", udsUserAccount.getCreationMode())
    if ((udsUserAccount.getSerializedProperties != null) && (!udsUserAccount.getSerializedProperties.isEmpty)) {
      if (!ConfigFactory.parseString(udsUserAccount.getSerializedProperties).isEmpty) {
        try {
          userGroups = ConfigFactory.parseString(udsUserAccount.getSerializedProperties).root().get("user_group").unwrapped().toString
        } catch {
          case e: Exception =>
            userGroups = ""
        }
      }
    }
    if ((udsUserAccount.getSerializedProperties != null) && (!udsUserAccount.getSerializedProperties.isEmpty)) {
      if (!ConfigFactory.parseString(udsUserAccount.getSerializedProperties).isEmpty) {
        try {
          if (ConfigFactory.parseString(udsUserAccount.getSerializedProperties).root().get("is_active").unwrapped().toString.toBoolean == false) {
            userIsActives = "Disabled"
          }
        } catch {
          case e: Exception =>
            userIsActives = "Active"
        }
      }
    }
    val userGroup = new ObjectProperty(this, "userGroup", userGroups)
    val userIsActive = new ObjectProperty(this, "userIsActive", userIsActives)
  }

  /**
   * ************************************************************** *
   * Simplified model for externalDb  to ScalaFx TableView *
   * ************************************************************** *
   */
  implicit class ExternalDbView(externaldb: ExternalDb) {

    val dbId = new ObjectProperty(this, "dbId", externaldb.getId())
    val dbName = new ObjectProperty(this, "dbName", externaldb.getDbName())
    val dbPassword = new ObjectProperty(this, "dbPassword", externaldb.getDbPassword())
    val dbUser = new ObjectProperty(this, "dbUser", externaldb.getDbUser())
    val dbVersion = new ObjectProperty(this, "dbVersion", externaldb.getDbVersion().toString)
    val dbPort = new ObjectProperty(this, "dbPort", externaldb.getPort().toString)
    val dbHost = new ObjectProperty(this, "dbHost", externaldb.getHost())

  }

  /**
   * ************************************************************** *
   * Simplified model for Task  to ScalaFx TableView *
   * ************************************************************** *
   */
  //  implicit class TaskView(service: Service) {
  //    val taskState = new ObjectProperty(this, "taskState", service)
  //  }
}
