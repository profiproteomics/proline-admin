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

  //Proline server initial values 

  private val adminConfig = Monitor.serverInitialConfing.get
  private val JmsConfig = Monitor.serverJmsInitialConfig.get

  //Proline JMS server initial values 
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
    text = JmsConfig.jmsServerHost.get
  }

  val jmsPortLabel = new Label("Port: ")
  val jmsPortField = new TextField() {
    disable = true
    text = JmsConfig.jmsServePort.get.toString
  }

  val jmsProlineQueueLabel = new Label("Proline Queue Name:")
  val jmsProlineQueueField = new TextField {
    disable = true
    text = JmsConfig.requestQueueName.get
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
    text = adminConfig.dbHost.get
  }

  val pgPortLabel = new Label("Port: ")
  val pgPortField = new TextField() {
    disable = true
    text = adminConfig.dbPort.get.toString
  }

  val pgUserLabel = new Label("User: ")
  val pgUserField = new TextField() {
    disable = true
    text = adminConfig.dbUserName.get
  }

  val pgPasswordLabel = new Label("Password: ")
  val pgPasswordField = new TextField() {
    disable = true
    text = adminConfig.dbPassword.get
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
      spacing = V_SPACING * 4
      children = List(jmsServerPane, ScalaFxUtils.newVSpacer(10), postgreSQLServerPane)
    })
}