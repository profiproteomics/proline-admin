package fr.proline.admin.gui.wizard.monitor.component

import scalafx.Includes._
import scalafx.scene.control.{ TableColumn, TableView }
import scalafx.scene.control.TableColumn._
import scalafx.scene.layout.{ VBox, HBox }
import javafx.scene.{ control => jfxsc }
import scalafx.beans.property.{ BooleanProperty, StringProperty }
import scala.collection.immutable.List
import scalafx.collections.ObservableBuffer
import scalafx.beans.property.ObjectProperty
import scalafx.geometry.Pos
import scalafx.scene.control.Button

import fr.proline.admin.gui.process.UdsRepository
import fr.proline.core.orm.uds.Project
import fr.proline.admin.service.user.DeleteProject
import fr.profi.util.scalafx.TitledBorderPane
import fr.proline.admin.gui.component.resource.implicits.ProjectView
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils._
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.Monitor._
import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.wizard.util.GetConfirmation

/**
 * builds projects view
 * @author aromdhani
 *
 */
object ProjectPane extends VBox {

  //import data from  database 
  lazy val projectViews = getProjectViews()
  lazy val tableLines = ObservableBuffer(projectViews)

  //create table view 
  val projectTable = new TableView[ProjectView](tableLines) {
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
        text = "Schema version (MSI-LCMS)"
        cellValueFactory = { _.value.version }
      },
      new TableColumn[ProjectView, String] {
        text = "Size (MSI-LCMS)"
        cellValueFactory = { _.value.size }
      })
  }
  projectTable.setColumnResizePolicy(jfxsc.TableView.CONSTRAINED_RESIZE_POLICY)

  //buttons panel  
  val newProjButton = new Button {
    text = "New project"
    graphic = FxUtils.newImageView(IconResource.PLUS)
    onAction = handle {
      NewProjectPane("New Project", Some(Monitor.stage), false)
    }

  }

  val deleteProjButton = new Button {
    text = "Disable project"
    graphic = FxUtils.newImageView(IconResource.TRASH)
    onAction = handle {
      disableProject()

    }
  }

  val saveProjButton = new Button {
    text = "Save project"
    graphic = FxUtils.newImageView(IconResource.SAVE)
    onAction = handle {
      saveProject()
    }
  }

  val restoreProjButton = new Button {
    text = "Restore project"
    graphic = FxUtils.newImageView(IconResource.LOAD)
    onAction = handle {
      restoreProject()
    }
  }

  Seq(
    newProjButton,
    deleteProjButton,
    saveProjButton,
    restoreProjButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }
  val buttonsPanel = new HBox {
    spacing = 50
    alignment_=(Pos.BOTTOM_CENTER)
    children = Seq(newProjButton, deleteProjButton, saveProjButton, restoreProjButton)
  }
  val usersTitledPane = new TitledBorderPane(
    title = "Proline projects",
    titleTooltip = "Proline projects",
    contentNode = new VBox {
      spacing = 20
      children = Seq(projectTable, buttonsPanel)
    })

  spacing = 20
  children = Seq(usersTitledPane)

  /** get projectViews list from database*/
  def getProjectViews(): Seq[ProjectView] = {
    UdsRepository.getAllProjects().toBuffer[Project].sortBy(_.getId).map(new ProjectView(_))
  }

  lazy val userList = UdsRepository.getAllUserAccounts().toSeq

  /** refresh tableView*/
  def refreshTableView() {
    tableLines.clear()
    tableLines.addAll(ObservableBuffer(getProjectViews()))
  }

  /** disable project*/
  private def disableProject() {
    val confirmed = GetConfirmation("Are you sure that you want to disable the selected project? ", "Confirm your action", "Yes", "Cancel", Monitor.stage)
    if (confirmed) {
      //drop project from uds_db , keep Lcms and Msi databases 
      val dataStore = UdsRepository.getDataStoreConnFactory()
      DeleteProject(dataStore, projectTable.getSelectionModel.getSelectedItem.id.apply(), false)
      refreshTableView()
      UdsRepository.getDataStoreConnFactory()
    }
  }
  /** save project */
  private def saveProject() {
    SaveProjectPane("Save Project", Some(Monitor.stage), false)
  }

  /** restore project */
  private def restoreProject() {

    LoadProjectPane("Restore Project", Some(Monitor.stage), false)
  }
}