package fr.proline.admin.gui.wizard.monitor.component

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.scene.control.{ TableColumn, TableView, TableCell }
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.Button
import scalafx.scene.control.TitledPane
import scalafx.scene.control.SelectionMode
import javafx.scene.{ control => jfxsc }
import scalafx.scene.layout.{ VBox, HBox }
import scalafx.geometry.Pos
import scalafx.scene.Cursor
import scalafx.collections.ObservableBuffer

import scala.collection.immutable.List
import scala.collection.mutable.Set
import javafx.scene.input.MouseEvent
import javafx.event.EventHandler

import fr.proline.core.orm.uds.ExternalDb
import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util.FxUtils

import fr.proline.admin.gui.wizard.service.UpgradeDatabases
import fr.proline.admin.gui.wizard.service.CheckForUpdates
import fr.proline.admin.gui.component.resource.implicits.ExternalDbView
import fr.proline.admin.gui.wizard.util.MultiSelectTableView
import fr.proline.admin.gui.wizard.util.{ GetConfirmation, HelpPopup }
import fr.profi.util.scala.ScalaUtils._

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * UsesrsPane builds a table of external databases
 * @author aromdhani
 *
 */
object DatabasesPanel extends VBox with LazyLogging {
  //load list of users from database 
  val databases = Await.ready(getExtDbasViews(UdsRepository.getAllExtDbs()), Duration.Inf).value.get
  require(databases.isSuccess, "Error while trying to load external databases...")
  lazy val tableLines: ObservableBuffer[ExternalDbView] = ObservableBuffer(databases.get)
  //create the table user view 
  val externalDbTable = new MultiSelectTableView[ExternalDbView](tableLines) {
    columns ++= List(
      new TableColumn[ExternalDbView, Long] {
        text = "Id"
        cellValueFactory = { _.value.dbId }
      },
      new TableColumn[ExternalDbView, String] {
        text = "Name"
        cellValueFactory = { _.value.dbName }
      },
      new TableColumn[ExternalDbView, String] {
        text = "Version"
        cellValueFactory = { _.value.dbVersion }
      },
      new TableColumn[ExternalDbView, String] {
        text = "Port"
        cellValueFactory = { _.value.dbPort }
      },
      new TableColumn[ExternalDbView, String] {
        text = "Host"
        cellValueFactory = { _.value.dbHost }
      })
  }

  /*  refresh table view */
  val refreshButton = new Button {
    text = "Refresh"
    tooltip = "Refresh the table of external databases."
    graphic = FxUtils.newImageView(IconResource.REFERESH)
    onAction = handle {
      refreshTableView()
    }
  }
  /*change database properties  */
  val editDbEntry = new Button {
    text = "Edit database properties"
    tooltip = "Edit the properties of the selected external databases."
    graphic = FxUtils.newImageView(IconResource.EDITSMALL)
    onAction = handle {
      changeDbProperties()
    }
  }
  val upgradeButton = new Button {
    text = "Upgrade Proline databases"
    tooltip = "Upgrade all databases to the last version."
    graphic = FxUtils.newImageView(IconResource.UPGRADE)
    onAction = handle {
      upgradeAllDbs()
    }
  }

  val checkUpdatesButton = new Button {
    text = "Check for updates"
    tooltip = "Check version the available updates for all databases."
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {
      checkDbsVerion()
    }
  }

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
    children = Seq(externalDbTable, buttonsPanel)
  }
  val usersTitledPane = new TitledPane {
    text = "External Databases Table"
    expanded_=(true)
    collapsible_=(false)
    content_=(contentNode)
  }
  children = Seq(usersTitledPane)

  def getExtDbasViews(array: => Array[ExternalDb]): Future[Seq[ExternalDbView]] = Future { array.toBuffer[ExternalDb].sortBy(_.getId).map(new ExternalDbView(_)) }

  /** change external database properties */
  def changeDbProperties() {
    ChangeExtDbPropDialog("Change Database Properties", Some(Monitor.stage), false)
  }

  /** upgrade all Proline databases */
  private def upgradeAllDbs() {
    val confirmed = GetConfirmation("Are you sure that you want to upgrade all databases. This action might take several minutes?", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
    if (confirmed) UpgradeDatabases.Worker.restart()
  }

  /** check for database updates */
  def checkDbsVerion() {
    val confirmed = GetConfirmation("Are you sure that you want to check for updates. This action might take several minutes?", "Confirm your action", " Yes ", "Cancel", Monitor.stage)
    if (confirmed) CheckForUpdates.Worker.restart()
  }

  /** refresh external_db table */
  def refreshTableView() {
    getExtDbasViews(UdsRepository.getAllExtDbs()).map { databases =>
      tableLines.clear()
      tableLines.addAll(ObservableBuffer(databases))
    }
  }

}