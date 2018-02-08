package fr.proline.admin.gui.wizard.monitor.component

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.ComboBox
import scalafx.scene.control.CheckBox
import scalafx.scene.control.PasswordField
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.StackPane
import scalafx.stage.Modality
import scalafx.stage.Stage
import fr.profi.util.scalafx.ScalaFxUtils
import scalafx.scene.layout.Priority
import scalafx.scene.layout.{ VBox, HBox }
import scalafx.scene.control.TextField
import com.sun.javafx.css.StyleClass

import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.profi.util.scalafx.ScalaFxUtils
import fr.proline.admin.gui.wizard.service._
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle
import com.sun.glass.ui.Application.EventHandler

/**
 * builds new Project panel
 *
 */

class NewUserPane(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) extends Stage {
  val popup = this
  title = wTitle
  minWidth_=(400)
  minHeight_=(200)
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  popup.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
  // component
  val loginLabel = new Label("User login")
  val loginTextField = new TextField()
  val userFirstPwLabel = new Label("User password")
  val userFirstPwTextField = new PasswordField()

  val userSecondPwLabel = new Label("Confirm password")
  val userSecondPwTextField = new PasswordField()

  val warningPwLabel = new Label {
    text = "Invalid password"
    visible = false
    prefWidth = 200
    style = TextStyle.RED_ITALIC
  }
  val warningLoginLabel = new Label {
    text = "Login could not be empty"
    visible <== loginTextField.text.isEmpty()
    prefWidth = 200
    style = TextStyle.RED_ITALIC
  }

  val isAdmin = new CheckBox {
    text = "Add user to administrttor group"
    selected = false
    minWidth = 200
  }

  val addButton = new Button("Add") {
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {
      //check password
      if (userFirstPwTextField.getText() == userSecondPwTextField.getText()) {
        //login is empty 
        if (!loginTextField.getText.isEmpty()) {
          val userTask = new UserTask(loginTextField.getText, Some(userFirstPwTextField.getText()), isAdmin.selected.apply())
          new Thread(userTask.Worker).start()
          popup.close()
        }
      } else {
        warningPwLabel.visible_=(true)
      }
    }
  }
  val cancelButton = new Button("Cancel") {
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      popup.close()
    }
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
    children = Seq(warningLoginLabel, warningPwLabel, userLoginPanel, userFirstPwPanel, userSecondPwPanel, isAdmin)
  }
  //Style  
  Seq(addButton,
    cancelButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }
  warningPwLabel.setPrefWidth(200)

  isAdmin.setPrefWidth(200)
  Seq(userFirstPwTextField,
    userSecondPwTextField, loginTextField).foreach { component =>
      component.prefWidth = 120
      component.hgrow_=(Priority.ALWAYS)
    }
  Seq(loginLabel,
    userFirstPwLabel, userSecondPwLabel, isAdmin).foreach { component =>
      component.prefWidth = 120
    }
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(popup, ke) }
    root = new VBox {
      alignment = Pos.Center
      spacing = 15
      padding = Insets(10)
      children = List(
        userPanel,
        new HBox {
          alignment = Pos.Center
          padding = Insets(10)
          spacing = 30
          children = Seq(addButton,
            cancelButton)
        })
    }
  }
}
object NewUserPane {
  def apply(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) { new NewUserPane(wTitle, wParent, isResizable).showAndWait() }
}