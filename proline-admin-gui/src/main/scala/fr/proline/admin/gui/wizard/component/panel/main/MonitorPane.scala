package fr.proline.admin.gui.wizard.component.panel.main

import com.typesafe.scalalogging.LazyLogging
import scalafx.stage.Stage
import scalafx.geometry.Pos
import scalafx.scene.layout.VBox
import scalafx.scene.layout.HBox
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.scene.layout.Priority
import scalafx.scene.text.{ Font, FontWeight }
import fr.profi.util.scalafx.BoldLabel
import fr.profi.util.scalafx.TitledBorderPane
import fr.profi.util.scalafx.TitledBorderPane
import fr.proline.admin.gui.Monitor
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.ScalaFxUtils
import scalafx.stage.Stage
import fr.proline.admin.gui.wizard.util.WindowSize
import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.proline.admin.gui.process.config.AdminConfig
import fr.proline.admin.gui.wizard.process.config.NodeConfigFile
import fr.proline.admin.gui.wizard.process.config.NodeConfig

/**
 * Builds home panel of monitor GUI
 *
 */

object MonitorPane extends VBox with LazyLogging {

  // Proline server initial properties 
  private var jmsServerHost: String = "localhost"
  private var jmsServerPort: Int = 5445
  private var jmsServerQueueName = "ProlineServiceRequestQueue"
  private var serverHost: String = "<db_host>"
  private var serverUser: String = "<db_user>"
  private var serverPort: Int = 5432
  private var serverPassword: String = "<db_password>"
  //used  to get bin dir to archive a project or to retsore project 
  var serverPgsqlDataDir: Option[String] = None
  private val errorServerLabel = new Label {
    text = "Error while trying to read initial Proline server configurations. Make sure that you have already setup Proline."
    visible = false
    style = TextStyle.RED_ITALIC
  }

  try {
    val adminConfig = Monitor.serverInitialConfing.get
    serverHost = adminConfig.dbHost.get
    serverUser = adminConfig.dbUserName.get
    serverPort = adminConfig.dbPort.get
    serverPassword = adminConfig.dbPassword.get
    serverPgsqlDataDir = adminConfig.pgsqlDataDir
  } catch {
    case t: Throwable =>
      logger.error("Error while trying to get Proline sevrer initial configurations.")
      errorServerLabel.visible_=(true)
  }

  //Jms server initial properties 
  try {
    val JmsConfig = Monitor.serverJmsInitialConfig.get
    jmsServerHost = JmsConfig.jmsServerHost.get
    jmsServerPort = JmsConfig.jmsServePort.get
    jmsServerQueueName = JmsConfig.requestQueueName.get

  } catch {
    case t: Throwable =>
      logger.error("Error while trying to get Proline JMS server initial configuration!")
      errorServerLabel.visible_=(true)
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
    text = jmsServerHost
  }

  val jmsPortLabel = new Label("Port: ")
  val jmsPortField = new TextField() {
    disable = true
    text = jmsServerPort.toString
  }

  val jmsProlineQueueLabel = new Label("Proline Queue Name:")
  val jmsProlineQueueField = new TextField {
    disable = true
    text = jmsServerQueueName
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
    text = serverHost
  }

  val pgPortLabel = new Label("Port: ")
  val pgPortField = new TextField() {
    disable = true
    text = serverPort.toString
  }

  val pgUserLabel = new Label("User: ")
  val pgUserField = new TextField() {
    disable = true
    text = serverUser
  }

  val pgPasswordLabel = new Label("Password: ")
  val pgPasswordField = new TextField() {
    disable = true
    text = serverPassword
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
          spacing = H_SPACING * 2
          children = List(pgPortLabel, pgPortField)
        },
          new HBox {
            spacing = H_SPACING * 2
            children = List(pgUserLabel, pgUserField)
          }, new HBox {
            spacing = H_SPACING * 2
            children = List(pgPasswordLabel, pgPasswordField)
          })
      }))
  }

  val postgreSQLServerPane = new HBox {
    spacing = H_SPACING * 2
    fillWidth = true
    children = List(pgSQLServerLabel, pgTitledPane)
  }

  Seq(pgSQLServerLabel, jmsServelLabel, pgPortLabel, pgHostLabel, pgUserLabel, pgPasswordLabel, jmsHostLabel, jmsPortLabel, jmsProlineQueueLabel).foreach(_.minWidth = 150)
  Seq(pgPortField, pgUserField, pgPasswordField, pgHostField, jmsHostField, jmsProlineQueueField, jmsPortField).foreach {
    f => f.hgrow = Priority.Always
  }

  //final monitor pane 
  alignment = Pos.CENTER
  alignmentInParent = Pos.CENTER
  spacing = 1
  hgrow = Priority.Always
  vgrow = Priority.ALWAYS
  children = Seq(ScalaFxUtils.newVSpacer(25),
    new VBox {
      spacing = V_SPACING * 2
      children = List(errorServerLabel, jmsServerPane, ScalaFxUtils.newVSpacer(10), postgreSQLServerPane)
    })
}