package fr.proline.admin.gui.component.configuration.tab

import scalafx.Includes._
import scalafx.stage.Stage

import fr.proline.admin.gui.Main
import fr.proline.admin.gui.component.configuration.file.SeqRepoConfigFilePanel
import fr.proline.admin.gui.component.configuration.form.SeqRepoConfigForm

/**
 * ******************************************************** *
 * Tab for the edition of the SeqRepo configuration file 		*
 * 5th tab of ConfigurationTabbedWindow									  	*
 * ******************************************************** *
 */
class SeqRepoConfigTab()(implicit val parentStage: Stage) extends IConfigTab {

  lazy val name = "Protein sequences database"
  tooltip = "Edit the SeqRepo config file."

  def setContent(): Unit = {
    val newContent = new SeqRepoConfigTabContent()
    _form = newContent.getForm()
    content = newContent
  }
}

/**
 * ********************************************** *
 * Content of SeqRepoConfigTab, dynamically built *
 * ********************************************** *
 */
class SeqRepoConfigTabContent()(implicit val parentStage: Stage) extends IConfigTabContent {

  /* Config files panel */
  protected lazy val configFilesPanel = new SeqRepoConfigFilePanel()
  protected lazy val configFilesPanelTitle = "Working file"

  /* Init. form if possible */
  this.init()

  /** Initialize form panel if possible **/
  protected def init(): Unit = {
    onApplyWorkingFilesPressed()
  }

  /** Save the config and load corresponding form (applyWorkingFileButton pressed) **/
  def onApplyWorkingFilesPressed(): Unit = {

    /* Enable (resp. disable) 'Apply' button if form is corect (resp. incorrect) */
    if (configFilesPanel.checkForm() == false) {
      applyWorkingFileButton.disable = true
    } else {
      applyWorkingFileButton.disable = false

      /* Save config and build new config form */
      configFilesPanel.saveForm()
      form = new SeqRepoConfigForm()
      this.setContentNode(getNodeForScrollPane())
    }
  }

  /** Set last saved config path (cancelButton pressed) **/
  protected def onCancelWorkingFilesPressed(): Unit = {
    // Set last saved value in field
    configFilesPanel.setSeqRepoConfFile(Main.seqRepoConfPath)
  }
}