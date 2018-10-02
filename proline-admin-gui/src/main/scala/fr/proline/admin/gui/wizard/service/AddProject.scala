package fr.proline.admin.gui.wizard.service

import java.util.concurrent.atomic.AtomicBoolean
import javafx.{ concurrent => jfxc }
import javafx.beans.{ binding => jfxbb }
import scalafx.concurrent.Service
import scala.util.{ Try, Success, Failure }

import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.service.user.{ CreateProject, ProjectUtils }
import fr.proline.admin.service.db.CreateProjectDBs
import fr.proline.admin.gui.wizard.monitor.component.NewProjectDialog
import fr.proline.admin.gui.wizard.monitor.component.ProjectPane
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

/**
 *  Create a new Proline project
 * @author aromdhani
 *
 */

class AddProject(val projectName: String, val projectDesc: String, val ownerId: Long, projectDialog: NewProjectDialog) extends Service(new jfxc.Service[Long]() {
  protected def createTask(): jfxc.Task[Long] = new jfxc.Task[Long] {
    protected def call(): Long = {
      /* Create project */
      val udsDbContext = UdsRepository.getUdsDbContext()
      val projectCreator = new CreateProject(udsDbContext, projectName, projectDesc, ownerId)
      projectCreator.doWork()
      var projectId = projectCreator.projectId
      if (projectId > 0L) {
        val prolineConf = SetupProline.getUpdatedConfig
        // create project  databases
        new CreateProjectDBs(udsDbContext, prolineConf, projectId).doWork()
        // try to update extDbs with the current schema version
        try {
          val connectorFactory = UdsRepository.getDataStoreConnFactory()
          val msiDbConnector = connectorFactory.getMsiDbConnector(projectId)
          val msiDbVersionOpt = ProjectUtils.retrieveDbVersion(msiDbConnector)
          val lcmsDbConnector = connectorFactory.getLcMsDbConnector(projectId)
          val lcmsDbVersionOpt = ProjectUtils.retrieveDbVersion(lcmsDbConnector)
          ProjectUtils.updateExtDbs(udsDbContext, projectId, msiDbVersionOpt, lcmsDbVersionOpt)
        } catch {
          case t: Throwable => print("Error while trying to update external databases version: ", t.printStackTrace())
        }
      }
      projectId
    }
    override def scheduled(): Unit = {
      projectDialog.informationLabel.visible_=(true)
      projectDialog.progressBar.visible_=(true)
      projectDialog.projectPanel.disable_=(false)
      projectDialog.addButton.disable_=(false)
      projectDialog.exitButton.disable_=(false)
    }
    override def running(): Unit = {
      projectDialog.informationLabel.setStyle(TextStyle.BLUE_ITALIC)
      projectDialog.informationLabel.setText(s"Creating project with name $projectName in progress, please wait... ")
      projectDialog.progressBar.progress_=(this.getProgress)
      projectDialog.projectPanel.disable_=(true)
      projectDialog.addButton.disable_=(true)
      projectDialog.exitButton.disable_=(true)
    }
    override def succeeded(): Unit = {
      val projectId = this.get
      if (projectId > 0L) {
        projectDialog.informationLabel.setStyle(TextStyle.GREEN_ITALIC)
        projectDialog.informationLabel.setText(s"The project with name $projectName has been created successfully! ")
        projectDialog.progressBar.progress_=(100)
        projectDialog.projectPanel.disable_=(false)
        projectDialog.addButton.disable_=(false)
        projectDialog.exitButton.disable_=(false)
        ProjectPane.refreshTableView()
      } else {
        projectDialog.informationLabel.setStyle(TextStyle.RED_ITALIC)
        // projectDialog.informationLabel.setText(s"Error while trying to create project with name $projectName :$t")
        projectDialog.informationLabel.setText(s"Error while trying to create project!")
        projectDialog.progressBar.visible_=(false)
        projectDialog.informationLabel.visible_=(true)
        projectDialog.projectPanel.disable_=(false)
        projectDialog.addButton.disable_=(false)
        projectDialog.exitButton.disable_=(false)
      }
    }
  }
})

object AddProject {
  /**
   * @param projectName: the project name
   * @param projectDesc : specify the project description is optional.
   * @param ownerId: specify the owner id of the project.
   * @param stage : specify the parent stage of this dialog.
   * @return the project id.
   */
  def apply(projectName: String, projectDesc: String, ownerId: Long, stage: NewProjectDialog): AddProject = {
    new AddProject(projectName, projectDesc, ownerId, stage)
  }
}