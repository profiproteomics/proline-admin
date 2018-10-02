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
import scalafx.scene.control.TitledPane
import scalafx.scene.control.ProgressBar
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.Priority
import scalafx.scene.layout.{ VBox, HBox }

import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.wizard.service.AddUser
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

/**
 * NewUserDialog build a dialog to create a new user
 * @author aromdhani
 * @param wTitle The dialog title
 * @param wParent The parent of this dialog.
 * @param isResizable is the dialog resizable.
 *
 */

class NewUserDialog(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) extends Stage with IMonitorForm with LazyLogging {
  val newUserPane = this
  title = wTitle
  width_=(600)
  height_=(350)
  this.setResizable(isResizable)
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  newUserPane.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
  // component
  val warningPwLabel = new Label {
    text = "The user passwords are invalid. Both passwords must be the same."
    visible = false
    managed <== visible
    style = TextStyle.RED_ITALIC
  }
  val warningLoginLabel = new Label {
    text = "Login must not be empty. Only letters, numbers, white spaces in the middle, '-', '.' and '_' may be used."
    visible = false
    managed <== visible
    style = TextStyle.RED_ITALIC
  }
  val warningLoginExistLabel = new Label {
    text = s"This login has already been taken. Choose a new login."
    visible = false
    managed <== visible
    style = TextStyle.RED_ITALIC
  }
  val warningPanel = new VBox {
    spacing = 5
    children = Seq(warningLoginLabel, warningPwLabel, warningLoginExistLabel)
  }
  val loginLabel = new Label("User login")
  val loginTextField = new TextField()

  val userFirstPwLabel = new Label("User password")
  val userFirstPwTextField = new PasswordField()

  val userSecondPwLabel = new Label("Confirm password")
  val userSecondPwTextField = new PasswordField()

  val userGrpChbox = new CheckBox {
    text = "Add user to administrator group"
    selected = false
    minWidth = 200
  }
  val addButton = new Button("Create") {
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
    prefWidth = newUserPane.width.get
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
  //layout
  val userLoginPanel = new HBox {
    spacing = 30
    children = Seq(loginLabel, loginTextField)
  }
  val userFirstPwPanel = new HBox {
    spacing = 30
    children = Seq(userFirstPwLabel, userFirstPwTextField)
  }
  val userSecondPwPanel = new HBox {
    spacing = 30
    children = Seq(userSecondPwLabel, userSecondPwTextField)
  }
  val userPanel = new VBox {
    spacing = 10
    children = Seq(warningPanel, userLoginPanel, userFirstPwPanel, userSecondPwPanel, userGrpChbox)
  }
  //Style  
  Seq(addButton,
    exitButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }

  userGrpChbox.setPrefWidth(200)
  Seq(userFirstPwTextField,
    userSecondPwTextField, loginTextField).foreach { component =>
      component.prefWidth = 200
      component.hgrow_=(Priority.ALWAYS)
    }
  Seq(loginLabel,
    userFirstPwLabel, userSecondPwLabel, userGrpChbox).foreach { component =>
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
        children = Seq(addButton,
          exitButton)
      }, informationPanel)
  }
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(newUserPane, ke) }
    root = new TitledPane {
      text = "New user"
      expanded_=(true)
      collapsible_=(false)
      margin = Insets(5, 5, 5, 5)
      content_=(contentPane)
    }
  }

  /** check if already login exist */
  def isDefinedLogin: Boolean = {
    UsesrsPanel.getAllUsers.find(_.getLogin == loginTextField.getText).isDefined
  }

  /** check new user panel fields */
  def checkFields(): Boolean = {
    val isValidLogin = loginTextField.getText match {
      case str if str matches ("^([a-zA-Z0-9_.-]+)$") => true
      case _ => false
    }
    if (isValidLogin) {
      warningLoginLabel.visible_=(false)
    } else {
      warningLoginLabel.visible_=(true)
    }
    if (userFirstPwTextField.getText == userSecondPwTextField.getText) {
      warningPwLabel.visible_=(false)
    } else {
      warningPwLabel.visible_=(true)
    }
    if (!isDefinedLogin) {
      warningLoginExistLabel.visible_=(false)
    } else {
      warningLoginExistLabel.visible_=(true)
    }
    Seq(
      isValidLogin,
      userFirstPwTextField.getText == userSecondPwTextField.getText,
      !isDefinedLogin).forall(_.==(true))
  }

  /** create new user  */
  def validate(): Unit = {
    if (checkFields()) {
      val userPwd = if (!userFirstPwTextField.getText().isEmpty) Some(userFirstPwTextField.getText()) else None
      val userGroup = !userGrpChbox.isSelected
      AddUser(loginTextField.getText, userPwd, userGroup, newUserPane).restart()
    }
  }
  /** exit and close this dialog */
  def exit(): Unit = {
    newUserPane.close()
  }
}
object NewUserDialog {
  /**
   * @param wTitle The dialog title
   * @param wParent The parent of this dialog.
   * @param isResizable is the dialog resizable.
   */
  def apply(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) { new NewUserDialog(wTitle, wParent, isResizable).showAndWait() }
}