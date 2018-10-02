package fr.proline.admin.gui.wizard.service

import javafx.{ concurrent => jfxc }
import scalafx.concurrent.Service
import scala.collection.mutable.Set
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.wizard.util.HelpPopup
import fr.proline.admin.gui.Monitor
import fr.proline.admin.service.user.ChangeProjectState
import fr.proline.admin.gui.wizard.monitor.component.UsesrsPanel
import fr.proline.admin.gui.wizard.monitor.component.ProjectPane

/**
 * Change a Proline project state to activate or to disable
 * @param projectIdSet The set of project(s) id to change.
 * @param isActivated specify the state
 * @author aromdhani
 */

class ChangeProjState(projectIdSet: Set[Long], isActivated: Boolean) extends Service(new jfxc.Service[Boolean]() {
  protected def createTask(): jfxc.Task[Boolean] = new jfxc.Task[Boolean] {
    protected def call(): Boolean = {
      val udsDbContext = UdsRepository.getUdsDbContext()
      val changeProjectState = new ChangeProjectState(udsDbContext, projectIdSet, isActivated)
      changeProjectState.run()
      changeProjectState.isSuccess
    }
    override def succeeded(): Unit = {
      val isChangedProjectState = this.get
      if (isChangedProjectState) {
        HelpPopup("Change Project(s) State", s"The state of the projects with id= ${projectIdSet.mkString(",")} have been changed successfully.", Some(Monitor.stage), false)
        ProjectPane.refreshTableView()
      } else {
        HelpPopup("Change Project State", s"The task to change the state of projects with id= ${projectIdSet.mkString(",")} have been failed.", Some(Monitor.stage), false)
      }
    }
    ProjectPane.projectTable.selectedItems.clear()
  }
})

object ChangeProjState {
  /**
   * Change a Proline project state to activate or to disable.
   * @param projectId The set of project ids to activate/disable.
   * @param isActivated activate/disable the project.
   */
  def apply(projectIdSet: Set[Long], isActivated: Boolean) = {
    new ChangeProjState(projectIdSet, isActivated)
  }
}