package fr.proline.admin.gui.install.view.postgres

import scalafx.Includes._
import scalafx.geometry.{ Insets, Pos }
import scalafx.stage.Stage
import scalafx.scene.control.Tab
import scalafx.scene.control.TabPane
import scalafx.scene.control.Label
import scalafx.scene.layout.{ Priority, VBox, HBox }

import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.Install
import fr.proline.admin.gui.install.model.PostgresModelView
import fr.proline.admin.gui.component.configuration.form._
import fr.proline.admin.gui.util.FxUtils
import fr.profi.util.scalafx.CustomScrollPane

import scala.collection.Seq

/**
 * Creates and displays a scroll pane with postgresql.conf parameters.
 * @author aromdhani
 *
 */

class PostgresConfigPanel(model: PostgresModelView)(implicit val parentStage: Stage) extends CustomScrollPane {
  private val postgresFilePath = model.postgresqlFilePath
  private val postgresConfigForm = new PostgresConfigForm(postgresFilePath)(parentStage)
  postgresConfigForm.applyButton.visible = false
  setContentNode(
    new VBox {
      prefWidth <== parentStage.width - 70
      prefHeight <== parentStage.height - 45
      padding = Insets(5, 5, 5, 5)
      children = List(postgresConfigForm)
    })

  /** Save postgesql.conf new settings */
  def saveForm() {
    postgresConfigForm.saveForm()
  }
}