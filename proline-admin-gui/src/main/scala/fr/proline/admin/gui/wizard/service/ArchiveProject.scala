package fr.proline.admin.gui.wizard.service

import javafx.{ concurrent => jfxc }
import scalafx.concurrent.Service
import scalafx.stage.Stage
import scala.util.{ Try, Success }

import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.service.user.{ ArchiveProject => ArchiveUserProject }
import fr.proline.admin.gui.wizard.monitor.component.ArchiveProjDialog
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle
import fr.proline.admin.gui.wizard.monitor.component.ProjectPane

/**
 * Service  to archive a Proline project.
 *
 * @author aromdhani
 */

class ArchiveProject(projectId: Long, binDirPath: String, archiveProjectDirPath: String, dialog: ArchiveProjDialog) extends Service(new jfxc.Service[Try[Boolean]]() {

  protected def createTask(): jfxc.Task[Try[Boolean]] = new jfxc.Task[Try[Boolean]] {
    protected def call(): Try[Boolean] = {
      Try {
        val udsDbContext = UdsRepository.getUdsDbContext()
        val userName = UdsRepository.getUdsDbConfig().userName
        val archiveProject = new ArchiveUserProject(udsDbContext, userName, projectId, binDirPath, archiveProjectDirPath)
        archiveProject.run()
        archiveProject.isSuccess
      }
    }
    override def scheduled(): Unit = {
      dialog.informationLabel.setStyle(TextStyle.BLUE_ITALIC)
      dialog.informationLabel.visible_=(true)
      dialog.progressBar.visible_=(true)
      dialog.loadProjectPanel.disable_=(true)
    }
    override def running(): Unit = {
      dialog.informationLabel.setStyle(TextStyle.BLUE_ITALIC)
      dialog.informationLabel.setText(s"Archiving project with id= #$projectId in progress, please wait...")
      dialog.progressBar.progress_=(this.getProgress)
      dialog.loadProjectPanel.disable_=(false)
      dialog.archiveButton.disable_=(false)
      dialog.exitButton.disable_=(false)
    }
    override def succeeded(): Unit = {
      val isArchProjOk = this.get
      isArchProjOk match {
        case Success(isArchProjOk) if (isArchProjOk) => {
          dialog.informationLabel.setStyle(TextStyle.GREEN_ITALIC)
          dialog.informationLabel.setText(s"The project with id $projectId has been archived successfully!")
          dialog.informationLabel.setStyle(TextStyle.GREEN_ITALIC)
          dialog.progressBar.progress_=(100)
          dialog.loadProjectPanel.disable_=(false)
          dialog.archiveButton.disable_=(false)
          dialog.exitButton.disable_=(false)
          ProjectPane.refreshTableView()
        }
        case _ => {
          dialog.informationLabel.setStyle(TextStyle.RED_ITALIC)
          dialog.progressBar.visible_=(false)
          dialog.informationLabel.visible_=(true)
          dialog.informationLabel.setText("Error while trying to archive project!")
          dialog.contentPane.disable_=(false)
        }
      }
    }
  }
})

object ArchiveProject {
  /**
   * @param projectId: The project id to restore.
   * @param binPath : Specify the bin directory path of postgreSQL.
   * @param archivePath: Specify the project directory path  where the project will be archived.
   * @param stage : Specify the parent stage of this dialog.
   * @return <code>true</code> if the project has been archived successfully.
   */
  def apply(projectId: Long, binPath: String, archivePath: String, stage: ArchiveProjDialog): ArchiveProject = {
    new ArchiveProject(projectId, binPath, archivePath, stage)
  }
}
