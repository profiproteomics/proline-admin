package fr.proline.admin.gui.install.view.jms

import scalafx.Includes._
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.control.{ RadioButton, ToggleGroup }
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.scene.layout.Priority
import fr.proline.admin.gui.Install
import fr.proline.admin.gui.install.model.JmsModelView
import fr.proline.admin.gui.process.config.NodeConfig
import fr.profi.util.scalafx.NumericTextField
import fr.profi.util.scalafx.TitledBorderPane
import fr.profi.util.scalafx.ScalaFxUtils
import scala.collection.Seq
import scalafx.scene.control.RadioButton.sfxRadioButton2jfx

/**
 * Creates and displays JMS server panel.
 * @author aromdhani
 *
 */

class ServerJmsPanel(model: JmsModelView) extends VBox {

  // Load initial jms-node configurations
  val nodeConfigOpt = model.nodeConfig()
  require(nodeConfigOpt.isDefined, "Jms-node config must not be null!")
  val nodeConfig = nodeConfigOpt.get
  private var jmsHostName = nodeConfig.jmsServerHost.getOrElse("localhost")
  private var jmsPort = nodeConfig.jmsServePort.getOrElse(5445)
  private var prolineQueueName = nodeConfig.requestQueueName.getOrElse("ProlineServiceRequestQueue")

  /*
	 * ********** *
	 * COMPONENTS *
	 * ********** *
	 */

  // CheckBox panel
  val tog = new ToggleGroup()
  var embeddedJmsRdButton = new RadioButton {
    text = "  Use embedded JMS server"
    selected = true
    toggleGroup = tog
  }
  var specificJmsRdButton = new RadioButton {
    text = "  Use specific JMS server"
    toggleGroup = tog
  }
  val hostLabel = new Label("Host: ")
  val hostField = new TextField {
    tooltip = "Enter Proline host name value"
    disable <== embeddedJmsRdButton.selected
    if (jmsHostName != null) text = jmsHostName
    text.onChange { (_, oldText, newText) =>
      jmsHostName = newText
    }
  }
  // Port panel
  val portLabel = new Label("Port: ")
  val portField = new NumericTextField {
    tooltip = "Enter Proline port value"
    disable <== embeddedJmsRdButton.selected
    text = jmsPort.toString
    text.onChange {
      (_, oldText, newText) =>
        if ((newText != null) && !newText.equals("")) {
          jmsPort = newText.toInt
        }
    }
  }

  // ProlineQueueName panel
  val queueNameLabel = new Label("Proline Queue Name: ")
  val queueNameField = new TextField {
    tooltip = "Enter Proline queue name value"
    disable <== embeddedJmsRdButton.selected
    if (prolineQueueName != null) text = prolineQueueName
    text.onChange { (_, oldText, newText) =>
      prolineQueueName = newText
    }
  }

  /*
 * ****** *
 * LAYOUT *
 * ****** *
 */

  Seq(queueNameLabel, portLabel, hostLabel).foreach(_.minWidth = 60)
  Seq(hostField, portField, queueNameField).foreach {
    f => { f.hgrow = Priority.Always; f.vgrow = Priority.Always }
  }
  private val V_SPACING = 5
  private val H_SPACING = 5
  val jmsServerPane = new TitledBorderPane(
    title = "JMS Server",
    contentNode = new VBox {
      prefHeight <== (Install.stage.height -350)
      hgrow = Priority.Always
      vgrow = Priority.Always
      spacing = V_SPACING
      children = Seq(
        hostLabel,
        new HBox {
          hgrow = Priority.Always
          spacing = H_SPACING
          children = Seq(hostField)
        },
        ScalaFxUtils.newVSpacer(V_SPACING),
        portLabel,
        new HBox {
          hgrow = Priority.Always
          spacing = H_SPACING
          children = Seq(portField)
        },
        ScalaFxUtils.newVSpacer(V_SPACING),
        queueNameLabel,
        new HBox {
          hgrow = Priority.Always
          spacing = H_SPACING
          children = Seq(queueNameField)
        },
        ScalaFxUtils.newVSpacer(V_SPACING))
    })

  // Radio Button panel
  val rdButtonPane = new VBox {
    spacing = 5
    minWidth = 20
    children = List(
      new HBox {
        spacing = 5
        children = Seq(embeddedJmsRdButton, specificJmsRdButton)
      })
  }

  // Node content
  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = 10
  padding = Insets(15, 5, 5, 5)
  children = List(
    ScalaFxUtils.newVSpacer(V_SPACING),
    rdButtonPane,
    ScalaFxUtils.newVSpacer(V_SPACING),
    jmsServerPane)

  /** Return jms-node config  */
  def toConfig() = NodeConfig(
    Option(this.jmsHostName),
    Option(this.jmsPort),
    Option(this.prolineQueueName),
    Option(-1),
    Option(true))

  /** Return Jms server type */
  def getType: Boolean = { embeddedJmsRdButton.isSelected }

}