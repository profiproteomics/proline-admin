package fr.proline.admin.gui.monitor.view

import com.typesafe.scalalogging.LazyLogging
import fr.proline.admin.gui.{IconResource, Monitor}
import fr.proline.admin.gui.monitor.model.AdapterModel._
import fr.proline.admin.gui.monitor.model.ProjectViewModel
import fr.proline.admin.gui.util.{FxUtils, GetConfirmation}
import javafx.scene.{control => jfxsc}
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.{Button, TableColumn, TableView, TitledPane}
import scalafx.scene.layout.{HBox, VBox}

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
    onAction =  _ => {
      model.onRefresh()
    }
  }

  val newProjButton = new Button {
    text = " Add "
    graphic = FxUtils.newImageView(IconResource.PLUS)
    tooltip = "Add a new project"
    onAction =  _ => {
      model.onAdd()
    }
  }

  val disableProjButton = new Button {
    text = "Disable"
    tooltip = "Disable the selected project."
    graphic = FxUtils.newImageView(IconResource.DISABLE)
    onAction =  _ => {
      if (!model.selectedItems.isEmpty) {
        val confirmed = GetConfirmation(s"Are you sure that you want to disable the project with id= #${model.selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")}? ", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
        if (confirmed) model.onDisable()
      }
    }
  }

//  lazy val deleteProjButton = new Button {
//    text = "Delete"
//    tooltip = "Delete PERMANENTLY the selected project."
//    graphic = FxUtils.newImageView(IconResource.TRASH)
//    onAction =  _ => {
//      if (!model.selectedItems.isEmpty) {
//        val confirmed = GetConfirmation(s"Are you sure that you want to delete PERMANENTLY the project with id= #${model.selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")}? \n Warning : This is not reversible ", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
//        if (confirmed) model.onDelete()
//      }
//    }
//  }
  val activeProjButton = new Button {
    text = "Activate"
    tooltip = "Activate the selected project."
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction =  _ => {
      if (!model.selectedItems.isEmpty) {
        val confirmed = GetConfirmation(s"Are you sure that you want to activate the project with id= #${model.selectedItems.headOption.map(_.id.value).getOrElse("no item was selected")}? ", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
        if (confirmed) model.onActivate()
      }
    }
  }
  val changeProjOwnerButton = new Button {
    tooltip = "Change the selected project owner."
    text = "Change owner"
    graphic = FxUtils.newImageView(IconResource.EDITSMALL)
    onAction =  _ => {
      if (!model.selectedItems.isEmpty) {
        model.onChangeOwner()
      }
    }
  }
//  val archiveProjButton = new Button {
//    text = "Archive"
//    tooltip = "Archive the selected project."
//    graphic = FxUtils.newImageView(IconResource.SAVE)
//    onAction =  _ => {
//      if (!model.selectedItems.isEmpty) {
//        model.onArchive()
//      }
//    }
//  }
//  val restoreProjButton = new Button {
//    text = "Restore"
//    tooltip = "Restore a Proline project."
//    graphic = FxUtils.newImageView(IconResource.LOAD)
//    onAction =  _ => {
//      model.onRestore()
//    }
//  }
  val infosButton = new Button {
    tooltip = "Show more informations about the selected project."
    text = "More Info..."
    graphic = FxUtils.newImageView(IconResource.INFO)
    onAction =  _ => {
      if (!model.selectedItems.isEmpty) {
        model.onMoreInfo()
      }
    }
  }
  Seq(
    refreshButton,
    newProjButton,
    disableProjButton,
//    deleteProjButton,
    activeProjButton,
    changeProjOwnerButton,
//    archiveProjButton,
//    restoreProjButton,
    infosButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 140
      b.styleClass += ("mainButtons")
    }
  val buttonsPanel = new HBox {
    spacing = 15
    alignment_=(Pos.BottomLeft)
    children = Seq(refreshButton,
      newProjButton,
      activeProjButton,
      disableProjButton,
      changeProjOwnerButton,
//      deleteProjButton,
//      archiveProjButton,
//      restoreProjButton,
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