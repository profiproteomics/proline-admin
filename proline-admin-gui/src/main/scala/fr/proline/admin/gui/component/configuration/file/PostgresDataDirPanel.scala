package fr.proline.admin.gui.component.configuration.file

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.scene.control.Button
import scalafx.scene.control.Hyperlink
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.VBox
import scalafx.stage.Stage

import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.util.ShowPopupWindow

import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

/**
 * ************************************************** *
 * Create a panel to select PostgreSQL data directory. *
 * Common to PgHbaConfigForm and PostgresConfiForm.    *
 * ************************************************** *
 */
class PostgresDataDirPanel(onSelectionChange: String => Boolean = null)(implicit val parentStage: Stage) extends VBox with IConfigFilesPanel with LazyLogging {

  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */
  val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    onAction = _ => { _openHelpDialog() }
  }

  val headerLabel = new Label("Full path to PostgreSQL data directory :")

  val dataDirField = new TextField {
    hgrow = Priority.Always

    // if (Main.postgresDataDirIsEmpty() == false) text = Main.postgresqlDataDir

    if (onSelectionChange != null) {
      text.onChange { (_, oldString, newString) => onSelectionChange(newString) }
    }
  }

  val browseButton = new Button("Browse...") {
    onAction = _ => { _browseDataDir() }
  }

  val warningLabel = new Label() {
    visible = false
    style = TextStyle.RED_ITALIC
  }

  /*
   * ****** *
   * LAYOUT *
   * ****** *
   */
  children = Seq(
    new HBox {
      spacing = 5
      children = List(headerHelpIcon, headerLabel, dataDirField, browseButton)
    },
    warningLabel)

  /*
   * ******** *
   * FEATURES *
   * ******** *
   */

  /** Open a popup to help user understand whant the PostgreSQL data dir is **/
  private def _openHelpDialog() = ShowPopupWindow(
    new Label("The PostgreSQL data directory is defined when PostgreSQL is installed on the machine.\n" +
      "This is the folder in which you will find the \"postgresql.conf\" and \"pg_hba.conf\" files."),
    wTitle = "Help")

  /** Browse PostresSQL data dir and update field **/
  private def _browseDataDir() {
    val file = FxUtils.browseDirectory(
      dcTitle = "Select PostgreSQL data directory",
      dcInitialDir = dataDirField.text())

    val newPath = file.getPath()
    if (file != null) {
      dataDirField.text = newPath
    }
  }

  /** Get the selected directory **/
  def getPgDataDir(): String = dataDirField.text()

  /** Set a given path in the textfield **/
  def setPgDataDir(newPath: String) { dataDirField.text = newPath }
  /** Set a given path in the textfield **/
  def setPgDataDir(newPathOpt: Option[String]) { setPgDataDir(newPathOpt.getOrElse("")) }

  /** Check the form, return a boolean. Display or hide warnings depending on form conformity **/
  def checkForm(allowEmptyPaths: Boolean = true): Boolean = this.checkFolderFromField(dataDirField, warningLabel, allowEmptyPaths)

  /** Save PostgreSQL datadir in global variables and, if possible, in admin config file **/
  def saveForm(): Unit = {

    val newPath = dataDirField.text()
    val newPathIsEmpty = newPath.isEmpty()

    /* Update global variable */
    //  Main.postgresqlDataDir = newPath

    /* Store data dir in admin config if possible and needed */
    // val adminConfigFileOpt = None
    //    if (adminConfigFileOpt.isDefined) {
    //
    //      val adminConfigFile = adminConfigFileOpt.get
    //      val pathInAdminConfigOpt = adminConfigFile.getPostgreSqlDataDir()
    //
    //      if (newPathIsEmpty == false && (pathInAdminConfigOpt.isEmpty || pathInAdminConfigOpt.get != newPath)) {
    //        adminConfigFile.setPostgreSqlDataDir(newPath)
    //      }
    //    }

    /* Reset warning */
    warningLabel.visible = false

    /* Logback */
    val sb = new StringBuilder()
    sb ++= "[INFO]-- Configuration directory path --\n"

    if (newPathIsEmpty) sb ++= "[INFO]PostgreSQL data directory: undefined\n"
    else sb ++= "[INFO]PostgreSQL data directory @ " + newPath + "\n"

    sb ++= "[INFO]------------------"

    val msg = sb.result()
    println(msg) //for integrated console output
    logger.debug(msg)
  }
}