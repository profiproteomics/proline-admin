package fr.proline.admin.gui.component

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.application.Platform
import scalafx.geometry.Insets
import scalafx.scene.control.Button
import scalafx.scene.control.Button._
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.Scene
import fr.proline.admin.gui.process._
import fr.proline.admin.gui.util._
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils._
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle._
import fr.proline.admin.gui.component.wizard._
import fr.proline.admin.gui.QuickStart
import fr.proline.admin.gui.component.configuration.form._
import fr.proline.admin.gui.process.config._
import fr.proline.admin.gui.component.wizard.MountFilesContent
import scalafx.scene.control.Label
import fr.proline.admin.gui.IconResource
import java.io.File

/**
 * Create  panel contains buttons : cancel ,previous and next .
 */
object ButtonsPanelQStart extends LazyLogging {

  //initialize panels 
  val prolineConfigFilesPanel = new ProlineConfigFilesPanelQStart()
  private val Databaseconfig = new DatabaseConfig()
  // private val monutfiles = new MountFilesContent()
  private var buttonValue: String = _
  var monutfiles: MountFilesContent = null

  /**
   * ******* *
   * BUTTONS *
   * ******* *
   */

  val nextButton = new Button("Next") {
    onAction = handle {
      getButton()
      panelStateOnNext()
      activePreviousButton()
      changeNextToFinish()
    }
  }
  val previousButton = new Button("Previous") {
    onAction = handle {
      panelStateOnPrevious()
      activePreviousButton()
      changeNextToFinish()
    }
  }

  // exit  application 

  val cancelButton = new Button("Cancel") {
    onAction = handle {

      confirmDialog()
    }
  }
  // Warning 
  val warningAboutExitText = "WARNING: Are you sure  to exit setup ? "
  val warningAboutExitLabel = new Label {
    graphic = ScalaFxUtils.newImageView(IconResource.WARNING)
    text = warningAboutExitText
  }

  /**
   * ***** *
   * LAYOUT *
   * **** *
   */

  Seq(
    nextButton,
    previousButton,
    cancelButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }

  /**
   * ****** *
   * APPLY() *
   * ***** *
   */

  /** Display these buttons in a HBox **/
  def apply(): HBox = {
    new HBox {
      padding = Insets(10)
      spacing = 10
      content = Seq(
        cancelButton,
        ScalaFxUtils.newHSpacer(),
        ScalaFxUtils.newHSpacer(),
        ScalaFxUtils.newHSpacer(),
        previousButton,
        nextButton)
    }
  }
  previousButton.setVisible(false)

  // change panel when next button is applied 

  private def panelStateOnNext() {
    if (QuickStart.panelState.equals("panelConfig")) {
      prolineConfigFilesPanel.setAdminfield(QuickStart.adminConfPath)
      prolineConfigFilesPanel.setServerfield(QuickStart.serverConfPath)
      prolineConfigFilesPanel.setSeqfield(QuickStart.seqRepoConfPath)
      QuickStart.mainPanel.getChildren().clear()
      QuickStart.mainPanel.getChildren().add(Databaseconfig)
      QuickStart.panelState = "Databaseconfig"
    } else {
      if (QuickStart.panelState.equals("Databaseconfig")) {

        QuickStart.mainPanel.getChildren().clear()
        monutfiles = new MountFilesContent()
        QuickStart.mainPanel.getChildren().add(monutfiles)
        QuickStart.panelState = "mountfiles"
      }
    }
  }

  // change panel when previous button is applied 

  def panelStateOnPrevious() {
    if (QuickStart.panelState.equals("Databaseconfig")) {
      prolineConfigFilesPanel.setAdminfield(QuickStart.adminConfPath)
      prolineConfigFilesPanel.setServerfield(QuickStart.serverConfPath)
      prolineConfigFilesPanel.setSeqfield(QuickStart.seqRepoConfPath)

      QuickStart.mainPanel.getChildren().clear()
      QuickStart.mainPanel.getChildren().add(prolineConfigFilesPanel)
      QuickStart.panelState = "panelConfig"
    } else {
      if (QuickStart.panelState.equals("mountfiles")) {
        QuickStart.mainPanel.getChildren().clear()
        QuickStart.mainPanel.getChildren().add(Databaseconfig)
        QuickStart.panelState = "Databaseconfig"
      }
    }
  }

  // set visible or not the button in button panel 

  private def activePreviousButton() {
    if (QuickStart.panelState.equals("panelConfig")) {
      previousButton.setVisible(false)
    } else {
      previousButton.setVisible(true)
    }
  }

  // change button next to finish in the end 

  private def changeNextToFinish() {
    if (QuickStart.panelState.equals("mountfiles")) {
      nextButton.setText("Finish")
    } else {
      nextButton.setText("Next")
    }
  }

  // save parameters and close 

  private def getButton() {
    buttonValue = nextButton.getText()
    if (buttonValue.equals("Finish")) {
      monutfiles.saveForm()
      // closeStage()
    }
  }

  // confirm dialog 

  private def confirmDialog() {

    ShowConfirmWindow(
      wTitle = "Warning",
      wText = warningAboutExitText,
      wParent = Option(QuickStart.stage))
  }

  // close window 

  private def closeStage() {

    QuickStart.stage.close()
  }
}