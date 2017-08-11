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
import scala.collection.mutable.ListBuffer
import fr.proline.admin.gui.wizard.component.items._
import fr.profi.util.scalafx.TitledBorderPane
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.proline.admin.gui.Wizard
import fr.profi.util.scalafx.{ ScalaFxUtils, BoldLabel }
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

  var setUpUpdateChBox: CheckBox = _
  val taskUpgrade: TaskUpgradeDatabases = new TaskUpgradeDatabases()

  def get(item: Item) {
    if (item.isInstanceOf[PgServerConfig]) {
      val pgServer = item.asInstanceOf[PgServerConfig]
      NavigationButtonsPanel.summaryPanel.prolinePgServerBox.content = new TitledBorderPane(
        title = "PostgreSQL Server Configuration ",
        contentNode = new VBox {
          spacing = 1
          content = Seq(
            new Label {
              text = s"""Access Right: OK """
            },
            new Label {
              text = s"""Optimization: OK """
            }, ScalaFxUtils.newVSpacer(1))
        })
    }

    /* Proline Server summary area */
    if (item.isInstanceOf[ServerConfig]) {

      val server = item.asInstanceOf[ServerConfig]
      setUpUpdateChBox = new CheckBox {
        text = "Set up or update Proline databases"
        selected = false
      }
      NavigationButtonsPanel.summaryPanel.prolineServerBox.content = new TitledBorderPane(
        title = "Proline Server Configuration",
        contentNode = new VBox {
          spacing = 1
          content = Seq(
            new Label {
              text = server.postgres.getInfos
            },
            new Label {
              text = server.mountsPoint.getInfos
            },
            new Label {
              text = server.jmsServer.getInfos
            }, ScalaFxUtils.newVSpacer(10), setUpUpdateChBox, ScalaFxUtils.newVSpacer(1))
        })
    }

    /* Proline module summary area  */
    if (item.isInstanceOf[ModuleConfig]) {
      val module = item.asInstanceOf[ModuleConfig]
      val SeqReposArea = new VBox {
        spacing = 1
        content = Seq(
          new Label {
            text = module.PostGreSQLSeq.getInfos
          },
          new Label {
            text = module.parsingRules.getInfos
          },
          new Label {
            text = module.jmsServer.getInfos
          }, ScalaFxUtils.newVSpacer(1))
      }
      NavigationButtonsPanel.summaryPanel.prolineModuleBox.content = new TitledBorderPane(title = "Proline Sequence Repository ",
        contentNode = new VBox { content = Seq(SeqReposArea) })
    }
    /* Proline web summary area */
    if (item.isInstanceOf[ProlineWebConfig]) {
      val prolineWeb = item.asInstanceOf[ProlineWebConfig]
      val prolineWebArea = new VBox {
        spacing = 1
        content = Seq(
          new Label {
            text = prolineWeb.prolinePwx.getInfos
          }, ScalaFxUtils.newVSpacer(1))
      }
      NavigationButtonsPanel.summaryPanel.prolineWebBox.content = new TitledBorderPane(title = "Proline Web",
        contentNode = new VBox { content = Seq(prolineWebArea) })
    }
  }

  /** save item's form on Button validate */
  def saveAll(item: Item) {
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
    if (item.isInstanceOf[PgServerConfig]) {
      val pgServerConfig = item.asInstanceOf[PgServerConfig]
      try {
        pgServerConfig.pgHbaForm.saveForm()
        pgServerConfig.postgresForm.saveForm()
      } catch {
        case t: Throwable => logger.error("Error while trying to save PostgreSQL properties.")
      }
    }
    if (item.isInstanceOf[ProlineWebConfig]) {
      try {
        val prolineWeb = item.asInstanceOf[ProlineWebConfig]
        prolineWeb.prolinePwx.saveForm()
      } catch {
        case t: Throwable => logger.error("Error while trying to save PostgreSQL properties.")
      }
    }
  }
}


