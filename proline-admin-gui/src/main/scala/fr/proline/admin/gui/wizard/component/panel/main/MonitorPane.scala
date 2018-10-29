package fr.proline.admin.gui.wizard.component.panel.main

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.geometry.Pos
import scalafx.scene.layout.{ VBox, HBox, Priority }
import scalafx.scene.control.{ Label, TextField, Hyperlink }
import scalafx.scene.text.{ Font, FontWeight }
import fr.proline.admin.gui.process.config.AdminConfig
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.proline.admin.gui.wizard.process.config.{ NodeConfig, ParsingRule }
import fr.proline.admin.gui.wizard.process.config.{ NodeConfigFile, ParsingRulesFile }
import fr.proline.admin.gui.Monitor

import fr.proline.admin.gui.process.DatabaseConnection
import fr.proline.admin.gui.process.{ UdsRepository, ProlineAdminConnection }
import fr.proline.admin.gui.wizard.util.UserGuide
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.wizard.util.WindowSize
import fr.proline.admin.gui.util.FxUtils
import fr.profi.util.scalafx.{ BoldLabel, TitledBorderPane }
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.ScalaFxUtils
import java.io.File

/**
 * Builds home panel of Proline-Admin GUI monitor .
 *
 */

object MonitorPane extends VBox with LazyLogging {

  // get Proline-Admin GUI initial settings  
  val adminConfigOpt = getAdminConfigOpt()
  require(adminConfigOpt.isDefined, "Proline Admin-config must not be empty or null.")
  val adminConfig = adminConfigOpt.get
  val nodeConfigOpt = getNodeConfigOpt()
  //load initial proline configyurations 
  setInitialConfig()
  // proline error and warning labels 
  val adminConfigErrorLabel = new Label {
    text = "The Proline-Admin configuration file not found. Make sure that you have already setup Proline."
    graphic = ScalaFxUtils.newImageView(IconResource.CANCEL)
    style = TextStyle.RED_ITALIC
    managed <== visible
  }
  val udsDbErrorLabel = new Label {
    text = "Error can not reach UDS database. Make sure that you have already setup Proline."
    graphic = ScalaFxUtils.newImageView(IconResource.CANCEL)
    style = TextStyle.RED_ITALIC
    managed <== visible
  }
  val connectionErrorLabel = new Label {
    text = "Error establishing a database connection. Please check the database connection configurations."
    graphic = ScalaFxUtils.newImageView(IconResource.CANCEL)
    style = TextStyle.RED_ITALIC
    managed <== visible
  }
  val serverConfigWarningLabel = new Label {
    text = "The path of Proline server and jms-node configuration files not found. Make sure that you have already setup Proline."
    graphic = ScalaFxUtils.newImageView(IconResource.WARNING)
    style = TextStyle.ORANGE_ITALIC
    managed <== visible
  }
  val seqReposWarningLabel = new Label {
    text = "The path of the sequence repository configuration file not found. Make sure that you have already setup Proline."
    graphic = ScalaFxUtils.newImageView(IconResource.WARNING)
    style = TextStyle.ORANGE_ITALIC
    managed <== visible
  }
  val pgsqlDataDirWarningLabel = new Label {
    text = "The path of the Proline data directory not found. Make sure that you have already setup Proline."
    graphic = ScalaFxUtils.newImageView(IconResource.WARNING)
    style = TextStyle.ORANGE_ITALIC
    managed <== visible
  }

  //JMS server initial properties 
  val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    alignmentInParent = Pos.BASELINE_RIGHT
    onAction = handle {
      openAdminGuide()
    }
  }
  val infoMessage = new BoldLabel("(By default, same server as Proline Server Cortex)")
  private val V_SPACING = 10
  private val H_SPACING = 5

  // jms components
  val jmsServelLabel = new HBox {
    vgrow = Priority.Always
    children = List(new Label {
      text = "JMS Server"
      font = Font.font("SanSerif", FontWeight.Bold, 12)
    })
  }

  val jmsHostLabel = new Label("Host: ")
  val jmsHostField = new TextField() {
    disable = true
    text = nodeConfigOpt.map(_.jmsServerHost.getOrElse("localhost")).getOrElse("localhost")
  }

  val jmsPortLabel = new Label("Port: ")
  val jmsPortField = new TextField() {
    disable = true
    text = nodeConfigOpt.map(_.jmsServePort.getOrElse(5442).toString()).getOrElse(5442).toString()
  }

  val jmsProlineQueueLabel = new Label("Proline Queue Name:")
  val jmsProlineQueueField = new TextField {
    disable = true
    text = nodeConfigOpt.map(_.requestQueueName.getOrElse("ProlineServiceRequestQueue")).getOrElse("ProlineServiceRequestQueue")
  }
  val jmsTiteledPane = new HBox {
    children = new TitledBorderPane(
      title = "",
      titleTooltip = "JMS Server properties",
      contentNode = new VBox {
        prefWidth = (WindowSize.prefWitdh)
        spacing = V_SPACING * 2
        children = List(infoMessage, new HBox {
          spacing = H_SPACING * 3
          children = List(jmsHostLabel, jmsHostField)
        }, new HBox {
          spacing = H_SPACING * 3
          children = List(jmsPortLabel, jmsPortField)
        },
          new HBox {
            spacing = H_SPACING * 3
            children = List(jmsProlineQueueLabel, jmsProlineQueueField)
          })
      })
  }
  val jmsServerPane = new HBox {
    spacing = H_SPACING * 2
    vgrow = Priority.Always
    hgrow = Priority.Always
    fillWidth = true
    children = List(jmsServelLabel, jmsTiteledPane)
  }

  // pgServer components 
  val pgSQLServerLabel = new HBox {
    vgrow = Priority.Always
    children = List(new Label {
      text = "PostgreSQL Server"
      font = Font.font("SanSerif", FontWeight.Bold, 12)
    })
  }

  val pgHostLabel = new Label("Host: ")
  val pgHostField = new TextField() {
    disable = true
    text = adminConfig.dbHost.getOrElse("<db_host>")
  }

  val pgPortLabel = new Label("Port: ")
  val pgPortField = new TextField() {
    disable = true
    text = adminConfig.dbPort.getOrElse(5432).toString()
  }

  val pgUserLabel = new Label("User: ")
  val pgUserField = new TextField() {
    disable = true
    text = adminConfig.dbUserName.getOrElse("<db_user>")
  }

  val pgPasswordLabel = new Label("Password: ")
  val pgPasswordField = new TextField() {
    disable = true
    text = adminConfig.dbPassword.getOrElse("<db_password>")
  }
  val pgTitledPane = new HBox {

    children = Seq(new TitledBorderPane(
      title = "",
      titleTooltip = "PostgreSQL Server properties",
      contentNode = new VBox {
        prefWidth = (WindowSize.prefWitdh)
        spacing = V_SPACING * 2
        children = List(new HBox {
          spacing = H_SPACING * 3
          children = List(pgHostLabel, pgHostField)
        }, new HBox {
          spacing = H_SPACING * 3
          children = List(pgPortLabel, pgPortField)
        },
          new HBox {
            spacing = H_SPACING * 3
            children = List(pgUserLabel, pgUserField)
          }, new HBox {
            spacing = H_SPACING * 3
            children = List(pgPasswordLabel, pgPasswordField)
          })
      }))
  }

  val postgreSQLServerPane = new HBox {
    spacing = H_SPACING * 2
    fillWidth = true
    children = List(pgSQLServerLabel, pgTitledPane)
  }

  Seq(pgSQLServerLabel,
    jmsServelLabel,
    pgPortLabel,
    pgHostLabel,
    pgUserLabel,
    pgPasswordLabel,
    jmsHostLabel,
    jmsPortLabel,
    jmsProlineQueueLabel).foreach(_.minWidth = 150)
  Seq(pgPortField, pgUserField, pgPasswordField, pgHostField, jmsHostField, jmsProlineQueueField, jmsPortField).foreach {
    f => f.hgrow = Priority.Always
  }

  val helpPane = new HBox {
    children = Seq(ScalaFxUtils.newHSpacer(minW = postgreSQLServerPane.getWidth - 50), headerHelpIcon)
  }

  //final monitor pane 
  alignment = Pos.CENTER
  alignmentInParent = Pos.CENTER
  spacing = 1
  hgrow = Priority.Always
  vgrow = Priority.ALWAYS
  children = Seq(ScalaFxUtils.newVSpacer(25),
    new VBox {
      spacing = V_SPACING
      children = List(
        udsDbErrorLabel,
        adminConfigErrorLabel,
        connectionErrorLabel,
        serverConfigWarningLabel,
        seqReposWarningLabel,
        pgsqlDataDirWarningLabel,
        helpPane,
        jmsServerPane,
        ScalaFxUtils.newVSpacer(10),
        postgreSQLServerPane)
    })

  /** open Proline-admin guide file */
  def openAdminGuide() {
    UserGuide.openUrl(Monitor.targetPath + File.separator + "classes" + File.separator + "documentation" + File.separator + "Proline_AdminGuide_1.7.pdf")
  }

  /** get Proline-Admin confi initial settings  */
  def getAdminConfigOpt(): Option[AdminConfig] = {
    try {
      if (Monitor.adminConfPathIsEmpty()) return None
      else {
        val adminConfFile = new AdminConfigFile(Monitor.adminConfPath)
        adminConfFile.read()
      }
    } catch {
      case t: Throwable => {
        logger.error("Error occured while trying to get Proline-admin configurations", t.getMessage())
        None
      }
    }
  }

  /** get Proline Server jms-node initial settings */
  def getNodeConfigOpt(): Option[NodeConfig] = {
    try {
      if (adminConfig.serverConfigFilePath.isDefined && new File(adminConfig.serverConfigFilePath.get).exists) {
        val prolineServerConfigParent = new File(adminConfig.serverConfigFilePath.get).getParent
        val nodeConfigPath = new File(prolineServerConfigParent + File.separator + "jms-node.conf").getCanonicalPath
        val nodeConfigFile = new NodeConfigFile(nodeConfigPath)
        nodeConfigFile.read
      } else {
        logger.warn("Could not find the jms-node configurations file.")
        None
      }
    } catch {
      case t: Throwable => {
        logger.error("Error occured while trying to get jms-node configurations", t.getMessage())
        None
      }
    }
  }

  /** set initial configurations */
  def setInitialConfig() {
    try {
      ProlineAdminConnection._setNewProlineInstallConfig(Monitor.adminConfPath)
    } catch {
      case t: Throwable => logger.error("Error while trying to set the new configurations", t)
    }
  }
  /** check Proline-Admin configurations and show warning and error labels */
  def isAdminConfigsOk(adminConfig: AdminConfig): Seq[Boolean] = adminConfig match {
    case adminConfigValue @ AdminConfig(filePath, serverConfigFilePath, pwx, pgsqlDataDir, seqRepoConfigFilePath, _, _, _, _, _, _) => {
      logger.debug("Loading Proline configurations. Please wait ...")
      System.out.println("INFO - Loading Proline configurations. Please wait ...")
      //check that uds is installed 
      val isUdsDbReachable = UdsRepository.isUdsDbReachable()
      udsDbErrorLabel.visible = !isUdsDbReachable
      //check that the connection is established 
      val isConnectionEstablished = DatabaseConnection.testDbConnection(adminConfigValue, showSuccessPopup = false, showFailurePopup = false)
      connectionErrorLabel.visible = !isConnectionEstablished
      //check AdminCinfi with warning messgaes
      adminConfigErrorLabel.visible = !(new File(filePath).exists)
      serverConfigWarningLabel.visible = !serverConfigFilePath.isDefined || !(new File(serverConfigFilePath.get).exists)
      pgsqlDataDirWarningLabel.visible = !pgsqlDataDir.isDefined || !(new File(pgsqlDataDir.get).exists)
      seqReposWarningLabel.visible = !seqRepoConfigFilePath.isDefined || !(new File(seqRepoConfigFilePath.get).exists)
      Seq(isUdsDbReachable,
        isConnectionEstablished,
        (new File(filePath).exists))
    }
    case _ => logger.error("Error while trying to load initial Proline configurations!"); Seq.empty
  }

}