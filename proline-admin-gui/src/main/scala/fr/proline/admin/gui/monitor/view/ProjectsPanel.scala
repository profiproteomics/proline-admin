package fr.proline.admin.gui.monitor.view

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.control.{ TableColumn, TableView }
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.TitledPane
import scalafx.scene.control.Button
import scalafx.scene.control.SelectionMode
import scalafx.scene.layout.{ VBox, HBox }
import scalafx.collections.ObservableBuffer
import javafx.scene.{ control => jfxsc }

import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.wizard.util.GetConfirmation
import fr.profi.util.scala.ScalaUtils._

import fr.proline.admin.gui.monitor.model.ProjectViewModel
import fr.proline.admin.gui.monitor.model.AdapterModel._

/**
 * ProjectsPanel Create and display a table view of Proline projects.
 * @author aromdhani
 *
 */
class ProjectsPanel(val model: ProjectViewModel) extends VBox with LazyLogging {
  //projects table view 
  private val projectsTable = new TableView[Project](model.items) {
    columns ++= List(
      new TableColumn[Project, Long] {
        text = "Id"
        cellValueFactory = { _.value.id }
      },
      new TableColumn[Project, String] {
        text = "Owner"
        cellValueFactory = { _.value.ownerLogin }
      },
      new TableColumn[Project, String] {
        text = "Name"
        cellValueFactory = { _.value.name }
      },
      new TableColumn[Project, String] {
        text = "State"
        cellValueFactory = { _.value.isActivated }
      },
      new TableColumn[Project, String] {
        text = "Databases "
        cellValueFactory = { _.value.databases }
      },
      new TableColumn[Project, String] {
        text = "LCMS version "
        cellValueFactory = { _.value.lcmsDbVersion }
      },
      new TableColumn[Project, String] {
        text = "MSI version "
        cellValueFactory = { _.value.msiDbVersion }
      })
  }

  //resize columns
  projectsTable.setColumnResizePolicy(jfxsc.TableView.CONSTRAINED_RESIZE_POLICY)
  //selected items 
  model.selectedItems = projectsTable.selectionModel.value.selectedItems

  //buttons panel 
  val refreshButton = new Button {
    text = "Refresh"
    tooltip = "Refresh the table view."
    graphic = FxUtils.newImageView(IconResource.REFERESH)
    onAction = handle {
      model.onRefresh()
    }
  }

  val newProjButton = new Button {
    text = " Add "
    graphic = FxUtils.newImageView(IconResource.PLUS)
    tooltip = "Add a new project"
    onAction = handle {
      model.onAdd()
    }
  }

  val disableProjButton = new Button {
    text = "Disable"
    tooltip = "Disable the selected project."
    graphic = FxUtils.newImageView(IconResource.DISABLE)
    onAction = handle {
      if (!model.selectedItems.isEmpty) {
        val confirmed = GetConfirmation(s"Are you sure that you want to disable the project with id= #${model.selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")}? ", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
        if (confirmed) model.onDisable()
      }
    }
  }

  lazy val deleteProjButton = new Button {
    text = "Delete"
    tooltip = "Delete permanently the selected project."
    disable = true
    graphic = FxUtils.newImageView(IconResource.TRASH)
    onAction = handle {
      if (!model.selectedItems.isEmpty) {
        val confirmed = GetConfirmation(s"Are you sure that you want to Delete the project with id= #${model.selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")}? ", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
        if (confirmed) model.onDelete()
      }
    }
  }
  val activeProjButton = new Button {
    text = "Activate"
    tooltip = "Acivate the selected project."
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {
      if (!model.selectedItems.isEmpty) {
        val confirmed = GetConfirmation(s"Are you sure that you want to activate the project with id= #${model.selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")}? ", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
        if (confirmed) model.onActivate()
      }
    }
  }
  val archiveProjButton = new Button {
    text = "Archive"
    tooltip = "Archive the selected project."
    graphic = FxUtils.newImageView(IconResource.SAVE)
    onAction = handle {
      if (!model.selectedItems.isEmpty) {
        model.onArchive()
      }
    }
  }
  val restoreProjButton = new Button {
    text = "Restore"
    tooltip = "Restore a Proline project."
    graphic = FxUtils.newImageView(IconResource.LOAD)
    onAction = handle {
      model.onRestore()
    }
  }
  val infosButton = new Button {
    tooltip = "Show more informations about the selected project."
    text = "More Info..."
    graphic = FxUtils.newImageView(IconResource.INFO)
    onAction = handle {
      if (!model.selectedItems.isEmpty) {

      }

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

  val contentNode = new VBox {
    spacing = 10
    children = Seq(projectsTable, buttonsPanel)
  }

  val projectsTitledPane = new TitledPane {
    text = "Projects Table"
    expanded_=(true)
    collapsible_=(false)
    content_=(contentNode)
  }

  children = Seq(projectsTitledPane)

}