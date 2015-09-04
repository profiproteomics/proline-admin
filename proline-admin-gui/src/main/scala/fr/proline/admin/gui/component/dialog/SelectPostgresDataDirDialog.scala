package fr.proline.admin.gui.component.dialog

import java.io.File

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Cursor
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Hyperlink
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.VBox
import scalafx.stage.Modality
import scalafx.stage.Stage

import com.typesafe.scalalogging.slf4j.Logging

import fr.profi.util.scala.ScalaUtils
import fr.profi.util.scalafx.ScalaFxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.Main
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.proline.admin.gui.util.FxUtils


/**
 * ************************************************* *
 * Open a dialog to select PostgreSQL data directory *
 * ************************************************* *
 **/

object SelectPostgresDataDirDialog extends Stage with Logging {

  val dialog = this

  /* Stage's properties */
  title = s"Select PostgreSQL data directory"
  initModality(Modality.WINDOW_MODAL)
  initOwner(Main.stage)

  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */

  val headerLabel =new Label("Full path to PostgreSQL data directory :")
  
  val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    onAction = handle { _openHelpDialog() }
  }

  val dataDirField = new TextField {
    //prefWidth <== dialog.width
    hgrow = Priority.Always
    onKeyReleased = (ke: KeyEvent) => ScalaFxUtils.fireIfEnterPressed(saveButton, ke)
  }

  val browseButton = new Button("Browse...") {
    onAction = handle { _browseDataDir() }
  }

  val saveButton = new Button("Save") { onAction = handle { _onApplyPressed() } }

  val cancelButton = new Button("Cancel") { onAction = handle { dialog.close() } }

  /*
   * ****** *
   * LAYOUT *
   * ****** *
   */

  scene = new Scene {

    onKeyPressed = (ke: KeyEvent) => {
      ScalaFxUtils.closeIfEscapePressed(dialog, ke)
      ScalaFxUtils.fireIfEnterPressed(saveButton, ke)
    }
    
    root = new VBox {
      padding = Insets(20)
      minWidth = 464
      prefWidth = 592
      spacing = 5

      content = Seq(

        /* Header */
        new HBox {
          spacing = 10
          content = List(headerLabel, headerHelpIcon)
        },

        /* Data dir */
        new HBox {
          spacing = 5
          content = Seq(dataDirField, browseButton)
        },

        ScalaFxUtils.newVSpacer(minH = 10),

        /* Bottom (save/cancel) */
        new HBox {
          hgrow = Priority.Always
          alignment = Pos.BaselineCenter
          content = Seq(
            saveButton,
            ScalaFxUtils.newHSpacer(maxW = 10),
            cancelButton
          )
        }
      )
    }
  }

  /*
   * ******** *
   * FEATURES *
   * ******** *
   */

  /** Init and show this stage **/
  def apply() {
    dataDirField.text = Main.postgresqlDataDir
    this.showAndWait()
  }

  /** Open a popup to help user understand whant the PostgreSQL data dir is **/
  private def _openHelpDialog() = ShowPopupWindow(
    "Help",
    "The PostgreSQL data directory is defined when PostgreSQL is installed on the machine.\n" +
      "This is the folder in which you will find the \"postgresql.conf\" and \"pg_hba.conf\" files."
  )

  /** Browse PostresSQL data dir and update field **/
  private def _browseDataDir() {
    val file = FxUtils.browseDirectory(
      dcTitle = "Select PostgreSQL data directory",
      dcInitialDir = Main.postgresqlDataDir
    )

    if (file != null) dataDirField.text = file.getPath()
  }

  /** Action run when "Apply" in pressed: save in global variable if defined **/
  private def _onApplyPressed() {

    Main.stage.scene().setCursor(Cursor.WAIT)

    /* Make sure dataDir is provided */
    val selectedDataDir = dataDirField.text()

    if (ScalaUtils.isEmpty(selectedDataDir)) {
      ShowPopupWindow(
        "Data directory is empty",
        "Path to PostgreSQL data directory must be provided to configure PostgreSQL."
      )
    } else {

      /* Test if directory exists, offer to create it if not */
      val dataDir = new File(selectedDataDir)

      if ((dataDir.exists() && dataDir.isDirectory()) == false) {

        val isConfirmed = GetConfirmation(
          title = "Unknown directory",
          text = "The directory you choosed doesn't exit.",
          yesText = "Create directory"
        )

        if (isConfirmed) dataDir.createNewFile()
      }

      /* Update variable and close dialog */
      if (Main.postgresqlDataDir != selectedDataDir) {
        Main.postgresqlDataDir = selectedDataDir
        //TODO: make new AdminConfigFile(Main.adminConfPath) static
        new AdminConfigFile(Main.adminConfPath).setPostgreSqlDataDir(selectedDataDir) //persist in config file
      }
      dialog.close()

      /* Logback */
      println("[INFO] PostgreSQL data directory @ " + selectedDataDir)
      logger.debug("PostgreSQL data directory @ " + selectedDataDir)
    }

    Main.stage.scene().setCursor(Cursor.DEFAULT)
  }
  
}