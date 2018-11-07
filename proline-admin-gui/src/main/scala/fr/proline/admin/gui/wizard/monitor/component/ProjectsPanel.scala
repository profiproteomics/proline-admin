package fr.proline.admin.gui.wizard.monitor.component

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.control.{ TableColumn, TableCell, TableView }
import scalafx.scene.control.TableColumn._
import javafx.scene.{ control => jfxsc }
import javafx.scene.input.MouseEvent
import javafx.event.EventHandler
import scalafx.scene.control.SelectionMode
import scalafx.scene.layout.{ VBox, HBox, StackPane, Priority }
import scalafx.scene.control.Button
import scalafx.scene.control.TitledPane
import scalafx.scene.Cursor
import scalafx.collections.ObservableBuffer
import scalafx.beans.property.BooleanProperty
import scala.collection.immutable.List
import scala.collection.Set
import fr.proline.core.orm.uds.{ UserAccount, Project }
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.wizard.service.{ DeleteUserProject, ChangeProjState }
import fr.proline.admin.gui.component.resource.implicits.ProjectView
import fr.proline.admin.gui.wizard.util.GetConfirmation
import fr.proline.admin.gui.wizard.util.HelpPopup
import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util.FxUtils
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils._
import scalafx.application.Platform

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import fr.proline.admin.gui.wizard.util.MultiSelectTableView

/**
 * ProjectPane builds a table view with the list of projects.
 *
 * @author aromdhani
 *
 */
object ProjectPane extends StackPane with LazyLogging {

  var selectedProject: Option[ProjectView] = None
  val hideColumns: BooleanProperty = BooleanProperty(false)
  //load list of project from  database  
  val projects = Await.ready(getProjectasViews(UdsRepository.getAllProjects()), Duration.Inf).value.get
  require(projects.isSuccess, "Error while trying to load projects...")
  lazy val tableLines: ObservableBuffer[ProjectView] = ObservableBuffer(projects.get)

  //create table view 
  val projectTable = new MultiSelectTableView[ProjectView](tableLines) {
    columns ++= List(
      new TableColumn[ProjectView, Long] {
        text = "Id"
        cellValueFactory = { _.value.id }
      },
      new TableColumn[ProjectView, String] {
        text = "Owner"
        cellValueFactory = { _.value.ownerLogin }
      },
      new TableColumn[ProjectView, String] {
        text = "Name"
        cellValueFactory = { _.value.name }
      },
      new TableColumn[ProjectView, String] {
        text = "State"
        cellValueFactory = { _.value.isActivated }
      },
      new TableColumn[ProjectView, String] {
        text = "Databases "
        cellValueFactory = { _.value.databases }
      },
      new TableColumn[ProjectView, String] {
        text = "LCMS version "
        cellValueFactory = { _.value.lcmsDbVersion }
      },
      new TableColumn[ProjectView, String] {
        text = "MSI version "
        cellValueFactory = { _.value.msiDbVersion }
      })
    prefWidth <== ProjectPane.width
  }

  //buttons panel  
  val extendButton = new Button {
    tooltip = "Extend the projects table to show database's size."
    graphic = FxUtils.newImageView(IconResource.PLUS)
    disable <== hideColumns
    onAction = handle {
      moreColmuns()
    }
  }
  val refreshButton = new Button {
    text = "Refresh"
    tooltip = "Refresh the table of Proline projects"
    graphic = FxUtils.newImageView(IconResource.REFERESH)

    onAction = handle {
      refreshTableView()
    }
  }

  val newProjButton = new Button {
    text = "New project"
    graphic = FxUtils.newImageView(IconResource.PLUS)
    tooltip = "Create a new project"
    onAction = handle {
      addProject()
    }
  }

  val disableProjButton = new Button {
    text = "Disable project"
    tooltip = "Disable the selected projects."
    graphic = FxUtils.newImageView(IconResource.DISABLE)
    onAction = handle {
      disableProject()
    }
  }

  lazy val deleteProjButton = new Button {
    text = "Delete project"
    tooltip = "Delete permanently the selected projects."
    disable = true
    graphic = FxUtils.newImageView(IconResource.TRASH)
    onAction = handle {
      deleteProject()
    }
  }
  val activeProjButton = new Button {
    text = "Active project"
    tooltip = "Acivate the selected projects."
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {
      activateProject()
    }
  }
  val archiveProjButton = new Button {
    tooltip = "Archive the selected project."
    text = "Archive project"
    graphic = FxUtils.newImageView(IconResource.SAVE)
    onAction = handle {
      archiveProject()
    }
  }
  val restoreProjButton = new Button {
    tooltip = "Restore a Proline project."
    text = "Restore project."
    graphic = FxUtils.newImageView(IconResource.LOAD)
    onAction = handle {
      restoreProject()
    }
  }
  val infosButton = new Button {
    tooltip = "Show more informations about the selected project."
    text = "More Info..."
    graphic = FxUtils.newImageView(IconResource.INFO)
    onAction = handle {
      moreInfos()
    }
  }
  Seq(
    refreshButton,
    newProjButton,
    disableProjButton,
    deleteProjButton,
    activeProjButton,
    archiveProjButton,
    restoreProjButton,
    infosButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 140
      b.styleClass += ("mainButtons")
    }
  val buttonsPanel = new HBox {
    spacing = 20
    alignment_=(Pos.BOTTOM_CENTER)
    children = Seq(refreshButton,
      newProjButton,
      activeProjButton,
      disableProjButton,
      deleteProjButton,
      archiveProjButton,
      restoreProjButton,
      infosButton)
  }
  val projectsTablePanel = new HBox {
    spacing = 5
    children = Seq(projectTable, extendButton)
  }

  val contentNode = new VBox {
    spacing = 10
    children = Seq(projectsTablePanel, buttonsPanel)
  }
  val usersTitledPane = new TitledPane {
    text = "Projects Table"
    expanded_=(true)
    collapsible_=(false)
    content_=(contentNode)
  }

  Platform.runLater(children = Seq(usersTitledPane))

  /**
   * get the list of project as views
   */
  private def getProjectasViews(array: => Array[Project]): Future[Seq[ProjectView]] = Future { array.toBuffer[Project].sortBy(_.getId).map(new ProjectView(_)) }

  /**
   * list of users
   */
  //var userList: Seq[UserAccount] = UdsRepository.getAllUserAccounts().toSeq

  /** refresh tableView */
  def refreshTableView() {
    getProjectasViews(UdsRepository.getAllProjects()).map { projects =>
      tableLines.clear
      tableLines.addAll(ObservableBuffer(projects))
    }
  }

  /** add a project */
  private def addProject() {
    NewProjectDialog("New Project", Some(Monitor.stage), true)
  }

  /** disable the set of selected project(s) */
  private def disableProject() {
    if (!projectTable.selectedItems.isEmpty) {
      val confirmed = GetConfirmation("Are you sure that you want to disable the selected projects ?", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
      if (confirmed) {
        val projectIds = projectTable.selectedItems.map(_.id.value)
        ChangeProjState(projectIds, false).restart()
      }
    } else {
      HelpPopup("Warning", "There is no project was selected !", Some(Monitor.stage), false)
    }
  }

  /** delete the selected project */
  private def deleteProject() {
    if (!projectTable.selectedItems.isEmpty) {
      val confirmed = GetConfirmation("Are you sure that you want to permenantly delete the selected projects ?", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
      if (confirmed) {
        val projectIds = projectTable.selectedItems.map(_.id.value)
        DeleteUserProject(projectIds, true).restart()
      }
    } else {
      HelpPopup("Warning", "There is no project was selected ! ", Some(Monitor.stage), false)
    }
  }

  /** activate the selected  project */
  private def activateProject() {
    if (!projectTable.selectedItems.isEmpty) {
      val confirmed = GetConfirmation("Are you sure that you want to activate the selected projects ?", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
      if (confirmed) {
        val projectIds = projectTable.selectedItems.map(_.id.value)
        ChangeProjState(projectIds, true).restart()
      }
    } else {
      HelpPopup("Warning", "There is no project was selected !", Some(Monitor.stage), false)
    }
  }

  /** archive the selected project */
  private def archiveProject() {
    selectedProject = Option(projectTable.getSelectionModel.getSelectedItem)
    ArchiveProjDialog("Archive Project", Some(Monitor.stage), false)
  }

  /** restore a project */
  private def restoreProject() {
    RestoreProjectDialog("Restore Project", Some(Monitor.stage), false)
  }

  /**show more columns */
  private def moreColmuns() {
    Seq(
      new TableColumn[ProjectView, String] {
        text = "LCMS db size"
        cellValueFactory = { _.value.lcmsSize }
      },
      new TableColumn[ProjectView, String] {
        text = "MSI db size"
        cellValueFactory = { _.value.msiSize }
      }).foreach { column: TableColumn[ProjectView, String] => projectTable.columns.add(column) }
    hideColumns.setValue(true)
  }

  /** show more informations about the selected project */

  private def moreInfos() {
    if (!projectTable.selectedItems.isEmpty) {
      val projectId = projectTable.getSelectionModel.getSelectedItem.id.getValue
      HelpPopup("Information", s"The databases size of project with id= $projectId : \n" +
        s" - MSI database : ${UdsRepository.computeMsiSize(projectId)}\n" +
        s" - LCMS database : ${UdsRepository.computeLcmsSize(projectId)}\n",
        Some(Monitor.stage), false)
    } else {
      HelpPopup("Warning", "There is no project was selected !", Some(Monitor.stage), false)
    }
  }

}