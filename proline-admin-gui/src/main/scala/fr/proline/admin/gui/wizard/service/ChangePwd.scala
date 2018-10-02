package fr.proline.admin.gui.wizard.service

import javafx.{ concurrent => jfxc }
import scalafx.concurrent.Service
import scalafx.stage.Stage
import scalafx.scene.control.ProgressIndicator
import scalafx.scene.control.Label

import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.service.user.ChangePassword
import fr.proline.admin.gui.wizard.monitor.component.{ UsesrsPanel, ChangePwdDialog }
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

/**
 * Reset the password of Proline user
 * @author aromdhani
 *
 */

class ChangePwd(userId: Long, password: Option[String], stage: ChangePwdDialog) extends Service(new jfxc.Service[Boolean]() {
  protected def createTask(): jfxc.Task[Boolean] = new jfxc.Task[Boolean] {
    protected def call(): Boolean = {
      /* change password */
      val udsDbContext = UdsRepository.getUdsDbContext()
      val changePwd = new ChangePassword(udsDbContext, userId, password)
      changePwd.run()
      changePwd.isSuccess
    }
    override def succeeded(): Unit = {
      val isChangedPwd = this.get
      if (isChangedPwd) {
        stage.informationLabel.visible_=(true)
        stage.progressBar.visible_=(true)
        stage.userPanel.disable_=(false)
        stage.informationLabel.setStyle(TextStyle.GREEN_ITALIC)
        stage.informationLabel.setText(s"The password of the user with id= $userId has been changed successfully.")
        //refresh table view
        UsesrsPanel.refreshTableView()
      } else {
        stage.informationLabel.setStyle(TextStyle.RED_ITALIC)
        stage.informationLabel.setText(s"Error while trying to change the user password!")
      }
      stage.progressBar.progress_=(100)
      stage.userPanel.disable_=(false)
      stage.changePwdButton.disable_=(false)
      stage.exitButton.disable_=(false)
    }
    UsesrsPanel.usersTable.selectedItems.clear()
  }
})

object ChangePwd {
  /**
   * @param userId The user id to change his password.
   * @param passsword The user id to change his password.
   * @param stage The parent stage of this dialog.
   */
  def apply(userId: Long, passsword: Option[String] = None, stage: ChangePwdDialog): ChangePwd = {
    new ChangePwd(userId, passsword, stage)
  }
}

