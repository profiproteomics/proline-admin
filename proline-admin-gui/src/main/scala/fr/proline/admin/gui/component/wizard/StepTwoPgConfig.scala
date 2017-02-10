package fr.proline.admin.gui.component.wizard

import scalafx.Includes._
import scalafx.stage.Stage

import fr.proline.admin.gui.component.configuration.form.IConfigFilesForm
import fr.proline.admin.gui.component.configuration.form.PgHbaConfigForm
import fr.proline.admin.gui.component.configuration.tab.IConfigTab

class StepTwoPgConfig()(implicit val parentStage: Stage) extends IConfigTab {

  lazy val name = "PostgreSQL authorizations"
  tooltip = "Edit pg_hba.conf file."

  def setContent(): Unit = {
    val newContent = new StepTwoPgConfigContent()
    _form = newContent.getForm()
    content = newContent
  }
}

/**
 * ******************************************** *
 * Content of HbaConfig, dynamically built *
 * ******************************************** *
 */
class StepTwoPgConfigContent()(implicit val parentStage: Stage) extends StepTwoAbstractConfig {

  protected implicit def _parentStage: Stage = parentStage

  protected def _workingFileName: String = "pg_hba.conf"

  protected def _newFormPanel(filePath: String): IConfigFilesForm = new PgHbaConfigForm(filePath)

  protected lazy val _pgTabType = PostgresTabType.AUTHORIZATIONS

  /* Try to display PgHbaConfigForm with initial config */
  this.init()
}