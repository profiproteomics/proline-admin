//package fr.proline.admin.gui.component.dialog
//
//import java.io.FileWriter
//import scala.io.Source
//import scalafx.Includes._
//import scalafx.geometry.Insets
//import scalafx.geometry.Pos
//import scalafx.scene.Cursor
//import scalafx.scene.Cursor.sfxCursor2jfx
//import scalafx.scene.Scene
//import scalafx.scene.control.Button
//import scalafx.scene.control.TextArea
//import scalafx.scene.input.KeyCode
//import scalafx.scene.input.KeyEvent
//import scalafx.scene.layout.HBox
//import scalafx.scene.layout.Priority
//import scalafx.scene.layout.VBox
//import scalafx.stage.Modality
//import scalafx.stage.Stage
//import com.typesafe.scalalogging.slf4j.Logging
//import fr.proline.admin.gui.Main
//import fr.proline.admin.gui.process.ProlineAdminConnection
//import fr.profi.util.scalafx.ScalaFxUtils
//
///**
// * Create a modal window to edit Proline configuration's file.
// */
//class ConfFileEditor extends Logging {
//
//  /** Define modal window */
//  private val _stage = new Stage {
//
//    val configEditor = this
//
//    title = s"Proline configuration editor -- ${Main.adminConfPath}"
//    initModality(Modality.WINDOW_MODAL)
//    initOwner(Main.stage)
//
//    //    configEditor.onShowing = handle { ButtonsPanel.someActionRunning.set(true) }
//    //    configEditor.onHiding = handle { ButtonsPanel.someActionRunning.set(false) }
//
//    //    configEditor.onShowing = handle { ButtonsPanel.disableAll() }
//
//    scene = new Scene {
//
//      onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(configEditor, ke) }
//
//      /**
//       * ********** *
//       * COMPONENTS *
//       * ********** *
//       */
//
//      /** Text area to display and edit config file content */
//      val textEditArea = new TextArea {
//        id = "fileEditArea"
//        hgrow = Priority.ALWAYS
//        vgrow = Priority.ALWAYS
//
//        //        val is = Source.fromInputStream(this.getClass().getResourceAsStream("/application.conf"))
//        //        val is = Source.fromInputStream(SetupProline.getClass().getResourceAsStream("/application.conf"))
//        //        text = is.mkString
//        //        is.close()
//        text = Source.fromFile(Main.adminConfPath).mkString
//
//      }
//
//      /** "Save" and "Cancel" buttons */
//      val saveButton = new Button("Save")
//      //      {
//      //        styleClass += ("minorButtons")
//      //      }
//
//      val cancelButton = new Button("Cancel")
//      //      {
//      //        styleClass += ("minorButtons")
//      //        cancelButton = true
//      //      }
//
//      /**
//       * ****** *
//       * LAYOUT *
//       * ****** *
//       */
//      //      stylesheets = List(Main.CSS)
//
//      val buttons = new HBox {
//        padding = Insets(5)
//        hgrow = Priority.ALWAYS
//        alignment = Pos.CENTER
//        spacing = 20
//        content = List(saveButton, cancelButton)
//      }
//
//      root = new VBox {
//        content = List(textEditArea, buttons)
//        prefHeight = 800
//        prefWidth = 700
//        padding = Insets(5)
//        spacing = 10
//      }
//
//      /**
//       * ****** *
//       * ACTION *
//       * ****** *
//       */
//      cancelButton.onAction = handle { configEditor.close() }
//
//      saveButton.onAction = handle {
//
//        Main.stage.scene().setCursor(Cursor.WAIT)
//
//        /** Update file content */
//        val fileWriter = new FileWriter(Main.adminConfPath)
//
//        try {
//          fileWriter.write(textEditArea.text.value)
//
//        } catch {
//          case fxt: java.lang.IllegalStateException => logger.warn(fxt.getLocalizedMessage())
//
//          case t: Throwable =>
//            synchronized {
//              logger.warn("Can't write in configuration file")
//              logger.warn(t.getMessage())
//              //println("ERROR - Unable to write in configuration file:")
//              //println(t.getMessage())
//            }
//          //throw e ?
//
//        } finally {
//          fileWriter.close()
//        }
//
//        /** Set new Proline configuration effective */
//        ProlineAdminConnection.loadProlineConf()
//
//        /** Close window */
//        configEditor.close()
//        Main.stage.scene().setCursor(Cursor.DEFAULT)
//      }
//    }
//  }
//
//  /** Display this window */
//  _stage.showAndWait()
//}