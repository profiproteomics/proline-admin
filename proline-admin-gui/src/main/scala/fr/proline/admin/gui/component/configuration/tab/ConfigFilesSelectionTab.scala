package fr.proline.admin.gui.component.configuration.tab

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.layout.VBox
import scalafx.stage.Stage

import fr.proline.admin.gui.component.configuration.file._
import fr.proline.admin.gui.component.configuration.form.IConfigFilesForm
import fr.proline.admin.gui.process.config.AdminConfigFile

import fr.profi.util.scalafx.TitledBorderPane

/**
 * *************************************************************************** *
 * Tab for the selection of all editable configuration files from ProlineAdmin *
 * 1st tab of ConfigurationTabbedWindow																				 *
 * *************************************************************************** *
 */
class ConfigFilesSelectionTab()(implicit val parentStage: Stage) extends IConfigTab {

  lazy val name = "Configuration files"
  tooltip = "Select all configuration files at once, then edit then in the following tabs."
  
    def setContent(): Unit = {
    val newContent = new ConfigFilesSelectionTabContent()
    _form = newContent.getForm()
    content = newContent
  }
}

/**
 * ***************************************************** *
 * Content of ConfigFilesSelectionTab, dynamically built *
 * ***************************************************** *
 */
class ConfigFilesSelectionTabContent()(implicit parentStage: Stage) extends AbstractConfigTabContent with IConfigFilesForm {

  // Set AbstractConfigTabContent form
  form = this
  
  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */

  val prolineConfigFilesPanel = new ProlineConfigFilesPanel(onAdminConfigChange)
  val prolineConfigFiles = new TitledBorderPane(
    "Proline",
    prolineConfigFilesPanel
  )

  val postgresDataDirPanel = new PostgresDataDirPanel()
  val pgsqlDataDir = new TitledBorderPane(
    "PostgreSQL",
    postgresDataDirPanel
  )

  //TODO: enable me: 
  /*val seqRepoConfigFilePanel = new SeqRepoConfigFilePanel()
  val seqRepoConfigFile = new TitledBorderPane(
    "Protein sequences database (SeqRepo)",
    seqRepoConfigFilePanel
  )*/

  /*
   * ****** *
   * LAYOUT *
   * ****** *
   */

  setContentNode(
    new VBox {
      alignmentInParent = Pos.TopCenter
      spacing = 40
      content = Seq(
        prolineConfigFiles,
        pgsqlDataDir,
        //TODO: enable me: seqRepoConfigFile,
        wrappedApplyButton
      )
    }
  )
  
  this.bindSizeToParent(parentStage)

  /*
   * ******** *
   * FEATURES *
   * ******** *
   */
  
  /** Try to get all conf files paths from ProlineAdmin conf file **/
  def onAdminConfigChange(adminConfigFile: AdminConfigFile): Unit = {
    postgresDataDirPanel.setPgDataDir(adminConfigFile.getPostgreSqlDataDir())
    //TODO: enable me: seqRepoConfigFilePanel.setSeqRepoConfFile(adminConfigFile.getSeqRepoConfigPath())
  }

  /** Check the conformity of the form, handle warning labels (text/style/hide) **/
  def checkForm(): Boolean = prolineConfigFilesPanel.checkForm() && postgresDataDirPanel.checkForm()
  //TODO: enable me: def checkForm(): Boolean = prolineConfigFilesPanel.checkForm() && postgresDataDirPanel.checkForm() && seqRepoConfigFilePanel.checkForm()

  /** Register all paths in global variables and config file **/
  def saveForm(): Unit = if (checkForm()) {
    prolineConfigFilesPanel.saveForm()
    postgresDataDirPanel.saveForm()
    //TODO: enable me: seqRepoConfigFilePanel.saveForm()
  }

}