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
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.text.Text
import scalafx.scene.control.ProgressBar

import fr.proline.admin.gui.wizard.component.items._
import fr.profi.util.scalafx.TitledBorderPane
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.proline.admin.gui.Wizard
import fr.profi.util.scalafx.ScalaFxUtils
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.wizard.util.PopupHelpWindow
import fr.proline.admin.gui.process._
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.gui.wizard.util.GetConfirmation
import fr.proline.admin.service.db.migration.UpgradeAllDatabases
import fr.proline.admin.gui.wizard.util.ProgressBarWindow

/**
 *  Item  build  item's Panel
 *
 */

trait Item extends VBox {
  val panelTitle: Label
  val headerHelpIcon: Hyperlink
  def _openHelpDialog()
}

/**
 *  Object build summary panel
 *
 */

object SelectedItem extends LazyLogging {
  var setUpUpdateChBox, updateCheckBox: CheckBox = _
  val taskUpgrade: TaskUpgradeDatabases = new TaskUpgradeDatabases()
  def get(item: Item) {
    try {
      if (item.isInstanceOf[PgServerConfig]) {
        val pgServer = item.asInstanceOf[PgServerConfig]
        NavigationButtonsPanel.summaryPanel.prolinePgServerBox.content = new TitledBorderPane(
          title = "PostgreSQL Server Configuration ",
          contentNode = new VBox {
            content = Seq(
              new Label {
                text = s"""Access Right: OK """
                styleClass = List("summaryLabel")
              },
              new Label {
                text = s"""Optimization: OK """
                styleClass = List("summaryLabel")
              }, ScalaFxUtils.newVSpacer(25))
          })
      }
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
              new Label {
                text = server.postgres.getInfos
                styleClass = List("summaryLabel")
              },
              new Label {
                text = server.mountsPoint.getInfos
                styleClass = List("summaryLabel")
              },
              new Label {
                text = server.jmsServer.getInfos
                styleClass = List("summaryLabel")
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
              new Label {
                text = module.PostGreSQLSeq.getInfos
                styleClass = List("summaryLabel")
              },
              new Label {
                text = module.parsingRules.getInfos
                styleClass = List("summaryLabel")
              },
              new Label {
                text = module.jmsServer.getInfos
                styleClass = List("summaryLabel")

              }, ScalaFxUtils.newVSpacer(25))
          })
      }
    } catch {
      case t: Throwable => logger.error("Error in selected item")
    }
  }

  /** save item's form on Button validate */
  def saveAll(item: Item) {
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

      /* save Proline Module properties  */
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


