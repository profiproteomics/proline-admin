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
import fr.proline.admin.gui.monitor.model.AdapterModel._
import fr.proline.admin.gui.monitor.model.ExternalDbViewModel
import fr.proline.admin.gui.util.GetConfirmation
import fr.proline.admin.gui.util.FxUtils
import fr.profi.util.scala.ScalaUtils._

/**
 * ExternalDbPanel Create and display a table view of External databases.
 * @author aromdhani
 *
 */
class ExternalDbPanel(val model: ExternalDbViewModel) extends VBox with LazyLogging {
  // Users table view 
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

  // Resize columns
  externalDbsTable.setColumnResizePolicy(jfxsc.TableView.CONSTRAINED_RESIZE_POLICY)

  //selected items 
  model.selectedItems = externalDbsTable.selectionModel.value.selectedItems

  //buttons panel 

  /* Refresh table view button */
  val refreshButton = new Button {
    text = "Refresh"
    tooltip = "Refresh the table view of external databases."
    graphic = FxUtils.newImageView(IconResource.REFERESH)
    onAction =  _ => {
      model.onRefresh()
    }
  }

  /* Change database parameters button */
  val editDbEntry = new Button {
    text = "Change database parameters"
    tooltip = "Change external database configuration parameters."
    graphic = FxUtils.newImageView(IconResource.EDITSMALL)
    onAction =  _ => {
      model.onChange()
    }
  }

  /* Upgrade all Proline databases button */
  val upgradeButton = new Button {
    text = "Upgrade all databases"
    tooltip = "Upgrade all Proline databases to the last version. This action can take a while."
    graphic = FxUtils.newImageView(IconResource.UPGRADE)
    onAction =  _ => {
      val confirmed = GetConfirmation(s"Are you sure that you want to upgrade all Proline databases to the last version. This action can take a while.", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
      if (confirmed) model.onUpgradeAllDbs()
    }
  }

  /* Check for updates button */
  val checkUpdatesButton = new Button {
    text = "Check for updates"
    tooltip = "Check for updates for all Proline databases."
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction =  _ => {
      val confirmed = GetConfirmation(s"Are you sure that you want to check for updates. This action can take a while.", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
      if (confirmed) model.onCheckForUpdates()
    }
  }
  /* Check for updates button */
  val deleteObsoleDbsButton = new Button {
    text = "Delete obsolete databases"
    tooltip = "Delete obsolete Proline databases. Make sure that all Proline databases are upgraded before to procede!"
    graphic = FxUtils.newImageView(IconResource.TRASH)
    disable = true
    onAction =  _ => {
      val confirmed = GetConfirmation(s"Are you sure that you want to delete obsolete Proline databases. Make sure that all databases are upgraded before to procede.", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
      if (confirmed) model.onDeleteObsoleteDbs()
    }
  }
  //Layout & Style
  Seq(
    refreshButton,
    editDbEntry,
    upgradeButton,
    checkUpdatesButton,
    deleteObsoleDbsButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 200
      b.styleClass += ("mainButtons")
    }
  val buttonsPanel = new HBox {
    spacing = 40
    alignment_=(Pos.BottomCenter)
    children = Seq(refreshButton, editDbEntry, deleteObsoleDbsButton, checkUpdatesButton, upgradeButton)
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