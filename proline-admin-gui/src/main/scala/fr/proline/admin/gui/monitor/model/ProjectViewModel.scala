package fr.proline.admin.gui.monitor.model

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.application.Platform
import scalafx.beans.property.{ BooleanProperty, ObjectProperty }
import scalafx.stage.Window

import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.util.FxUtils
import fr.profi.util.scala.ScalaUtils._
import fr.proline.admin.gui.monitor.database.ProjectsDB
import fr.proline.admin.gui.monitor.model.AdapterModel._
import fr.proline.admin.gui.monitor.view.dialog._
import fr.proline.admin.gui.util.ShowPopupWindow
import fr.proline.admin.gui.task.TaskRunner

/**
 * The project view model. Defines UI actions and database actions via projectsDB.
 * @author aromdhani
 *
 */
class ProjectViewModel extends LazyLogging {

  var taskRunner: TaskRunner = _
  private val projectsDB = ProjectsDB

  val parentWindow: ObjectProperty[Window] = ObjectProperty[Window](null.asInstanceOf[Window])

  val items: ObservableBuffer[Project] = new ObservableBuffer[Project]()

  // Read-only collection of rows selected in the table view
  var _selectedItems: ObservableBuffer[Project] = _
  def selectedItems: ObservableBuffer[Project] = _selectedItems
  def selectedItems_=(v: ObservableBuffer[Project]): Unit = {
    _selectedItems = v
    _selectedItems.onChange {
      canRemoveRow.value = selectedItems.nonEmpty
    }
  }

  val canRemoveRow = BooleanProperty(false)

  /** Initialize the table view with projects from UDS database */
  def onInitialize(): Unit = {
    items.clear()
    items ++= projectsDB.initialize()
  }

  /** Add a Proline Project */
  def onAdd(): Unit = {
    val result = AddProjectDialog.showAndWait(Monitor.stage)
    result match {
      case Some(projectCreator) =>
        taskRunner.run(
          caption = s"Creating project with name= #${projectCreator.name}",
          op = {
            // Create project 
            logger.info(s"Creating project with name= #${projectCreator.name}")
            projectsDB.add(projectCreator.name, projectCreator.description, projectCreator.ownerId)

            val updatedItems = projectsDB.queryProjectsAsView()
            // Update items on FX thread
            Platform.runLater {
              updateItems(updatedItems)
            }
          })
      case _ =>

    }

  }

  /** Disable a Proline project */
  def onDisable(): Unit = {
    taskRunner.run(
      caption = s"Disabling the project with id= #${selectedItems.headOption.map(_.id.value).getOrElse("no project was selected")}",
      op = {
        // Disable project 
        logger.info(s"Disabling the user project id= #${selectedItems.headOption.map(_.id.value).getOrElse("no project was selected")}")
        projectsDB.changeState(selectedItems.toSet, false)
        // Return items from database
        val updatedItems = projectsDB.queryProjectsAsView()
        // Update items on FX thread
        Platform.runLater {
          updateItems(updatedItems)
        }
      })
  }

  /** Activate a Proline project  */
  def onActivate(): Unit = {
    taskRunner.run(
      caption = s"Activating the project with id= #${selectedItems.headOption.map(_.id.value).getOrElse("no project was selected")}",
      op = {
        // Activate project 
        logger.info(s"Activating the project with id= #${selectedItems.headOption.map(_.id.value).getOrElse("no project was selected")}")
        projectsDB.changeState(selectedItems.toSet, true)

        // Return items from database
        val updatedItems = projectsDB.queryProjectsAsView()
        // Update items on FX thread
        Platform.runLater {
          updateItems(updatedItems)
        }
      })
  }

  /** Delete a Proline project  */
  def onDelete(): Unit = {
    taskRunner.run(
      caption = s"Deleting the project with id= #${selectedItems.headOption.map(_.id.value).getOrElse("no project was selected")}",
      op = {
        logger.info(s"Deleting the project with id= #${selectedItems.headOption.map(_.id.value).getOrElse("no project was selected")}")
        projectsDB.delete(selectedItems.toSet, true)

        // Return items from database
        val updatedItems = projectsDB.queryProjectsAsView()
        // Update items on FX thread
        Platform.runLater {
          updateItems(updatedItems)
        }
      })
  }

  /** Refresh table view */
  def onRefresh(): Unit = {
    taskRunner.run(
      caption = "Refreshing the table view ",
      op = {
        logger.info("Refreshing the table view")
        items.clear()
        items ++= projectsDB.initialize()
      })
  }

  /** Archive a Proline project */
  def onArchive() {
    val result = ArchiveProjectDialog.showAndWait(Monitor.stage)
    result match {
      case Some(archiveProject) =>
        taskRunner.run(
          caption = s"Archiving project with id= #${selectedItems.headOption.map(_.id.value).get}",
          op = {
            // Archiving project 
            logger.info(s"Archiving project with id= #${selectedItems.headOption.map(_.id.value).get}")
            projectsDB.archive(selectedItems.headOption.map(_.id.value).get, archiveProject.binDirPath, archiveProject.archiveLocationPath)

            // Return items from database            
            val updatedItems = projectsDB.queryProjectsAsView()
            // Update items on FX thread          
            Platform.runLater {
              updateItems(updatedItems)
            }
          })
      case _ =>

    }

  }

  /** Restore a Proline project */
  def onRestore() {
    val result = RestoreProjectDialog.showAndWait(Monitor.stage)
    result match {
      case Some(restoreProject) =>
        taskRunner.run(
          caption = s"Restoring project",
          op = {
            // Restore project 
            logger.info(s"Restoring project")
            projectsDB.restore(restoreProject.ownerId,
              restoreProject.binDirPath,
              restoreProject.archivedProjDirPath,
              restoreProject.projectName)
            // Return items from database
            val updatedItems = projectsDB.queryProjectsAsView()

            // Update items on FX thread
            Platform.runLater {
              updateItems(updatedItems)
            }
          })
      case _ =>

    }

  }
  
  /** show more info about the the slected project */
  def onMoreInfo() {
    val projectId = selectedItems.headOption.map(_.id.value).get
    ShowPopupWindow(s"Databases size of project with id= #$projectId : \n" +
      s" - MSI database : ${if (ProjectsDB.computeMsiSize(projectId).isDefined) ProjectsDB.computeMsiSize(projectId).get}\n" +
      s" - LCMS database : ${if (ProjectsDB.computeLcmsSize(projectId).isDefined) ProjectsDB.computeLcmsSize(projectId).get}\n",
      "Info",
      Some(Monitor.stage),
      false)
  }
  
  /** Update items in table view */
  private def updateItems(updatedItems: Seq[Project]): Unit = {
    val toAdd = updatedItems.diff(items)
    val toRemove = items.diff(updatedItems)

    items ++= toAdd
    items --= toRemove
  }

}