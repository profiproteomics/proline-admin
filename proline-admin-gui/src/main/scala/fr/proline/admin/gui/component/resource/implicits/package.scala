package fr.proline.admin.gui.component.resource

import scalafx.beans.property.ObjectProperty
import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.UserAccount


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
    // schema version
    // taille db
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
  }
}
