package fr.proline.admin.gui.wizard.service

import javafx.{ concurrent => jfxc }
import scalafx.concurrent.Service
import scalafx.stage.Stage
import scala.util.{ Try, Success }

import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.service.user.{ RestoreProject => RestoreUserProject }
import fr.proline.admin.gui.wizard.monitor.component.RestoreProjectDialog
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle
import fr.proline.admin.gui.wizard.monitor.component.ProjectPane

/**
 * Restore/load a Proline project
 * @author aromdhani
 * @param ownerId: the owner of the project
 * @param projectDirPath: specify the project directory path from where the project will be restored.
 * @param binDirPath : specify the bin directory path of postgreSQL.
 * @param projectName : specify if the project will be renamed.
 * @param stage : specify the parent stage of this dialog.
 *
 */

class RestoreProject(ownerId: Long, projectDirPath: String, binDirPath: String, projectName: Option[String] = None, restoreProjDialog: RestoreProjectDialog) extends Service(new jfxc.Service[TaskState]() {
  protected def createTask(): jfxc.Task[TaskState] = new jfxc.Task[TaskState] {
    protected def call(): TaskState = {
      try {
        val udsDbContext = UdsRepository.getUdsDbContext()
        val userName = UdsRepository.getUdsDbConfig().userName
        val restoreProject = new RestoreUserProject(udsDbContext, userName, ownerId, binDirPath, projectDirPath, projectName)
        restoreProject.doWork()
        TaskState(true, s"The project for the owner with id=#$ownerId has been restored successfully with new id =#${restoreProject.newProjId}!")
      } catch {
        case e: Exception => TaskState(false, s"Error while trying to restore project.\n${e.getMessage}")
      }
    }
    override def scheduled(): Unit = {
      restoreProjDialog.informationLabel.visible_=(true)
      restoreProjDialog.progressBar.visible_=(true)
      restoreProjDialog.loadProjectPanel.disable_=(true)
      restoreProjDialog.restoreButton.disable_=(true)
      restoreProjDialog.exitButton.disable_=(true)
    }
    override def running(): Unit = {
      restoreProjDialog.informationLabel.setStyle(TextStyle.BLUE_ITALIC)
      restoreProjDialog.informationLabel.setText(s"Restoring the project for the user with id= #$ownerId in progress, please wait...")
      restoreProjDialog.progressBar.progress_=(this.getProgress)
      restoreProjDialog.loadProjectPanel.disable_=(true)
      restoreProjDialog.restoreButton.disable_=(true)
      restoreProjDialog.exitButton.disable_=(true)
    }
    override def succeeded(): Unit = {
      val isRestProjOk = this.get.isSucceeded
      isRestProjOk match {
        case true => {
          restoreProjDialog.informationLabel.setStyle(TextStyle.GREEN_ITALIC)
          restoreProjDialog.informationLabel.setText(this.get.message)
          restoreProjDialog.progressBar.progress_=(100)
          restoreProjDialog.loadProjectPanel.disable_=(false)
          restoreProjDialog.restoreButton.disable_=(false)
          restoreProjDialog.exitButton.disable_=(false)
          ProjectPane.refreshTableView()
        }
        case _ => {
          restoreProjDialog.informationLabel.setStyle(TextStyle.RED_ITALIC)
          restoreProjDialog.informationLabel.setText(this.get.message)
          restoreProjDialog.informationLabel.visible_=(true)
          restoreProjDialog.progressBar.visible_=(false)
          restoreProjDialog.loadProjectPanel.disable_=(false)
          restoreProjDialog.restoreButton.disable_=(false)
          restoreProjDialog.exitButton.disable_=(false)
        }
      }
    }
  }
})

object RestoreProject {
  /**
   * @param ownerId: the owner of the project
   * @param projectDirPath: specify the project directory path from where the project will be restored.
   * @param binDirPath : specify the bin directory path of postgreSQL.
   * @param projectName : specify if the project will be renamed.
   * @param stage : specify the parent stage of this dialog.
   * @return id of the restored project
   */
  def apply(ownerId: Long, projectDirPath: String, binDirPath: String, projectName: Option[String], stage: RestoreProjectDialog): RestoreProject = {
    new RestoreProject(ownerId, projectDirPath, binDirPath, projectName, stage)
  }
}