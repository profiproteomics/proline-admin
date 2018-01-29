package fr.proline.admin.gui.wizard.component.panel.main

import com.typesafe.scalalogging.LazyLogging
import scalafx.scene.control.Label
import scalafx.scene.control.Hyperlink
import scalafx.scene.layout.VBox
import scalafx.scene.layout.HBox
import scalafx.scene.control.CheckBox
import scalafx.scene.control.TabPane
import fr.proline.admin.gui.wizard.component.items._
import fr.profi.util.scalafx.TitledBorderPane
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.proline.admin.gui.Wizard
import fr.profi.util.scalafx.ScalaFxUtils
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.process._
import fr.proline.admin.gui.wizard.util.GetConfirmation
import fr.proline.admin.gui.wizard.util.ProgressBarWindow
import fr.proline.admin.gui.wizard.util.UserGuideView
import java.io.File
import fr.proline.admin.gui.wizard.component.DbMaintenanceTask
import fr.proline.admin.gui.wizard.component.panel.bottom.InstallNavButtons
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.service.db.SetupProline

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

/**
 *  builds summary panel to summarize the new configurations
 *
 */

object Summary extends LazyLogging {

  var setUpUpdateChBox: CheckBox = _
  def getItem[T](item: T) {
    item match {
      case pgServer: PgServerConfig => {
        val pgServer = item.asInstanceOf[PgServerConfig]
        InstallNavButtons.summaryPanel.prolinePgServerBox.children = new TitledBorderPane(
          title = "PostgreSQL Server Configuration ",
          contentNode = new VBox {
            spacing = 1
            children = Seq(
              new Label {
                text = s"""${Wizard.isPgOptimized match { case true => "PostgreSQL Server Configuration: Optimzed values" case false => "PostgreSQL Server Configuration: Default values" }}"""
              }, ScalaFxUtils.newVSpacer(1))
          })
      }
      case server: ServerConfig => {
        val server = item.asInstanceOf[ServerConfig]
        setUpUpdateChBox = new CheckBox {
          text = "Set up or update Proline databases"
          selected = false
        }
        val serverInfos = new Label {
          text = server.mountsPoint.getInfos
        }
        serverInfos.prefHeight_=(100)
        InstallNavButtons.summaryPanel.prolineServerBox.children = new TitledBorderPane(
          title = "Proline Server Configuration",
          contentNode = new VBox {
            spacing = 1
            children = Seq(
              new Label {
                text = server.postgres.server.getInfos
              },
              serverInfos,
              new Label {
                text = server.jmsServer.getInfos
              }, ScalaFxUtils.newVSpacer(10), setUpUpdateChBox, ScalaFxUtils.newVSpacer(1))
          })

      }
      case seqRepos: SeqReposConfig => {
        val module = item.asInstanceOf[SeqReposConfig]
        val SeqReposArea = new VBox {
          spacing = 1
          children = Seq(
            new Label {
              text = module.PostGreSQLSeq.seqRepos.getInfos
            },
            new Label {
              text = module.parsingRules.getInfos
            },
            new Label {
              text = module.jmsServer.getInfos
            }, ScalaFxUtils.newVSpacer(1))
        }
        InstallNavButtons.summaryPanel.prolineModuleBox.children = new TitledBorderPane(title = "Proline Sequence Repository ",
          contentNode = new VBox { children = Seq(SeqReposArea) })
      }
      case pwx: PwxConfig => {
        val prolineWeb = item.asInstanceOf[PwxConfig]
        val prolineWebArea = new VBox {
          spacing = 1
          children = Seq(
            new Label {
              text = prolineWeb.prolinePwx.getInfos
            }, ScalaFxUtils.newVSpacer(1))
        }
        InstallNavButtons.summaryPanel.prolineWebBox.children = new TitledBorderPane(title = "Proline Web",
          contentNode = new VBox { children = Seq(prolineWebArea) })

      }
      case _ => logger.error("Error while trying to select an item! ")
    }
  }

  /** save item's form on Button validate */
  def saveItem[T](item: T) {
    item match {
      case pgServerConfig: PgServerConfig => {
        val pgServerConfig = item.asInstanceOf[PgServerConfig]
        try {
          pgServerConfig.pgHbaForm.saveForm()
          pgServerConfig.postgresForm.saveForm()
        } catch {
          case t: Throwable => logger.error("Error while trying to save PostgreSQL properties.")
        }
      }
      case server: ServerConfig => {
        val server = item.asInstanceOf[ServerConfig]
        try {
          server.postgres.server.saveForm()
          server.jmsServer.saveForm()
          server.mountsPoint.saveForm()
          if (setUpUpdateChBox.isSelected()) {
            val confirmed = GetConfirmation("Are you sure you want to update Proline databases ?\n(This process may take hours.)")
            if (confirmed) {
              ProgressBarWindow("Setup/Update", "Setup / Update Proline databases\n  \t\t in progress...", Some(Wizard.stage), false, DbMaintenanceTask.Worker)
            }
          }
        } catch {
          case t: Throwable => logger.error("Error while trying to save Proline Server properties.")
        }
      }
      case seqRepos: SeqReposConfig => {
        val module = item.asInstanceOf[SeqReposConfig]
        try {
          module.PostGreSQLSeq.seqRepos.saveForm()
          module.jmsServer.saveForm()
          module.parsingRules.saveForm()
        } catch {
          case t: Throwable => logger.error("Error while trying to save Proline Module properties.")
        }
      }

      case pwx: PwxConfig => {
        try {
          val prolineWeb = item.asInstanceOf[PwxConfig]
          prolineWeb.prolinePwx.saveForm()
        } catch {
          case t: Throwable => logger.error("Error while trying to save PostgreSQL properties.")
        }
      }
      case _ => logger.error("Error while trying to save the new modifications! ")
    }
  }
}



