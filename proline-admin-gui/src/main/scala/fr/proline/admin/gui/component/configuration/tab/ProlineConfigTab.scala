package fr.proline.admin.gui.component.configuration.tab

import scalafx.Includes._
import scalafx.stage.Stage

import fr.proline.admin.gui.Main
import fr.proline.admin.gui.component.configuration.file.ProlineConfigFilesPanel
import fr.proline.admin.gui.component.configuration.form.ProlineConfigForm

/**
 * ****************************************************** *
 * Tab for the edition of Proline configuration files: 		*
 * ProlineAdmin, Proline server, and PWX application.conf *
 * 4th tab of ConfigurationTabbedWindow									  *
 * ****************************************************** *
 */
class ProlineConfigTab()(implicit val parentStage: Stage) extends IConfigTab {

  lazy val name = "Proline configuration"
  tooltip = "Edit ProlineAdmin configuration file, and optionally Proline server and PWX configuration files."

  def setContent(): Unit = {
    val newContent = new ProlineConfigTabContent()
    _form = newContent.getForm()
    content = newContent
  }
}

/**
 * ********************************************** *
 * Content of ProlineConfigTab, dynamically built *
 * ********************************************** *
 */
class ProlineConfigTabContent()(implicit val parentStage: Stage) extends IConfigTabContent {

  /* Config files panel */
  protected lazy val configFilesPanel = new ProlineConfigFilesPanel()
  protected lazy val configFilesPanelTitle = "Working files"

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
      form = new ProlineConfigForm()
      this.setContentNode(getNodeForScrollPane())
    }
  }

  /** Set last saved config path (cancelButton pressed) **/
  protected def onCancelWorkingFilesPressed(): Unit = {
    // Set last saved values in fields
    configFilesPanel.setProlineAdminConfFile(Main.adminConfPath)
    configFilesPanel.setProlineServerConfFile(Main.serverConfPath)
    configFilesPanel.setPwxConfFile(Main.pwxConfPath)
  }
}