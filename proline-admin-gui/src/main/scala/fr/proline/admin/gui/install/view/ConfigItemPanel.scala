package fr.proline.admin.gui.install.view

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.Node
import scalafx.scene.control.Tab
import scalafx.scene.control.TabPane
import scalafx.scene.control.Label
import scalafx.scene.control.CheckBox
import scalafx.scene.layout.{ Priority, VBox, HBox }
import scalafx.beans.property.IntegerProperty

import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.Install
import fr.proline.admin.gui.install.model._
import fr.proline.admin.gui.install.view.jms._
import fr.proline.admin.gui.install.view.server._
import fr.proline.admin.gui.install.view.postgres._
import fr.proline.admin.gui.install.view.modules._
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.process.ProlineAdminConnection
import fr.proline.admin.gui.util.GetConfirmation
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.service.db.migration.UpgradeAllDatabases
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.util.ShowPopupWindow
import fr.profi.util.scalafx.TitledBorderPane
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle
import fr.profi.util.scalafx.BoldLabel

import scala.collection.Seq
import scala.collection.SortedMap

/**
 *  All configuration items must be defined here
 *  @author aromdhani
 *
 */

sealed trait ConfigItemPanel extends LazyLogging {
  // Id used to sort, remove and add a configuration item
  def id: IntegerProperty
  def apply(): Node
  def save(): Unit
}

/**
 * Creates and displays PostgreSQL server configuration panel:
 * tabPane of postgresql.conf parameters and tabPane of pg_hba.conf parameters.
 *
 */

object PgServerConfigPanel extends ConfigItemPanel {
  var components: Option[(PostgresModelView, PostgresConfigPanel, PgHbaConfigPanel)] = None
  def id = IntegerProperty(1)
  /** Create postgreSQL server configuration panel */
  override def apply(): VBox = {
    // Create title panel
    val titlePane = new HBox {
      spacing = 15
      children = Seq(FxUtils.newImageView(IconResource.SETTING), new VBox {
        padding = Insets(0, 0, 10, 0)
        children = Seq(new Label {
          text = "PostgreSQL Server Configuration"
          styleClass = List("item")
        })
      })
    }
    val postgresModelView = new PostgresModelView(Install.pgDataDirPath)

    // Create PostgreSQL Optimization TabPane
    val postgresConfigPanel = new PostgresConfigPanel(postgresModelView)(Install.stage)
    val pgOptimtabPane =
      new Tab {
        text = "PostgreSQL Optimization"
        closable_=(false)
        //margin = Insets(5, 5, 5, 5)
        content_=(postgresConfigPanel)
      }

    // Create PostgreSQL Network Access TabPane
    val pgHbaConfigPanel = new PgHbaConfigPanel(postgresModelView)(Install.stage)
    val pgNetAccessTabPane =
      new Tab {
        text = "PostgreSQL Network Access"
        closable_=(false)
        // margin = Insets(5, 5, 5, 5)
        content_=(pgHbaConfigPanel)
      }
    // Update
    components = Option((postgresModelView, postgresConfigPanel, pgHbaConfigPanel))
    val tabPane = new TabPane {
      vgrow = Priority.ALWAYS
    }
    tabPane.tabs.addAll(pgOptimtabPane, pgNetAccessTabPane)
    new VBox {
      spacing = 1
      children = Seq(titlePane, tabPane)
    }
  }

  /** Save PostgreSQL new configurations */
  def save(): Unit = {
    components match {
      case Some((postgresModelView, postgresConfigPanel, pgHbaConfigPanel)) => {
        try {
          postgresConfigPanel.saveForm()
          pgHbaConfigPanel.saveForm()
        } catch {
          case ex: Exception => {
            logger.error("Error while trying to update PostgreSQL server configuration files. Make sure that you have administrator rights to edit configuration files", ex.printStackTrace())
            val exOpt = Option(ex)
            ShowPopupWindow(
              new Label(s"Error while trying to update PostgreSQL server configuration files. Make sure that you have administrator rights to edit configuration files.\n${exOpt.map("Exception: " + _.getClass).getOrElse("")}\n"
                + s"${exOpt.map(_.getMessage).getOrElse("")}"),
              "Error",
              Some(Install.stage),
              false)
          }
        }
      }
      case _ => logger.error("Error while trying to update PostgreSQL server properties!")
    }
  }
}

/**
 * Creates and displays server configuration panel: tabPane of PostgreSQL parameters, JMS server parameters and mount points.
 *
 */

object ServerConfigPanel extends ConfigItemPanel {

  var components: Option[(AdminModelView, ServerPgPanel, JmsModelView, ServerJmsPanel, ServerMountPointsPanel)] = None
  override def id = IntegerProperty(2)
  /** Create server configuration panel */
  override def apply(): VBox = {
    // Create title panel
    val titlePane = new HBox {
      spacing = 15
      children = Seq(FxUtils.newImageView(IconResource.SETTING), new VBox {
        padding = Insets(0, 0, 10, 0)
        children = Seq(new Label {
          text = "Proline Server Configuration"
          styleClass = List("item")
        })
      })
    }

    // Create PostgreSQL TabPane
    val adminModelView = new AdminModelView(Install.serverConfPath)
    val serverPgView = new ServerPgPanel(adminModelView)
    val postgresTabPane =
      new Tab {
        text = "PostgreSQL"
        closable_=(false)
        content_=(serverPgView)
      }

    // Create JMS server TabPane
    val jmsModelView = new JmsModelView(Install.serverJmsPath)
    val serverJmsView = new ServerJmsPanel(jmsModelView)
    val jmsServerTabPane =
      new Tab {
        text = "JMS Server"
        closable_=(false)
        content_=(serverJmsView)
      }
    // Create mount points TabPane
    val mountPointsView = new ServerMountPointsPanel(adminModelView)
    val mountPointsTabPane =
      new Tab {
        text = "Mount Points"
        closable_=(false)
        content_=(mountPointsView)
      }
    components = Option((adminModelView, serverPgView, jmsModelView, serverJmsView, mountPointsView))
    val tabPane = new TabPane {
      vgrow = Priority.ALWAYS
    }
    tabPane.tabs.addAll(postgresTabPane, jmsServerTabPane, mountPointsTabPane)
    new VBox {
      spacing = 1
      children = Seq(titlePane, tabPane)
    }
  }

  /** Save Proline server new configurations */
  def save(): Unit = {
    components match {
      case Some((adminModelView, serverPgView, jmsModelView, serverJmsView, mountPointsView)) => {
        adminModelView.onSaveAdminConfig(serverPgView.toAdminConfig())
        adminModelView.onSaveServerConfig(serverPgView.toAdminConfig(), mountPointsView.toServerConfig())
        jmsModelView.onSaveJmsConfig(serverJmsView.toConfig())
      }
      case _ => logger.error("Error while trying to update server properties")
    }
  }
}

/**
 * Creates and displays PWX configuration panel: tabPane of PostgreSQL parameters and mount points.
 *
 */

object PWXConfigPanel extends ConfigItemPanel {

  var components: Option[(PwxModelView, PwxPgPanel, PwxMountPointsPanel)] = None
  def id = IntegerProperty(3)
  /** Create PWX configuration panel */
  override def apply(): VBox = {
    // Create title panel
    val titlePane = new HBox {
      spacing = 15
      children = Seq(FxUtils.newImageView(IconResource.SETTING), new VBox {
        padding = Insets(0, 0, 10, 0)
        children = Seq(new Label {
          text = "Proline Web Extension"
          styleClass = List("item")
        })
      })
    }
    // Create PostgreSQL TabPane
    val pwxModelView = new PwxModelView(Install.pwxConfPath)
    val serverPgView = new PwxPgPanel(pwxModelView)
    val postgresTabPane =
      new Tab {
        text = "PostgreSQL"
        closable_=(false)
        content_=(serverPgView)
      }
    // Create mount points TabPane
    val mountPointsView = new PwxMountPointsPanel(pwxModelView)
    val mountPointsTabPane =
      new Tab {
        text = "Mount Points"
        closable_=(false)
        content_=(mountPointsView)
      }
    components = Option((pwxModelView, serverPgView, mountPointsView))
    val tabPane = new TabPane {
      vgrow = Priority.ALWAYS
    }
    tabPane.tabs.addAll(postgresTabPane, mountPointsTabPane)
    new VBox {
      spacing = 1
      children = Seq(titlePane, tabPane)
    }
  }

  /** Save PWX new configurations */
  def save(): Unit = {
    components match {
      case Some((pwxModelView, serverPgView, mountPointsView)) =>
        {
          pwxModelView.onSavePwxConfig(serverPgView.toSimpleConfig(), mountPointsView.toServerConfig())
        }
      case _ => logger.error("Error while trying to update Proline Web Extension configuration!")
    }
  }
}

/**
 *  Creates and displays sequence repository configuration panel: tabPane of PostgreSQL parameters, JMS server parameters and parsing rules.
 *
 */

object SeqReposConfigPanel extends ConfigItemPanel {

  var components: Option[(SeqReposModelView, SeqReposPgPanel, JmsModelView, ServerJmsPanel, SeqReposParsingRulesPanel)] = None
  def id = IntegerProperty(4)
  /** Create sequence repository configuration panel */
  override def apply(): VBox = {
    // Create title panel
    val titlePane = new HBox {
      spacing = 15
      children = Seq(FxUtils.newImageView(IconResource.SETTING), new VBox {
        padding = Insets(0, 0, 10, 0)
        children = Seq(new Label {
          text = "Sequence Repository Configuration"
          styleClass = List("item")
        })
      })
    }
    // Create PostgreSQL TabPane
    val seqReposModelView = new SeqReposModelView(Install.seqReposConfPath, Install.seqReposParsigRulesPath)
    val seqReposPgView = new SeqReposPgPanel(seqReposModelView)
    val postgresTabPane =
      new Tab {
        text = "PostgreSQL"
        closable_=(false)
        content_=(seqReposPgView)
      }
    // Create JMS server TabPane
    val jmsModelView = new JmsModelView(Install.seqReposJmsPath)
    val serverJmsView = new ServerJmsPanel(jmsModelView)
    val jmsServerTabPane =
      new Tab {
        text = "JMS Server"
        closable_=(false)
        content_=(serverJmsView)
      }
    // Create parsing rules TabPane
    val parsingRulesView = new SeqReposParsingRulesPanel(seqReposModelView, Install.stage)
    val parsingRulesTabPane =
      new Tab {
        text = "Parsing Rules"
        closable_=(false)
        content_=(parsingRulesView)
      }
    components = Option((seqReposModelView, seqReposPgView, jmsModelView, serverJmsView, parsingRulesView))
    val tabPane = new TabPane {
      vgrow = Priority.ALWAYS
    }
    tabPane.tabs.addAll(postgresTabPane, jmsServerTabPane, parsingRulesTabPane)
    new VBox {
      spacing = 1
      children = Seq(titlePane, tabPane)
    }
  }

  /** Save the new configurations */
  def save(): Unit = {
    components match {
      case Some((seqReposModelView, seqReposPgView, jmsModelView, serverJmsView, parsingRulesView)) => {
        seqReposModelView.onSaveConfig(seqReposPgView.toSeqConfig())
        seqReposModelView.onSaveParsingRules(parsingRulesView.toParsingRule())
        jmsModelView.onSaveJmsConfig(serverJmsView.toConfig())
      }
      case _ => logger.error("Error while trying to update sequence repository properties")
    }
  }
}

/**
 *  Creates and displays summary panel.
 *
 */

object SummaryConfigPanel extends ConfigItemPanel {
  def id = IntegerProperty(5)
  var upgradeDbsChBoxOpt: Option[CheckBox] = None
  var pgServerResultPanel = new VBox {
    managed <== visible
  }
  var serverResultPanel = new VBox {
    managed <== visible
  }
  var pwxResultPanel = new VBox {
    managed <== visible
  }
  var seqReposResultPanel = new VBox {
    managed <== visible
  }
  /** Create summary panel */
  override def apply(): VBox = {
    // Create title panel
    val titlePane = new HBox {
      spacing = 15
      children = Seq(FxUtils.newImageView(IconResource.BIGINFOS), new VBox {
        padding = Insets(0, 0, 10, 0)
        children = Seq(new Label {
          text = "Summary Configuration"
          styleClass = List("item")
        })
      })
    }
    // Create summary content
    val subTitle = new BoldLabel("Please review your selected configurations", upperCase = false)
    new VBox {
      prefHeight = 800
      spacing = 25
      children = Seq(
        titlePane,
        subTitle,
        pgServerResultPanel,
        serverResultPanel,
        seqReposResultPanel,
        pwxResultPanel)
    }
  }

  /** Set a summary  for the selected configurations items */
  def sum(): Unit = {
    ConfigItemPanel.configItemMap.values.toList.foreach {
      // PostgreSQL server properties
      case PgServerConfigPanel => {
        PgServerConfigPanel.components match {
          case Some((postgresModelView, postgresConfigPanel, pgHbaConfigPanel)) => {
            pgServerResultPanel.children = new TitledBorderPane(
              title = "PostgreSQL Server Configuration ",
              contentNode = new VBox {
                spacing = 1
                hgrow = Priority.ALWAYS
                vgrow = Priority.ALWAYS
                children = Seq(
                  new HBox {
                    children = Seq(new BoldLabel("PostgreSQL Server Configuration:\t", upperCase = false),
                      new Label {
                        text = s"${if (postgresConfigPanel.isOptimized) "Optimized values" else "Default values"}"
                        style = TextStyle.BLUE_ITALIC
                      })
                  }, ScalaFxUtils.newVSpacer(1))
              })
          }
          case _ =>
        }
      }

      // Proline server configuration summary  
      case ServerConfigPanel => {
        ServerConfigPanel.components match {
          case Some((adminModelView, serverPgView, jmsModelView, serverJmsView, mountPointsView)) => {
            upgradeDbsChBoxOpt = Some(new CheckBox {
              text = "Set up or upgrade all Proline databases"
              selected = true
            })
            serverResultPanel.children = new TitledBorderPane(
              title = "Proline Server Configuration",
              contentNode = new VBox {
                spacing = 1
                hgrow = Priority.ALWAYS
                vgrow = Priority.ALWAYS
                children = Seq(
                  new HBox {
                    children = Seq(new BoldLabel("PostgreSQL:\t", upperCase = false),
                      new Label {
                        text = s"""${if (serverPgView.onTestDbConn()) "OK" else "NOK"}"""
                        style = if (serverPgView.onTestDbConn()) TextStyle.BLUE_ITALIC else TextStyle.RED_ITALIC
                      })
                  },
                  new HBox {
                    children = Seq(new BoldLabel("JMS Server Type:\t", upperCase = false),
                      new Label {
                        text = s"""${if (serverJmsView.getType) "Embedded" else "Specific"}"""
                        style = TextStyle.BLUE_ITALIC
                      })
                  },
                  new HBox {
                    children = Seq(new BoldLabel("Mount Points:\t", upperCase = false),
                      new Label {
                        text = s"""${mountPointsView.getProperties()}"""
                        style = TextStyle.BLUE_ITALIC
                      })
                  },
                  ScalaFxUtils.newVSpacer(10),
                  upgradeDbsChBoxOpt.get,
                  ScalaFxUtils.newVSpacer(1))
              })
          }
          case _ =>
        }
      }

      // Proline Web Extension properties 
      case PWXConfigPanel => {
        PWXConfigPanel.components match {
          case Some((pwxModelView, serverPgView, mountPointsView)) => {
            pwxResultPanel.children = new TitledBorderPane(
              title = "Proline Web Extension Configuration",
              contentNode = new VBox {
                spacing = 1
                hgrow = Priority.ALWAYS
                children = Seq(
                  new HBox {
                    children = Seq(new BoldLabel("PostgreSQL:\t", upperCase = false),
                      new Label {
                        text = s"""${if (serverPgView.onTestDbConn()) "OK" else "NOK"}"""
                        style = if (serverPgView.onTestDbConn()) TextStyle.BLUE_ITALIC else TextStyle.RED_ITALIC
                      })
                  },
                  ScalaFxUtils.newVSpacer(1))
              })
          }
          case _ =>
        }
      }
      // Sequence repository properties 
      case SeqReposConfigPanel => {
        SeqReposConfigPanel.components match {
          case Some((seqReposModelView, seqReposPgView, jmsModelView, serverJmsView, parsingRulesView)) => {
            seqReposResultPanel.children = new TitledBorderPane(
              title = "Sequence Repository Configuration",
              contentNode = new VBox {
                spacing = 1
                hgrow = Priority.ALWAYS
                vgrow = Priority.ALWAYS
                children = Seq(
                  new HBox {
                    children = Seq(new BoldLabel("PostgreSQL:\t", upperCase = false),
                      new Label {
                        text = s"""${if (seqReposPgView.onTestDbConn()) "OK" else "NOK"}"""
                        style = if (seqReposPgView.onTestDbConn()) TextStyle.BLUE_ITALIC else TextStyle.RED_ITALIC
                      })
                  },
                  new HBox {
                    children = Seq(new BoldLabel("JMS Server Type:\t", upperCase = false),
                      new Label {
                        text = s"""${if (serverJmsView.getType) "Embedded" else "Specific"}"""
                        style = TextStyle.BLUE_ITALIC
                      })
                  },
                  new HBox {
                    children = Seq(new BoldLabel("Parsing Rules:  ", upperCase = false),
                      new Label {
                        text = s"""${parsingRulesView.getProperties()}"""
                        style = TextStyle.BLUE_ITALIC
                      })
                  },
                  ScalaFxUtils.newVSpacer(1))
              })
          }
          case _ =>
        }
      }
      case _ =>
    }
  }

  /** Save the new configurations */
  def save(): Unit = {
    upgradeDbsChBoxOpt.foreach { upgradeDbsChBox =>
      if (upgradeDbsChBox.isSelected) {
        val confirm = GetConfirmation(s"Are you sure that you want to set up and upgrade all Proline databases to the last version. This action can take a while.", "Confirm your action", " Yes ", "Cancel", Install.stage)
        if (confirm) {
          ProlineAdminConnection.setNewProlineInstallConfig(Install.adminConfPath)
          val prolineIsSetUp = UdsRepository.isUdsDbReachable()

          if (!prolineIsSetUp) {
            // Setup Proline databases task
            Install.taskRunner.run("Setup Proline databases",
              {
                new SetupProline(SetupProline.getUpdatedConfig(), UdsRepository.getUdsDbConnector()).run()
              },
              true,
              Some(Install.stage))
          } else {
            // Upgrade all Proline databases task
            Install.taskRunner.run("Upgrading Proline databases",
              {
                val dsConnectorFactory = UdsRepository.getDataStoreConnFactory()
                val upgradeDbsTask = new UpgradeAllDatabases(dsConnectorFactory)
                upgradeDbsTask.doWork()
                upgradeDbsTask.failedDbSet
              },
              true,
              Some(Install.stage))
          }
        }
      }
    }
  }
}

object ConfigItemPanel {

  var configItemMap: SortedMap[Int, ConfigItemPanel] = SortedMap(5 -> SummaryConfigPanel)

  /** Add node to the selected items */
  def add(node: ConfigItemPanel) {
    configItemMap += (node.id.value -> node)
  }

  /** Remove node from the selected items */
  def remove(id: Int) {
    configItemMap -= id
  }
}