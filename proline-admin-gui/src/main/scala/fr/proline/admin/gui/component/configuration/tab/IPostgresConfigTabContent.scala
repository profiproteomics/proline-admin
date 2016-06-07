package fr.proline.admin.gui.component.configuration.tab

import java.io.File
import scalafx.Includes._
import scalafx.scene.control.Label
import fr.proline.admin.gui.Main
import fr.proline.admin.gui.component.configuration.file.PostgresDataDirPanel
import fr.proline.admin.gui.component.configuration.form.IConfigFilesForm
import fr.profi.util.StringUtils
import fr.profi.util.scala.ScalaUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle
import scalafx.scene.layout.VBox
import scalafx.scene.layout.StackPane
import scalafx.geometry.Pos

/**
 * ************************************* *
 * List all types of Postges config tabs *
 * ************************************* *
 **/
object PostgresTabType extends Enumeration{
  val AUTHORIZATIONS, OPTIMIZATION = Value
}

/**
 * ***************************************************************** *
 * Common content of PgHba- and PostgresConfigTab, dynamically built *
 * ***************************************************************** *
 */
trait IPostgresConfigTabContent extends IConfigTabContent {

  /* Define in each implementation */
  protected def _workingFileName: String
  protected def _newFormPanel(filePath: String): IConfigFilesForm
  protected def _pgTabType: PostgresTabType.Value

  /* Stage properties */
  private var currentFilePath: String = _
  private val WORKING_FILE = "Working file: "
  private val NO_WORKING_FILE = "No working file identified. Please select your PostgreSQL data directory."
  private val INEXISTING_WORKING_FILE = "This file doesn't exist. Make sure the selected PostgreSQL data directory is correct." +
    s"\nYou would also make sure it doesn't affect the 'PostgreSQL ${if (_pgTabType == PostgresTabType.AUTHORIZATIONS) "optimization" else "authorizations"}' tab."

  /* File browsing */
  protected lazy val configFilesPanel = new PostgresDataDirPanel(_updateWorkingFile)
  protected lazy val configFilesPanelTitle = "Working file"

  val workingFileLabel = new Label()
  val workingFileWarning = new Label() {
    visible = false
    style = TextStyle.RED_ITALIC
  }

  /* Layout */
  val warningsAsVBox = new VBox {
    content = Seq(workingFileLabel, workingFileWarning)
  }


  /* IConfigTabContent methods (implement) */

  /** Try to display ConfigForm with initial config **/
  protected def init() {
    /* Add warnings to default components */
    setConfigFilePanelEnhancedContent(warningsAsVBox)
    
    /* Update warnings */
    _updateWorkingFile(configFilesPanel.getPgDataDir())
    onApplyWorkingFilesPressed()
  }

  /** Save the config and load corresponding form  (applyWorkingFileButton pressed) **/
  def onApplyWorkingFilesPressed(): Unit = {
    // No need to check because it is handled by _updateWorkingFile
    configFilesPanel.saveForm()
    if (ScalaUtils.isEmpty(configFilesPanel.getPgDataDir()) == false) _buildFormPanel()
  }

  /** Set last saved config path  (cancelButton pressed)**/
  protected def onCancelWorkingFilesPressed(): Unit = {
    // Set last saved value in field
    configFilesPanel.setPgDataDir(Main.postgresqlDataDir)
  }

  /* Private methods */
  /** Update working file when PGSQL data dir is changed **/
  private def _updateWorkingFile(newDirPath: String): Boolean = {
    val dirPathIsEmpty = StringUtils.isEmpty(newDirPath)

    if (dirPathIsEmpty) {
      workingFileWarning.text = NO_WORKING_FILE

      workingFileLabel.visible = false
      workingFileWarning.visible = true
      applyWorkingFileButton.disable = true
    } else {
      currentFilePath = newDirPath + "/" + _workingFileName
      val fileExists = new File(currentFilePath).exists()

      workingFileLabel.text = WORKING_FILE + currentFilePath
      if (!fileExists) workingFileWarning.text = INEXISTING_WORKING_FILE

      workingFileLabel.visible = true
      workingFileWarning.visible = !fileExists
      applyWorkingFileButton.disable = !fileExists
    }

    dirPathIsEmpty
  }

  /** Build and display the form panel **/
  private def _buildFormPanel() {
    form = _newFormPanel(currentFilePath)
    this.setContentNode(getNodeForScrollPane())
  }
}