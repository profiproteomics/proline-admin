package fr.proline.admin.gui.component.configuration.tab

import scalafx.Includes._
import scalafx.stage.Stage

import fr.proline.admin.gui.component.configuration.form.IConfigFilesForm
import fr.proline.admin.gui.component.configuration.form.PgHbaConfigForm

/**
 * *************************************** *
 * Tab for the edition of pg_hba.conf file *
 * 2nd tab of ConfigurationTabbedWindow		 *
 * *************************************** *
 */
class PgHbaConfigTab()(implicit val parentStage: Stage) extends IConfigTab {

  lazy val name = "PostgreSQL authorizations"
  tooltip = "Edit pg_hba.conf file."

  def setContent(): Unit = {
    val newContent = new PgHbaConfigTabContent()
    _form = newContent.getForm()
    content = newContent
  }
}

/**
 * ******************************************** *
 * Content of PgHbaConfigTab, dynamically built *
 * ******************************************** *
 */
class PgHbaConfigTabContent()(implicit val parentStage: Stage) extends IPostgresConfigTabContent {

  protected implicit def _parentStage: Stage = parentStage
  
  protected def _workingFileName: String = "pg_hba.conf"

  protected def _newFormPanel(filePath: String): IConfigFilesForm = new PgHbaConfigForm(filePath)

  protected lazy val _pgTabType = PostgresTabType.AUTHORIZATIONS

  /* Try to display PgHbaConfigForm with initial config */
  this.init()
}