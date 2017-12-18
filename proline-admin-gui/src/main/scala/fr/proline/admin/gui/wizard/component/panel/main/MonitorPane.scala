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
import javafx.stage.Stage

/**
 * Builds home panel of monitor GUI
 *
 */

object MonitorPane extends VBox with LazyLogging {

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
  val jmsPortLabel = new Label("Port: ")
  val jmsProlineQueueLabel = new Label("Proline Queue Name:")
  val jmsHostField = new TextField() {
    editable = false
  }
  val jmsPortField = new TextField() {
    editable = false
  }
  val jmsProlineQueueField = new TextField {
    editable = false
  }
  val jmsTiteledPane = new TitledBorderPane(
    title = "",
    titleTooltip = "JMS Server properties",
    contentNode = new VBox {
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
  val jmsServerPane = new HBox {
    spacing = H_SPACING * 4
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
  val pgPortLabel = new Label("Port: ")
  val pgUserLabel = new Label("User: ")
  val pgPasswordLabel = new Label("Password: ")
  val pgHostField = new TextField() {
    editable = false
  }
  val pgPortField = new TextField() {
    editable = false
  }
  val pgUserField = new TextField() {
    editable = false
  }
  val pgPasswordField = new TextField() {
    editable = false
  }
  val pgTitledPane = new TitledBorderPane(
    title = "",
    titleTooltip = "PostgreSQL Server properties",
    contentNode = new VBox {
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
    })

  val postgreSQLServerPane = new HBox {
    spacing = V_SPACING * 5
    fillWidth = true
    children = List(pgSQLServerLabel, pgTitledPane)
  }

  Seq(pgSQLServerLabel, jmsServelLabel, pgPortLabel, pgHostLabel, pgUserLabel, pgPasswordLabel, jmsHostLabel, jmsPortLabel, jmsProlineQueueLabel).foreach(_.minWidth = 250)
  Seq(pgPortField, pgUserField, pgPasswordField, pgHostField, jmsHostField, jmsProlineQueueField, jmsPortField).foreach {
    f => f.hgrow = Priority.Always
  }
  Seq(pgPortField, pgUserField, pgPasswordField, pgHostField, jmsHostField, jmsProlineQueueField, jmsPortField).foreach(_.minWidth(250))

  //final monitor pane 
  alignment = Pos.CENTER
  alignmentInParent = Pos.CENTER
  spacing = 1
  fillWidth = true
  vgrow = Priority.ALWAYS
  children = Seq(ScalaFxUtils.newVSpacer(50),
    new VBox {
      spacing = V_SPACING * 4
      children = List(jmsServerPane, ScalaFxUtils.newVSpacer(30), postgreSQLServerPane)
    })
}