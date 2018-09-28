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
import fr.proline.admin.gui.wizard.util.UserGuide
import fr.proline.admin.gui.wizard.util.HelpPopup

import fr.proline.admin.gui.wizard.service.SetupDbs
import fr.proline.admin.gui.wizard.component.panel.bottom.InstallNavButtons
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource

import scalafx.scene.layout.Priority

/**
 *  Builds summary panel to summarize the new configurations.
 *  @author aromdhani
 */

object Summary extends LazyLogging {
  var setUpUpdateChBox: CheckBox = _
  def getItem(item: Item) {
    item match {
      case pgServer: PgServerConfig => {
        InstallNavButtons.summaryPanel.prolinePgServerPanel.children = new TitledBorderPane(
          title = "PostgreSQL Server Configuration ",
          contentNode = new VBox {
            spacing = 1
            children = Seq(
              new Label {
                text = s"""${Wizard.isPgOptimized match { case true => "PostgreSQL Server Configuration: Optimzed values." case false => "PostgreSQL Server Configuration: Default values." }}"""
              }, ScalaFxUtils.newVSpacer(1))
          })
      }
      case server: ServerConfig => {
        setUpUpdateChBox = new CheckBox {
          text = "Set up or update Proline databases"
          selected = true
        }
        val serverInfos = new Label {
          text = server.mountsPoint.getInfos
        }

        InstallNavButtons.summaryPanel.prolineServerPanel.children = new TitledBorderPane(
          title = "Proline Server Configuration",
          contentNode = new VBox {
            spacing = 1
            hgrow = Priority.ALWAYS
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
        val SeqReposArea = new VBox {
          spacing = 1
          children = Seq(
            new Label {
              text = seqRepos.postgreSQLSeqPanel.seqRepos.getInfos
            },
            new Label {
              text = seqRepos.parsingRules.getInfos
            },
            new Label {
              text = seqRepos.jmsServerPanel.getInfos
            }, ScalaFxUtils.newVSpacer(1))
        }
        InstallNavButtons.summaryPanel.prolineModulePanel.children = new TitledBorderPane(title = "Proline Sequence Repository ",
          contentNode = new VBox { children = Seq(SeqReposArea) })
      }
      case pwx: PwxConfig => {
        val prolineWebArea = new VBox {
          spacing = 1
          children = Seq(
            new Label {
              text = pwx.prolinePwx.getInfos
            }, ScalaFxUtils.newVSpacer(1))
        }
        InstallNavButtons.summaryPanel.prolineWebPanel.children = new TitledBorderPane(title = "Proline Web",
          contentNode = new VBox { children = Seq(prolineWebArea) })
      }
      case _ => logger.error("Error while trying to select an item! ")
    }
  }

  /** save item's form on Button validate */
  def saveItem(item: Item) {
    item match {
      case pgServerConfig: PgServerConfig => {
        try {
          pgServerConfig.pgHbaForm.saveForm()
          pgServerConfig.postgresForm.saveForm()
        } catch {
          case t: Throwable => {
            HelpPopup("Error", "An error has occured\nMake sure that you have administrator rights\nto edit PostgreSQL configurations files", Some(Wizard.stage), true)
            logger.error("Error while trying to save PostgreSQL properties.")
          }
        }
      }
      case server: ServerConfig => {
        try {
          server.postgres.server.saveForm()
          server.jmsServer.saveForm()
          server.mountsPoint.saveForm()
          if (setUpUpdateChBox.isSelected()) {
            val confirmed = GetConfirmation("Are you sure you want to setup and update Proline databases ?\n(This process may take hours.)", "Confirm your action", "Yes", "Cancel", Wizard.stage)
            if (confirmed) {
              SetupDbs(Wizard.stage).restart()
            }
          }
        } catch {
          case t: Throwable => logger.error("Error while trying to save Proline Server properties.")
        }
      }
      case seqRepos: SeqReposConfig => {
        try {
          seqRepos.postgreSQLSeqPanel.seqRepos.saveForm()
          seqRepos.jmsServerPanel.saveForm()
          seqRepos.parsingRules.saveForm()
        } catch {
          case t: Throwable => logger.error("Error while trying to save Proline Module properties.")
        }
      }
      case pwx: PwxConfig => {
        try {
          pwx.prolinePwx.saveForm()
        } catch {
          case t: Throwable => {
            logger.error("Error while trying to save PostgreSQL properties.")
          }
        }
      }
      case _ => {
        logger.error("Error while trying to save the new modifications! ")
      }

    }
  }
}



