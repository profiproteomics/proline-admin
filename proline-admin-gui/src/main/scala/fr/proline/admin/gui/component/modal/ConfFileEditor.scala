package fr.proline.admin.gui.component.modal

import java.io.FileWriter

import scala.io.Source

import fr.proline.admin.gui.Main
import fr.proline.admin.gui.component.panel.ButtonsPanel
import fr.proline.admin.gui.process.LaunchAction
import fr.proline.admin.gui.process.UdsConnection
import fr.proline.admin.service.db.SetupProline

import scalafx.Includes.handle
import scalafx.application.Platform
import scalafx.beans.property.BooleanProperty.sfxBooleanProperty2jfx
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.TextArea
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.VBox
import scalafx.stage.Modality
import scalafx.stage.Stage

/**
 * Create a modal window to edit Proline configuration's file.
 */
class ConfFileEditor {

  /** Define modal window */
  private val _stage = new Stage {

    configEditor =>
    title = "Proline configuration editor"
    initModality(Modality.WINDOW_MODAL)
    initOwner(Main.stage)

    scene = new Scene {

      /**
       * ********** *
       * COMPONENTS *
       * ********** *
       */

      /** Text area to display and edit config file content */
      val textEditArea = new TextArea {
        id = "fileEditArea"
        hgrow = Priority.ALWAYS
        vgrow = Priority.ALWAYS

                val is = Source.fromInputStream(this.getClass().getResourceAsStream("/application.conf"))
//        val is = Source.fromInputStream(SetupProline.getClass().getResourceAsStream("/application.conf"))
        text = is.mkString
        is.close()
      }

      /** "Save" and "Cancel" buttons */
      val saveButton = new Button("Save")
      //      {
      //        styleClass += ("minorButtons")
      //      }

      val cancelButton = new Button("Cancel")
      //      {
      //        styleClass += ("minorButtons")
      //        cancelButton = true
      //      }

      /**
       * ****** *
       * LAYOUT *
       * ****** *
       */
      //      stylesheets = List(Main.CSS)

      val buttons = new HBox {
        padding = Insets(5)
        hgrow = Priority.ALWAYS
        alignment = Pos.CENTER
        spacing = 20
        content = List(saveButton, cancelButton)
      }

      root = new VBox {
        content = List(textEditArea, buttons)
        prefHeight = 800
        prefWidth = 700
        padding = Insets(5)
        spacing = 10
      }

      /**
       * ****** *
       * ACTION *
       * ****** *
       */
      cancelButton.onAction = handle { configEditor.close() }

      saveButton.onAction = handle {

        /** Update file content */

        //TODO: clean it (debug mode)

        println("1111")
        //        val _confFile = this.getClass().getClassLoader().getResource("/application.conf") //TODO: PAdmin . getClass.....
        val _confFile = SetupProline.getClass().getClassLoader().getResource("/application.conf") //TODO: PAdmin . getClass.....
        println(_confFile)
        println("222")
        try {
          val confFile = _confFile.getFile()
          println("333")
          val fileWriter = new FileWriter(confFile)
          println("444")

          //        val confRes = this.getClass().getClassLoader().getResource("/application.conf")
          //        val confFile = new File(confRes.toURI())
          //        val fileWriter = new FileWriter(confFile, false)

          fileWriter.write(textEditArea.text.value)
          fileWriter.close()
        } catch {
          case e: Exception =>
            println("exception here")
            e.printStackTrace()

        }

        /** Set new Proline configuration effective */

        LaunchAction(
          actionButton = ButtonsPanel.editConfButton,
          actionString = "Updating Proline configuration",
          action = () => {

            try {
              UdsConnection.setNewProlineConfig()
              ButtonsPanel.updateBooleans()

            } catch {
              case e: Exception => {
                Platform.runLater {
                  //                  println("  ERROR - Could not set new Proline configuration : " + e)
                  println("  ERROR - Could not set new Proline configuration : ")
                  e.printStackTrace()
                  ButtonsPanel.dbCanBeUsed.set(false)
                  ButtonsPanel.prolineMustBeSetUp.set(false)
                }
              }
            }
          }
        )
        Platform.runLater(println("Proline configuration updated."))
        configEditor.close()

      }
    }
  }

  /** Display this window */
  _stage.showAndWait()
}