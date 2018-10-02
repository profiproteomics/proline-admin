package fr.proline.admin.gui.wizard.monitor.component

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.stage.Modality
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.ComboBox
import scalafx.scene.control.CheckBox
import scalafx.scene.control.PasswordField
import scalafx.scene.control.TextField
import scalafx.scene.control.ProgressBar
import scalafx.scene.control.TitledPane
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.Priority
import scalafx.scene.layout.{ VBox, HBox }

import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.wizard.service.ChangePwd
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

/**
 * ChangePwdDialog build a dialog to change user password.
 *
 * @author aromdhani
 * @param wTitle The dialog title
 * @param wParent The parent of this dialog.
 * @param isResizable is the dialog resizable.
 *
 */

class ChangePwdDialog(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = true) extends Stage with IMonitorForm with LazyLogging {
  val changePwdPanel = this
  title = wTitle
  width_=(600)
  height_=(260)
  this.setResizable(isResizable)
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  changePwdPanel.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)

  // component
  val warningPwLabel = new Label {
    text = "The user passwords are invalid. Both passwords must be the same."
    visible = false
    managed <== visible
    style = TextStyle.RED_ITALIC
  }
  val warningLoginLabel = new Label {
    text = "Select a user to change his password."
    visible = false
    managed <== visible
    style = TextStyle.RED_ITALIC
  }
  val warningPanel = new VBox {
    spacing = 5
    children = Seq(warningLoginLabel, warningPwLabel)
  }
  val userFirstPwLabel = new Label("New password")
  val userFirstPwTextField = new PasswordField()

  val userSecondPwLabel = new Label("Confirm password")
  val userSecondPwTextField = new PasswordField()

  val changePwdButton = new Button("Change") {
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
    prefWidth = changePwdPanel.width.get
  }
  val informationPanel = new VBox {
    alignmentInParent = Pos.BOTTOM_CENTER
    spacing = 5
    children = Seq(new HBox {
      alignment = Pos.Center
      padding = Insets(10)
      children = Seq(new Label(""),
        informationLabel)
    }, progressBar)
  }
  //layout
  val userFirstPwPanel = new HBox {
    spacing = 30
    children = Seq(userFirstPwLabel,
      userFirstPwTextField)
  }
  val userSecondPwPanel = new HBox {
    spacing = 30
    children = Seq(userSecondPwLabel,
      userSecondPwTextField)
  }
  val userPanel = new VBox {
    spacing = 10
    children = Seq(warningPanel,
      userFirstPwPanel,
      userSecondPwPanel)
  }
  //Style  
  Seq(changePwdButton,
    exitButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }
  Seq(userFirstPwTextField,
    userSecondPwTextField).foreach { component =>
      component.prefWidth = 200
      component.hgrow_=(Priority.ALWAYS)
    }
  Seq(warningLoginLabel,
    userFirstPwLabel,
    userSecondPwLabel).foreach { component =>
      component.prefWidth = 200
    }
  val contentPane = new VBox {
    alignment = Pos.Center
    spacing = 5
    padding = Insets(5)
    children = List(
      userPanel,
      new HBox {
        alignment = Pos.Center
        padding = Insets(5)
        spacing = 30
        children = Seq(changePwdButton,
          exitButton)
      }, informationPanel)
  }
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(changePwdPanel, ke) }
    root = new TitledPane {
      text = "Change password"
      expanded_=(true)
      collapsible_=(false)
      margin = Insets(5, 5, 5, 5)
      content_=(contentPane)
    }
  }

  /** check dialog fields */
  def checkFields(): Boolean = {
    if (userFirstPwTextField.getText == userSecondPwTextField.getText) {
      warningPwLabel.visible_=(false)
    } else {
      warningPwLabel.visible_=(true)
    }
    if (!UsesrsPanel.usersTable.selectedItems.isEmpty) {
      warningLoginLabel.visible_=(false)
    } else {
      warningLoginLabel.visible_=(true)
    }
    Seq(userFirstPwTextField.getText == userSecondPwTextField.getText,
      !UsesrsPanel.usersTable.selectedItems.isEmpty).forall(_.==(true))
  }
  /** create new user task  */
  def validate(): Unit = {
    if (checkFields()) {
      val userIdOpt = UsesrsPanel.usersTable.selectedItems.headOption
      val newPwd = if (!userFirstPwTextField.getText.isEmpty) Some(userFirstPwTextField.getText) else None
      userIdOpt.foreach { userId => ChangePwd(userId.id.value, newPwd, changePwdPanel).restart() }
    }
  }
  /** exit and close dialog */
  def exit(): Unit = {
    changePwdPanel.close()
  }
}
object ChangePwdDialog {
  /**
   * @param wTitle The dialog title
   * @param wParent The parent of this dialog.
   * @param isResizable is the dialog resizable.
   */
  def apply(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) { new ChangePwdDialog(wTitle, wParent, isResizable).showAndWait() }
}