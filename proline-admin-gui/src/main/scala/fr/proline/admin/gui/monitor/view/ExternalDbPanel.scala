package fr.proline.admin.gui.monitor.view

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.control.{ TableColumn, TableView }
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.TitledPane
import scalafx.scene.control.Button
import scalafx.scene.layout.{ VBox, HBox }

import javafx.scene.{ control => jfxsc }

import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.wizard.util.GetConfirmation
import fr.profi.util.scala.ScalaUtils._
import fr.proline.admin.gui.monitor.model.ExternalDbViewModel
import fr.proline.admin.gui.monitor.model.AdapterModel._

/**
 * ExternalDbPanel Create and display a table view of External databases.
 * @author aromdhani
 *
 */
class ExternalDbPanel(val model: ExternalDbViewModel) extends VBox with LazyLogging {
  //users table view 
  private val externalDbsTable = new TableView[ExternalDb](model.items) {
    columns ++= List(
      new TableColumn[ExternalDb, Long] {
        text = "Id"
        cellValueFactory = { _.value.dbId }
      },
      new TableColumn[ExternalDb, String] {
        text = "Name"
        cellValueFactory = { _.value.dbName }
      },
      new TableColumn[ExternalDb, String] {
        text = "Version"
        cellValueFactory = { _.value.dbVersion }
      },
      new TableColumn[ExternalDb, String] {
        text = "Port"
        cellValueFactory = { _.value.dbPort }
      },
      new TableColumn[ExternalDb, String] {
        text = "Host"
        cellValueFactory = { _.value.dbHost }
      })
  }

  //resize columns
  externalDbsTable.setColumnResizePolicy(jfxsc.TableView.CONSTRAINED_RESIZE_POLICY)

  //selected items 
  model.selectedItems = externalDbsTable.selectionModel.value.selectedItems

  //buttons panel 

  /* Refresh table view button */
  val refreshButton = new Button {
    text = "Refresh"
    tooltip = "Refresh the table of external databases."
    graphic = FxUtils.newImageView(IconResource.REFERESH)
    onAction = handle {
      model.onRefresh()
    }
  }

  /* Change database parameters button */
  val editDbEntry = new Button {
    text = "Edit database parameters"
    tooltip = "Edit the parameters of the selected external databases."
    graphic = FxUtils.newImageView(IconResource.EDITSMALL)
    onAction = handle {
      model.onChange()
    }
  }
  /* Upgrade all Proline databases button */
  val upgradeButton = new Button {
    text = "Upgrade all databases"
    tooltip = "Upgrade all Proline databases to the last version."
    graphic = FxUtils.newImageView(IconResource.UPGRADE)
    onAction = handle {
      model.onUpgradeAllDbs()
    }
  }
  /* Check for updates button */
  val checkUpdatesButton = new Button {
    text = "Check for updates"
    tooltip = "Check version the available updates for all databases."
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {
      model.onCheckForUpdates()
    }
  }

  //Layout & Style
  Seq(
    refreshButton,
    editDbEntry,
    upgradeButton,
    checkUpdatesButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 200
      b.styleClass += ("mainButtons")
    }
  val buttonsPanel = new HBox {
    spacing = 50
    alignment_=(Pos.BOTTOM_CENTER)
    children = Seq(refreshButton, editDbEntry, checkUpdatesButton, upgradeButton)
  }
  val contentNode = new VBox {
    spacing = 10
    children = Seq(externalDbsTable, buttonsPanel)
  }
  val usersTitledPane = new TitledPane {
    text = "External Databases Table"
    expanded_=(true)
    collapsible_=(false)
    content_=(contentNode)
  }
  children = Seq(usersTitledPane)

}