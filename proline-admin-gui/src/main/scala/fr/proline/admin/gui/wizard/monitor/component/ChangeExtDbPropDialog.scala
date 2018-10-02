package fr.proline.admin.gui.wizard.monitor.component

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.stage.Modality
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.scene.control.TitledPane
import scalafx.scene.control.ProgressBar
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.Priority
import scalafx.scene.layout.{ VBox, HBox }

import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle
import fr.proline.admin.gui.wizard.service.ChangeExtDbProperties

/**
 * ChangeExtDbPropDialog build a dialog to change the external database properties.
 * @author aromdhani
 * @param wTitle The dialog title
 * @param wParent The parent of this dialog.
 * @param isResizable is the dialog resizable.
 */

class ChangeExtDbPropDialog(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) extends Stage with IMonitorForm with LazyLogging {
  val changeDbProperties = this
  title = wTitle
  width_=(600)
  height_=(300)
  this.setResizable(isResizable)
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  changeDbProperties.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
  val initSelectedExtDbOpt = if (!DatabasesPanel.externalDbTable.selectedItems.isEmpty) DatabasesPanel.externalDbTable.selectedItems.headOption else None
  // component
  val warningSelectedDbLabel = new Label {
    text = "Select a database to change its properties."
    visible = false
    managed <== visible
    style = TextStyle.RED_ITALIC
  }
  val warningEmptyFieldsLabel = new Label {
    text = "The following fileds must not be empty. Please specify the database properties."
    visible = false
    managed <== visible
    style = TextStyle.RED_ITALIC
  }
  val hostName = new Label {
    text = "Host name"
    prefWidth = 200
  }
  val hostTextField = new TextField {
    text = if (initSelectedExtDbOpt.isDefined) initSelectedExtDbOpt.get.dbHost.value else ""
  }

  val port = new Label {
    text = "Port"
    prefWidth = 200
  }
  val portTextField = new TextField {
    text = if (initSelectedExtDbOpt.isDefined) initSelectedExtDbOpt.get.dbPort.value else ""
  }
  //buttons
  val changeButton = new Button("Change") {
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {
      validate()
    }
  }
  val exitButton = new Button("Exit") {
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      exit()
    }
  }

  //layout 

  val hostPanel = new HBox {
    spacing = 30
    children = Seq(hostName, hostTextField)
  }
  val portPanel = new HBox {
    spacing = 30
    children = Seq(port, portTextField)
  }
  val warningPanel = new VBox {
    spacing = 10
    children = Seq(warningSelectedDbLabel, warningEmptyFieldsLabel)
  }
  val dbPropertiesPanel = new VBox {
    spacing = 10
    children = Seq(warningPanel, hostPanel, portPanel)
  }
  //informations componenets 
  val informationLabel = new Label {
    text = ""
    visible = false
    managed <== visible
    alignmentInParent = Pos.BOTTOM_CENTER
    style = TextStyle.BLUE_ITALIC
  }
  val progressBar = new ProgressBar {
    visible = false
    managed <== visible
    prefWidth = changeDbProperties.width.get
  }
  val informationPanel = new VBox {
    alignmentInParent = Pos.BOTTOM_CENTER
    spacing = 5
    children = Seq(new HBox {
      alignment = Pos.Center
      padding = Insets(10)
      children = Seq(new Label(""), informationLabel)
    }, progressBar)
  }

  //Style
  Seq(hostTextField,
    portTextField).foreach { c =>
      c.prefWidth = 200
      c.hgrow_=(Priority.ALWAYS)
    }
  Seq(changeButton,
    exitButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }

  val contentPane = new VBox {
    alignment = Pos.Center
    spacing = 5
    padding = Insets(5)
    children = List(
      dbPropertiesPanel,
      new HBox {
        alignment = Pos.Center
        padding = Insets(5)
        spacing = 30
        children = Seq(changeButton,
          exitButton)
      }, informationPanel)
  }
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(changeDbProperties, ke) }
    root = new TitledPane {
      text = "Change Exetrnal Database properties"
      expanded_=(true)
      collapsible_=(false)
      margin = Insets(5, 5, 5, 5)
      content_=(contentPane)
    }
  }

  /**
   * check new user panel fields
   *
   */
  def checkFields(): Boolean = {
    val fieldSeq = Seq(portTextField, hostTextField)
    if (fieldSeq.forall(!_.getText.trim.isEmpty())) warningEmptyFieldsLabel.visible_=(false)
    else warningEmptyFieldsLabel.visible_=(true)
    if (DatabasesPanel.externalDbTable.selectedItems.isEmpty) warningSelectedDbLabel.visible_=(true)
    else warningSelectedDbLabel.visible_=(false)
    fieldSeq.forall(!_.getText.trim.isEmpty) && !DatabasesPanel.externalDbTable.selectedItems.isEmpty
  }

  /**
   * Change external database properties.
   *
   */
  def validate(): Unit = {
    if (checkFields()) {
      val extDBsId = Set(DatabasesPanel.externalDbTable.selectedItems.map(_.dbId.value).toList: _*)
      ChangeExtDbProperties(extDBsId,
        hostTextField.getText,
        portTextField.getText.toInt,
        changeDbProperties).restart()
    }
  }
  /**
   * Exit and close this window.
   *
   */
  def exit(): Unit = {
    DatabasesPanel.externalDbTable.selectedItems.clear()
    changeDbProperties.close()
  }
}

object ChangeExtDbPropDialog {
  /**
   * @param wTitle The dialog title
   * @param wParent The parent of this dialog.
   * @param isResizable is the dialog resizable.
   */

  def apply(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) { new ChangeExtDbPropDialog(wTitle, wParent, isResizable).showAndWait() }
}