package fr.proline.admin.gui.component.configuration.file

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.stage.FileChooser.ExtensionFilter

import fr.proline.admin.gui.Main
import fr.proline.admin.gui.util.FxUtils

import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

/**
 * *************************************************************** *
 * Create a panel to select Proline configuration files. 					 *
 * Includes ProlineAdmin, Proline server, and PWX config files.    *
 * *************************************************************** *
 */
class SeqRepoConfigFilePanel() extends HBox with IConfigFilesPanel with LazyLogging {
//class SeqRepoConfigFilePanel(onSelectionChange: String => Unit = null)(implicit val parentStage: Stage) extends VBox with IConfigFilesPanel {

  val headerLabel = new Label("Full path to SeqRepo configuration file :")

  //  val headerHelpIcon = new Hyperlink {
  //    graphic = FxUtils.newImageView(IconResource.HELP)
  //    onAction = handle { _openHelpDialog() }
  //  }

  val configField = new TextField {
    hgrow = Priority.Always
    //    text.onChange { (_, oldString, newString) => 
    //      onSelectionChange(newString)
    //      say it's been changed  
    //    }
  }

  val browseButton = new Button("Browse...") {
    onAction = handle { _browseFile() }
  }

  val warningLabel = new Label() {
    visible = false
    style = TextStyle.RED_ITALIC
  }

  /** Organize and render **/
  spacing = 5
  children = List(headerLabel, configField, browseButton)

  /*
   * ******** *
   * FEATURES *
   * ******** *
   */

  /** Get the selected directory **/
  def getSeqRepoConfFile(): String = configField.text()
  def setSeqRepoConfFile(newPath: String) { configField.text = newPath }
  def setSeqRepoConfFile(newPathOpt: Option[String]) { setSeqRepoConfFile(newPathOpt.getOrElse("")) }

  //  /** Open a popup to help user understand whant the PostgreSQL data dir is **/
  //  private def _openHelpDialog() = ShowPopupWindow(
  //    wTitle = "Help",
  //    wText = "The PostgreSQL data directory is defined when PostgreSQL is installed on the machine.\n" +
  //      "This is the folder in which you will find the \"postgresql.conf\" and \"pg_hba.conf\" files."
  //  )

  /** Browse PostresSQL data dir and update field **/
  private def _browseFile() {
    val file = FxUtils.browseFile(
      fcTitle = "Select SeqRepo configuration file",
      fcInitialDir = configField.text(),
      fcExtFilters = Array(new ExtensionFilter("Configuration files", "*.conf"))
    )

    val newPath = file.getPath()
    if (file != null) {
      configField.text = newPath
    }
  }

  /** Check the form, return a boolean. Display or hide warnings depending on form conformity **/
  def checkForm(allowEmptyPaths: Boolean = true): Boolean = this.checkFileFromField(configField, warningLabel, allowEmptyPaths, Some("conf"))

  /** Save SeqRepo conf path in global variables and, if possible, in admin config file **/
  def saveForm(): Unit = {
    
    val newPath = configField.text()
    val newPathIsEmpty = newPath.isEmpty()
    
    /* Update global variable */
    Main.seqRepoConfPath = newPath

    /* Store data dir in admin config if possible and needed */
    val adminConfigFileOpt = Main.getAdminConfigFile()
    if (adminConfigFileOpt.isDefined) {

      val adminConfigFile = adminConfigFileOpt.get
      val pathInAdminConfigOpt = adminConfigFile.getSeqRepoConfigPath()

      if (newPathIsEmpty == false &&
        (pathInAdminConfigOpt.isEmpty || pathInAdminConfigOpt.get != newPath)
      ) {
        adminConfigFile.setSeqRepoConfigPath(newPath)
      }
    }
    
    /* Reset warning */
    warningLabel.visible = false
    
    /* Logback */
    val sb = new StringBuilder()
    sb ++= "[INFO]-- Configuration file path --\n"

    if (newPathIsEmpty) sb ++= "[INFO]SeqRepo: undefined\n"
    else sb ++= "[INFO]SeqRepo @ " + newPath + "\n"

    sb ++= "[INFO]------------------"

    val msg = sb.result()
    println(msg) //for integrated console output
    logger.debug(msg)
  }
}