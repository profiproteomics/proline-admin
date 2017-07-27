package fr.proline.admin.gui.wizard.component

import com.typesafe.scalalogging.LazyLogging

import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.scene.control.Hyperlink
import scalafx.scene.layout.VBox
import scalafx.scene.layout.HBox
import scalafx.scene.control.CheckBox
import scalafx.geometry.Pos
import scalafx.stage.Stage
import fr.proline.admin.gui.wizard.component.items._
import fr.profi.util.scalafx.TitledBorderPane
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.proline.admin.gui.Wizard
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.text.Text
import scalafx.scene.control.ProgressBar

import fr.profi.util.scalafx.ScalaFxUtils
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.wizard.util.PopupHelpWindow
import fr.proline.admin.gui.process._
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.gui.wizard.util.GetConfirmation
import fr.proline.admin.service.db.migration.UpgradeAllDatabases
import fr.proline.admin.gui.wizard.util.ProgressBarWindow
/**
 *  abstract class for all item's Panel
 */

abstract class Item extends VBox {
  val panelTitle: Label
  val headerHelpIcon: Hyperlink
  def _openHelpDialog()
}
/**
 * *
 * class to build summary panel in the end
 */

object SelectedItem extends LazyLogging {
  var setUpUpdateChBox, updateCheckBox: CheckBox = _
  val taskUpgrade: TaskUpgradeDatabases = new TaskUpgradeDatabases()
  def getItem(item: Item) {
    try {
      /* create Proline Server summary panel */
      if (item.isInstanceOf[ServerConfig]) {

        val server = item.asInstanceOf[ServerConfig]
        setUpUpdateChBox = new CheckBox {
          text = "Set up or update Proline databases"
          selected = false
        }
        NavigationButtonsPanel.summaryPanel.prolineServerBox.content = new TitledBorderPane(
          title = "Proline Server Configuration",
          contentNode = new VBox {
            spacing = 5
            content = Seq(
              new Text {
                text = server.postgres.getInfos
                style = "-fx-font-family: Arial Black"
              },
              new Text {
                text = server.mountsPoint.getInfos
              },
              new Text {
                text = server.jmsServer.getInfos
              }, ScalaFxUtils.newVSpacer(25), setUpUpdateChBox, ScalaFxUtils.newVSpacer(25))
          })
      }
      /* create Proline module summary panel */
      if (item.isInstanceOf[ModuleConfig]) {
        val module = item.asInstanceOf[ModuleConfig]
        NavigationButtonsPanel.summaryPanel.prolineModuleBox.content = new TitledBorderPane(
          title = "Proline Modules Configuration: Sequence Repository & Proline Web",
          contentNode = new VBox {
            spacing = 5
            content = Seq(
              new Text {
                text = module.PostGreSQLSeq.getInfos
              },
              new Text {
                text = module.parsingRules.getInfos
              },
              new Text {
                text = module.jmsServer.getInfos

              }, ScalaFxUtils.newVSpacer(25))
          })
      }
      /* create PostgreSQL summary panel */
      if (item.isInstanceOf[PgServerConfig]) {
        val pgServer = item.asInstanceOf[PgServerConfig]
        NavigationButtonsPanel.summaryPanel.prolinePgServerBox.content = new TitledBorderPane(
          title = "PostgreSQL Server Configuration ",
          contentNode = new VBox {
            content = Seq(
              new Text {
                text = s"""Access Right: OK """
              },
              new Text {
                text = s"""Optimization: OK """
              }, ScalaFxUtils.newVSpacer(25))
          })
      }
    } catch {
      case t: Throwable => logger.error("Error in selected item")
    }
  }

  /** save item's form on Button validate */
  def saveForms(item: Item) {
    try {
      /* save Proline server properties  */
      if (item.isInstanceOf[ServerConfig]) {
        val server = item.asInstanceOf[ServerConfig]
        try {
          server.postgres.saveForm()
          server.jmsServer.saveForm()
          server.mountsPoint.saveForm()
          if (setUpUpdateChBox.isSelected()) {
            val confirmed = GetConfirmation("Are you sure you want to update Proline databases ?\n(This process may take hours.)")
            if (confirmed) {
              ProgressBarWindow("Setup/Update", new TaskUpgradeDatabases(), Option(Wizard.stage))
            }
          }
        } catch {
          case t: Throwable => logger.error("Error while trying to save Proline Server properties.")
        }
      }
      /* save Proline module properties  */
      if (item.isInstanceOf[ModuleConfig]) {
        val module = item.asInstanceOf[ModuleConfig]
        try {
          module.PostGreSQLSeq.saveForm()
          module.jmsServer.saveForm()
          module.parsingRules.saveForm()
        } catch {
          case t: Throwable => logger.error("Error while trying to save Proline Module properties.")
        }
      }
      /* save PostgreSQL configurations  */
      if (item.isInstanceOf[PgServerConfig]) {
        val pgServerConfig = item.asInstanceOf[PgServerConfig]
        try {
          pgServerConfig.pgHbaForm.saveForm()
          pgServerConfig.postgresForm.saveForm()
        } catch {
          case t: Throwable => logger.error("Error while trying to save PostgreSQL properties.")
        }
      }
    } catch {
      case t: Throwable => logger.error("Error in selected item")
    }
  }
}


