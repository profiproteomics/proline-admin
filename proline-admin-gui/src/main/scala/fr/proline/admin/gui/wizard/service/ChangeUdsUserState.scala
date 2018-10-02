package fr.proline.admin.gui.wizard.service

import javafx.{ concurrent => jfxc }
import scalafx.concurrent.Service
import scala.collection.Set
import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.wizard.monitor.component.UsesrsPanel
import fr.proline.admin.gui.wizard.util.HelpPopup
import fr.proline.admin.service.user.ChangeUserState

/**
 * Change a set of Proline user(s) state to activate or to disabled.
 * @author aromdhani
 * @param userIdSet The set of user(s) id to activate or to disable.
 * @param isActivate to activate or to disable a Proline user(s).
 */
class ChangeUdsUserState(userIdSet: Set[Long], isActivate: Boolean) extends Service(new jfxc.Service[Boolean]() {
  protected def createTask(): jfxc.Task[Boolean] = new jfxc.Task[Boolean] {
    protected def call(): Boolean = {
      val udsDbContext = UdsRepository.getUdsDbContext()
      val changeUserState = new ChangeUserState(udsDbContext, userIdSet, isActivate)
      changeUserState.run()
      changeUserState.isSuccess
    }
    override def succeeded(): Unit = {
      val isActivatedUser = this.get
      if (isActivatedUser) {
        HelpPopup("Activate user", s"The users with id= ${userIdSet.mkString(",")} have been activated successfully.", Some(Monitor.stage), false)
        //refresh table view
        UsesrsPanel.refreshTableView()
      } else {
        HelpPopup("activate user", s"The task to activate the users with id= ${userIdSet.mkString(",")} have been failed.", Some(Monitor.stage), false)
      }
      UsesrsPanel.usersTable.selectedItems.clear()
    }
  }
})

object ChangeUdsUserState {
  /**
   * Change a set of Proline user(s) state to activated or to disabled.
   * @param userIdSet The set of user(s) id to activate or to disable.
   * @param isActivate specify to activate or to disable a Proline user(s).
   */
  def apply(userIdSet: Set[Long], isActivated: Boolean) = {
    new ChangeUdsUserState(userIdSet, isActivated)
  }
}