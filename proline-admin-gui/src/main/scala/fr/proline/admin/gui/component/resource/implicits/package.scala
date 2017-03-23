package fr.proline.admin.gui.component.resource

import scalafx.beans.property.ObjectProperty
import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.UserAccount
import fr.proline.core.orm.uds.ExternalDb
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import fr.proline.repository.ProlineDatabaseType
import fr.proline.admin.gui.process.UdsRepository

package object implicits {

  /**
   * ************************************************************** *
   * Simplified model for uds Project, adapted to ScalaFx TableView *
   * ************************************************************** *
   */
  implicit class ProjectView(udsProject: Project) {
    //class ProjectView(owner:UserAccount, project: Project, schemaVersion: String, dbSize:Double) {
  
    val id = new ObjectProperty(this, "id", udsProject.getId)
    val ownerLogin = new ObjectProperty(this, "owner", udsProject.getOwner.getLogin)
    val name = new ObjectProperty(this, "name", udsProject.getName)
    val description = new ObjectProperty(this, "description", udsProject.getDescription)
    val version = new ObjectProperty(this, "version", dbVersions(udsProject.getExternalDatabases))
    val size = new ObjectProperty(this, "size",UdsRepository.calculateSize(udsProject.getId))
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

    val login = new ObjectProperty(this, "owner", udsUserAccount.getLogin)
    val id = new ObjectProperty(this, "id", udsUserAccount.getId)
    val pwdHash = new ObjectProperty(this, "pwdHash", udsUserAccount.getPasswordHash)
    val serializedProps = new ObjectProperty(this, "serializedProps", udsUserAccount.getSerializedProperties)
    val mode = new ObjectProperty(this,"mode",udsUserAccount.getCreationMode())
  }
}
