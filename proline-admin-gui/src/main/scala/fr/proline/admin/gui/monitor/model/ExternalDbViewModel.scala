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

import fr.proline.admin.gui.task.TaskRunner
import fr.proline.admin.gui.monitor.model.AdapterModel._
import fr.proline.admin.gui.monitor.database.ExternalsDB
import fr.proline.admin.gui.monitor.view.dialog.ChangeExtDbParamsDialog
import fr.proline.admin.gui.monitor.view.dialog.ChangeExtDbParamsDialog.Result

/**
 * The ExternalDb view model. Defines UI actions and database actions via ExternalDB.
 * @author aromdhani
 *
 */
class ExternalDbViewModel extends LazyLogging {

  var taskRunner: TaskRunner = _

  private val externalsDB = ExternalsDB

  val parentWindow: ObjectProperty[Window] = ObjectProperty[Window](null.asInstanceOf[Window])

  val items: ObservableBuffer[ExternalDb] = new ObservableBuffer[ExternalDb]()

  // Read-only collection of rows selected in the table view
  var _selectedItems: ObservableBuffer[ExternalDb] = _
  def selectedItems: ObservableBuffer[ExternalDb] = _selectedItems
  def selectedItems_=(v: ObservableBuffer[ExternalDb]): Unit = {
    _selectedItems = v
    _selectedItems.onChange {
      canRemoveRow.value = selectedItems.nonEmpty
    }
  }

  val canRemoveRow = BooleanProperty(false)

  /** Initialize the table view with users from UDS database */
  def onInitialize(): Unit = {
    items.clear()
    items ++= externalsDB.initialize()
  }

  /** Change external databases parameters */
  def onChange(): Unit = {
    val result = ChangeExtDbParamsDialog.showAndWait(Monitor.stage)
    result match {
      case Some(extDbParams) =>
        taskRunner.run(
          caption = s"Changing external database parameters",
          op = {
            // change external database parameters 
            logger.info(s"Changing external database parameters")
            externalsDB.change(Set(selectedItems.headOption.map(_.dbId.value).get), extDbParams.host, extDbParams.port)

            // Return items from database
            val updatedItems = externalsDB.queryExternalDbsAsView()
            // Update items on FX thread
            Platform.runLater {
              updateItems(updatedItems)
            }
          })
      case _ =>
    }
  }

  /** Upgrade all Proline databases */
  def onUpgradeAllDbs(): Unit = {

    taskRunner.run(
      caption = s"Upgrading all Proline databases",
      op = {
        logger.info("Upgrading all Proline databases. It could take a while. Please wait...")
        externalsDB.upgradeAllDbs()

        val updatedItems = externalsDB.queryExternalDbsAsView()
        // Update items on FX thread
        Platform.runLater {
          updateItems(updatedItems)
        }
      })

  }

  /** Check for updates */
  def onCheckForUpdates(): Unit = {
    taskRunner.run(
      caption = s"Checking for Proline updates",
      op = {
        logger.info("Checking for Proline updates. It could take a while. Please wait...")
        externalsDB.checkForUpdates()

        // Return items from database
        val updatedItems = externalsDB.queryExternalDbsAsView()
        // Update items on FX thread
        Platform.runLater {
          updateItems(updatedItems)
        }
      })
      
  }

  /** Delete obsolete databases */
  def onDeleteObsoleteDbs(): Unit = {
    taskRunner.run(
      caption = s"Deleting Obsolete databases",
      op = {
        logger.info("Deleting Obsolete databases. It could take a while. Please wait...")
        externalsDB.checkForUpdates()
        // Update items on FX thread
        val updatedItems = externalsDB.queryExternalDbsAsView()
        Platform.runLater {
          updateItems(updatedItems)
        }
      })
  }

  /** Refresh table view */
  def onRefresh(): Unit = {
    taskRunner.run(
      caption = "Refreshing the table view",
      op = {
        logger.info("Refreshing the table view")
        items.clear()
        items ++= externalsDB.initialize()
      })
  }

  /** update items in table view */
  private def updateItems(updatedItems: Seq[ExternalDb]): Unit = {
    val toAdd = updatedItems.diff(items)
    val toRemove = items.diff(updatedItems)
    items ++= toAdd
    items --= toRemove
  }

}