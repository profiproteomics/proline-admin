package fr.proline.admin.gui.wizard.service

import javafx.{ concurrent => jfxc }
import scalafx.concurrent.Service
import scala.collection.Set
import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.wizard.util.HelpPopup
import fr.proline.admin.service.user.DeleteProject
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.wizard.monitor.component.ProjectPane
/**
 * Delete/disable a set of project
 *
 * @author aromdhani
 *
 */
class DeleteUserProject(projectIds: Set[Long], dropDatabases: Boolean) extends Service(new jfxc.Service[Boolean]() {
  protected def createTask(): jfxc.Task[Boolean] = new jfxc.Task[Boolean] {
    protected def call(): Boolean = {
      val udsDbContext = UdsRepository.getUdsDbContext()
      val deleteProject = new DeleteProject(udsDbContext, projectIds, dropDatabases)
      deleteProject.run()
      deleteProject.isSuccess
    }
    override def succeeded(): Unit = {
      val isDeletedProject = this.get
      if (isDeletedProject) {
        val text =
          if (dropDatabases)
            s"The projects with id= ${projectIds.mkString(",")} have been deleted successfully. "
          else s"The projects with id= ${projectIds.mkString(",")} have been disabled successfully."
        HelpPopup("Delete project", text, Some(Monitor.stage), false)
        //refresh tableView 
        ProjectPane.refreshTableView()
      } else {
        HelpPopup("Delete project", "Error while trying to delete project !", Some(Monitor.stage), false)
      }
    }
  }
})

object DeleteUserProject {
  /**
   * @param projectId the project id to disable or to delete.
   * @param dropDatabases Specify to drop the project databases.
   */
  def apply(projectIds: Set[Long], dropDatabases: Boolean) = {
    new DeleteUserProject(projectIds, dropDatabases)
  }
}

