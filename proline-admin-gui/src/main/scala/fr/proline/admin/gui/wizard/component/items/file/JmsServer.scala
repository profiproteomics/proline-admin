package fr.proline.admin.gui.wizard.component.items.file

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.control.{ RadioButton, ToggleGroup }
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.scene.layout.Priority

import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.wizard.process.config.NodeConfigFile
import fr.proline.admin.gui.wizard.process.config.NodeConfig
import fr.proline.admin.gui.wizard.component.panel.main.ITabForm
import fr.profi.util.scalafx.NumericTextField
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.TitledBorderPane
import fr.profi.util.scala.ScalaUtils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

/**
 * JmsServerTab build a tab for JMS server properties : Jms host , port and Proline queue name.
 * @param path the path of jms server configuration file.
 */

class JmsServer(path: String) extends VBox with ITabForm with LazyLogging {

  /*
	 * ********** *
	 * COMPONENTS *
	 * ********** *
	 */
  //initialize jms-node configuration properties

  val nodeConfigFile = new NodeConfigFile(path)
  val nodeConfigOpt: Option[NodeConfig] = nodeConfigFile.read
  require(nodeConfigOpt.isDefined, "Jms-node config is undefined.")
  val nodeConfig = nodeConfigOpt.get

  private var _jmsHostName = nodeConfig.jmsServerHost.getOrElse("")
  private var _jmsPort = nodeConfig.jmsServePort.getOrElse(0)
  private var _prolineQueueName = nodeConfig.requestQueueName.getOrElse("")

  // chekcbox
  val tog = new ToggleGroup()
  var embeddedJmsRdButton = new RadioButton {
    text = "  Use embedded JMS Server"
    selected = true
    toggleGroup = tog
  }
  var specificJmsRdButton = new RadioButton {
    text = "  Use Specific JMS Server"
    toggleGroup = tog
  }
  val hostLabel = new Label("Host: ")
  val hostField = new TextField {
    disable <== embeddedJmsRdButton.selected
    if (_jmsHostName != null) text = _jmsHostName
    text.onChange { (_, oldText, newText) =>
      _jmsHostName = newText
      checkForm
    }
  }
  // port 
  val portLabel = new Label("Port: ")
  val portField = new NumericTextField {
    disable <== embeddedJmsRdButton.selected
    text = _jmsPort.toString
    text.onChange {
      (_, oldText, newText) =>
        if ((newText != null) && !newText.equals("")) {
          _jmsPort = newText.toInt
          checkForm
        }
    }
  }

  // proline queue name 
  val queueNameLabel = new Label("Proline Queue Name: ")
  val queueNameField = new TextField {
    disable <== embeddedJmsRdButton.selected
    if (_prolineQueueName != null) text = _prolineQueueName
    text.onChange { (_, oldText, newText) =>
      _prolineQueueName = newText
      checkForm
    }
  }

  /*   
 * ****** *
 * LAYOUT *
 * ****** *
 */

  Seq(queueNameLabel, portLabel, hostLabel).foreach(_.minWidth = 60)
  Seq(hostField, portField, queueNameField).foreach {
    f => f.hgrow = Priority.Always
  }

  val jmsServerPane = new TitledBorderPane(
    title = "JMS Server",
    contentNode = new VBox {
      prefWidth <== Wizard.configItemsPanel.width - 30
      prefHeight <== Wizard.configItemsPanel.height - 30
      spacing = 3
      children = Seq(
        emptyFieldErrorLabel,
        hostLabel, new HBox {
          spacing = 5
          children = Seq(hostField)
        },
        ScalaFxUtils.newVSpacer(minH = 5), portLabel,
        new HBox {
          spacing = 5
          children = Seq(portField)
        },
        ScalaFxUtils.newVSpacer(minH = 5), queueNameLabel,
        new HBox {
          spacing = 5
          children = Seq(queueNameField)
        },
        ScalaFxUtils.newVSpacer(minH = 5))
    })

  // position in center
  val checkBoxPane = new VBox {
    spacing = 5
    children = List(
      new HBox {
        spacing = 5
        children = Seq(embeddedJmsRdButton, specificJmsRdButton)
      })
  }
  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = 5
  margin = Insets(5, 5, 5, 5)
  children = List(ScalaFxUtils.newVSpacer(minH = 5),
    checkBoxPane, ScalaFxUtils.newVSpacer(minH = 15),
    jmsServerPane)

  /** check fields */
  def checkForm: Boolean = {
    val isValidatedFields = Seq(hostField, portField, queueNameField).forall(!_.getText.trim().isEmpty())
    if (isValidatedFields) emptyFieldErrorLabel.visible = false else emptyFieldErrorLabel.visible = true
    isValidatedFields
  }

  /** Get GUI information to create new jmsConfig object */
  private def _toJMSConfig() = NodeConfig(Option(this._jmsHostName),
    Option(this._jmsPort),
    Option(this._prolineQueueName),
    Option(-1),
    Option(true))

  /** Save proline's Server nodeConfig properties */
  def saveForm() {
    /* new jmsConfig */
    if (nodeConfigOpt.isDefined) {
      val newConfig = Future {
        val newJmsNodeConfig = _toJMSConfig()
        nodeConfigFile.write(newJmsNodeConfig)
      }
      newConfig onFailure {
        case (t) => logger.error(s"An error has occured: ${t.getMessage}")
      }
    }
  }

  /** get type of JmsServer */
  def getInfos: String = {
    if (embeddedJmsRdButton.isSelected) s"""JMS Server: Embedded""" else
      s"""JMS Server: Specific"""
  }
}