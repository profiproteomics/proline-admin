package fr.proline.admin.gui.wizard.service

import javafx.{ concurrent => jfxc }
import scalafx.concurrent.Service
import scala.collection.Set
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.wizard.util.HelpPopup
import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.wizard.monitor.component.UsesrsPanel
import fr.proline.admin.service.user.ChangeUserGroup

/**
 *  Change the group of Proline user(s).
 *  @author aromdhani
 *
 *  @param userIdSet set of user(s) id.
 *  @param isUser Specify if the user(s) is in user group or in admin group.
 */

class ChangeUserGrp(userIdSet: Set[Long], isUser: Boolean) extends Service(new jfxc.Service[Boolean]() {
  protected def createTask(): jfxc.Task[Boolean] = new jfxc.Task[Boolean] {
    protected def call(): Boolean = {
      val udsDbContext = UdsRepository.getUdsDbContext()
      val changeUserGrp = new ChangeUserGroup(udsDbContext, userIdSet, isUser)
      changeUserGrp.run()
      changeUserGrp.isSuccess
    }
    override def succeeded(): Unit = {
      val isChangedUsrGrp = this.get
      if (isChangedUsrGrp) {
        HelpPopup("Change user's group", s"The users with id= ${userIdSet.mkString(",")} have changed the group successfully.", Some(Monitor.stage), false)
        UsesrsPanel.refreshTableView()
      } else {
        HelpPopup("Change user's group", "The task to change user's group has failed.", Some(Monitor.stage), false)
      }
      UsesrsPanel.usersTable.selectedItems.clear()
    }
  }
})

object ChangeUserGrp {
  /**
   * Change the group of Proline user(s).
   * @author aromdhani
   *
   * @param userId The user id to change his group.
   * @param isUser Specify if the user is in user group or in admin group.
   */
  def apply(userIdSet: Set[Long], isUser: Boolean) = {
    new ChangeUserGrp(userIdSet, isUser)
  }
}