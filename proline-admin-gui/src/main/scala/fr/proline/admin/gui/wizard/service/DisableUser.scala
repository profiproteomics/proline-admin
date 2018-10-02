package fr.proline.admin.gui.wizard.service

import javafx.{ concurrent => jfxc }
import scalafx.concurrent.Service
import scala.collection.Set

import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.wizard.util.HelpPopup
import fr.proline.admin.gui.Monitor
import fr.proline.admin.service.user.ChangeUserState
import fr.proline.admin.gui.wizard.monitor.component.UsesrsPanel

/**
 * disable a Proline user(s)
 *
 * @author aromdhani
 * @param userIdSet the user(s) set id to activate or to disable.
 * @param isActivated Specifiy <code>true</code> if the user is activated otherwise is false.
 *
 */

class DisableUser(userIdSet: Set[Long], isActivated: Boolean) extends Service(new jfxc.Service[Boolean]() {
  protected def createTask(): jfxc.Task[Boolean] = new jfxc.Task[Boolean] {
    protected def call(): Boolean = {
      val udsDbContext = UdsRepository.getUdsDbContext()
      val changeUserState = new ChangeUserState(udsDbContext, userIdSet, isActivated)
      changeUserState.run()
      changeUserState.isSuccess
    }
    override def succeeded(): Unit = {
      val isDisabledUser = this.get
      if (isDisabledUser) {
        HelpPopup("Disable user", s"The users with id= ${userIdSet.mkString(",")} have been disabled successfully.", Some(Monitor.stage), false)
        //refresh table view
        UsesrsPanel.refreshTableView()
      } else {
        HelpPopup("Disable user", s"The task to disable the users with id= ${userIdSet.mkString(",")} have been failed.", Some(Monitor.stage), false)
      }
      //empty the selected items 
      UsesrsPanel.usersTable.selectedItems.clear()
    }
  }
})

object DisableUser {
  /**
   * disable Proline user(s)
   * @param userIdSet the user(s) set id to activate or to disable.
   * @param isActivated specify <code>true</code> if the user is activated otherwise is false.
   */
  def apply(userIdSet: Set[Long], isActivated: Boolean) = {
    new DisableUser(userIdSet, isActivated)
  }
}