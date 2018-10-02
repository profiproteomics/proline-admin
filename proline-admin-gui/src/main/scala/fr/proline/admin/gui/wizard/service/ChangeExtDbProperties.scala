package fr.proline.admin.gui.wizard.service

import javafx.{ concurrent => jfxc }
import scalafx.concurrent.Service
import scalafx.stage.Stage
import scala.util.{ Try, Success }

import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.service.user.{ ChangeExtDbProperties => ChangeDbProp }
import fr.proline.admin.gui.wizard.monitor.component.ChangeExtDbPropDialog
import fr.proline.admin.gui.wizard.monitor.component.DatabasesPanel
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

/**
 * Service to change the connection properties of a external database
 * @author aromdhani
 *
 * @param extDbId The external database(s) id.
 * @param host The host name.
 * @param port The port number.
 * @param stage Specify the parent stage of this dialog.
 *
 */

class ChangeExtDbProperties(extDbIdSet: Set[Long], host: String, port: Int, stage: ChangeExtDbPropDialog) extends Service(new jfxc.Service[Boolean]() {
  protected def createTask(): jfxc.Task[Boolean] = new jfxc.Task[Boolean] {
    protected def call(): Boolean = {
      val udsDbContext = UdsRepository.getUdsDbContext()
      val changeDbProps = new ChangeDbProp(udsDbContext, extDbIdSet, host, port)
      changeDbProps.run()
      changeDbProps.isSuccess
    }
    override def running(): Unit = {
      stage.informationLabel.setStyle(TextStyle.BLUE_ITALIC)
      stage.informationLabel.setText(s"Changing the external database properties in progress, please wait...")
      stage.progressBar.visible_=(true)
      stage.informationLabel.visible_=(true)
      stage.progressBar.progress_=(this.getProgress)
      stage.dbPropertiesPanel.disable_=(true)
      stage.changeButton.disable_=(true)
      stage.exitButton.disable_=(true)
    }
    override def succeeded(): Unit = {
      val isChangedExtDbProps = this.get
      if (isChangedExtDbProps) {
        stage.informationLabel.setStyle(TextStyle.GREEN_ITALIC)
        stage.informationLabel.setText(s"The external database properties have been changed successfully!")
        stage.progressBar.progress_=(100)
        stage.dbPropertiesPanel.disable_=(false)
        stage.changeButton.disable_=(false)
        stage.exitButton.disable_=(false)
        DatabasesPanel.refreshTableView()
      } else {
        stage.informationLabel.setStyle(TextStyle.RED_ITALIC)
        stage.informationLabel.setText("Error while trying to change external_db properties!")
        stage.informationLabel.visible_=(true)
        stage.progressBar.visible_=(false)
        stage.dbPropertiesPanel.disable_=(false)
        stage.changeButton.disable_=(false)
        stage.exitButton.disable_=(false)
      }
      DatabasesPanel.externalDbTable.selectedItems.clear
    }
  }
})

object ChangeExtDbProperties {
  /**
   * @param extDbId The external database(s) id.
   * @param host The host name.
   * @param port The port number.
   * @param stage Specify the parent stage of this dialog.
   */
  def apply(extDbIdSet: Set[Long], host: String, port: Int = 5432, stage: ChangeExtDbPropDialog): ChangeExtDbProperties = {
    new ChangeExtDbProperties(extDbIdSet, host, port, stage)
  }
}