package fr.proline.admin.gui.component.configuration.tab

import scalafx.Includes._
import scalafx.stage.Stage

import fr.proline.admin.gui.component.configuration.form.IConfigFilesForm
import fr.proline.admin.gui.component.configuration.form.PostgresConfigForm

/**
 * ***************************************** *
 * Tab for the edition of postgres.conf file *
 * 3rd tab of ConfigurationTabbedWindow			 *
 * ***************************************** *
 */
class PostgresConfigTab()(implicit val parentStage: Stage) extends IConfigTab {

  lazy val name = "PostgreSQL optimization"
  tooltip = "Edit postgres.conf file."

  def setContent(): Unit = {
    val newContent = new PostgresConfigTabContent()
    _form = newContent.getForm()
    content = newContent
  }
}

/**
 * *********************************************** *
 * Content of PostgresConfigTab, dynamically built *
 * *********************************************** *
 */
class PostgresConfigTabContent()(implicit val parentStage: Stage) extends IPostgresConfigTabContent {
  
  protected implicit def _parentStage: Stage = parentStage
  
  protected def _workingFileName: String = "postgresql.conf"

  protected def _newFormPanel(filePath: String): IConfigFilesForm = {
    val formPanel = new PostgresConfigForm(filePath)(parentStage)
    formPanel.prefWidth <== this.width
    formPanel
  }
  
  protected lazy val _pgTabType = PostgresTabType.OPTIMIZATION

  /* Try to display PostgresConfigForm with initial config */
  this.init()
}