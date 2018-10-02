package fr.proline.admin.gui.wizard.service

import javafx.{ concurrent => jfxc }
import scalafx.concurrent.Service
import scalafx.stage.Stage
import scalafx.scene.control.ProgressIndicator
import scalafx.scene.control.Label

import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.service.user.CreateUser
import fr.proline.admin.gui.wizard.monitor.component.{ UsesrsPanel, NewUserDialog }

import fr.profi.util.security._
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

/**
 * Create a new  Proline user
 * @author aromdhani
 *
 */

class AddUser(val login: String, val pwField: Option[String], isGroupUser: Boolean, userDialog: NewUserDialog) extends Service(new jfxc.Service[Long]() {
  protected def createTask(): jfxc.Task[Long] = new jfxc.Task[Long] {
    protected def call(): Long = {
      /* Create user */
      val udsDbContext = UdsRepository.getUdsDbContext()
      val pswd = if (pwField.isDefined) sha256Hex(pwField.get) else sha256Hex("proline")
      val addUser = new CreateUser(udsDbContext, login, pswd, isGroupUser)
      addUser.run()
      addUser.userId
    }
    override def scheduled(): Unit = {
      userDialog.informationLabel.visible_=(true)
      userDialog.progressBar.visible_=(true)
      userDialog.userPanel.disable_=(true)
    }
    override def running(): Unit = {
      userDialog.informationLabel.setStyle(TextStyle.BLUE_ITALIC)
      userDialog.informationLabel.setText(s"Creating user with login $login in progress, please wait...")
      userDialog.progressBar.progress_=(this.getProgress)
      userDialog.userPanel.disable_=(true)
      userDialog.addButton.disable_=(true)
      userDialog.exitButton.disable_=(true)
    }
    override def succeeded(): Unit = {
      val userId = this.get
      if (userId > 0L) {
        userDialog.informationLabel.setStyle(TextStyle.GREEN_ITALIC)
        userDialog.informationLabel.setText(s"The user with login $login has been created successfully!")
        userDialog.progressBar.progress_=(100)
        userDialog.userPanel.disable_=(false)
        userDialog.addButton.disable_=(false)
        userDialog.exitButton.disable_=(false)
        //refresh table
        UsesrsPanel.refreshTableView()
      } else {
        userDialog.informationLabel.setStyle(TextStyle.RED_ITALIC)
        userDialog.progressBar.visible_=(false)
        userDialog.informationLabel.visible_=(true)
        userDialog.informationLabel.setText("Error while trying to create user!")
        userDialog.userPanel.disable_=(false)
        userDialog.addButton.disable_=(false)
        userDialog.exitButton.disable_=(false)
      }
    }
  }
})

object AddUser {
  /**
   * @param login: the user login
   * @param pwField: specify the owner password ,default value is 'proline'.
   * @param isGroupUser: specify if the user is in group user or in admin group, default value is in group user.
   * @param stage: specify the parent stage of this dialog.
   * @return  user
   */
  def apply(login: String, pwField: Option[String], isGroupUser: Boolean, stage: NewUserDialog): AddUser = {
    new AddUser(login, pwField, isGroupUser, stage)
  }
}

