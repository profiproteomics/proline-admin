package fr.proline.admin.gui.wizard.component.items

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.scene.layout.VBox
import scalafx.scene.layout.HBox
import scalafx.scene.control.Tab
import scalafx.scene.control.TabPane
import scalafx.scene.control.Label
import fr.profi.util.scalafx.BoldLabel
import scalafx.scene.control.Hyperlink
import javafx.scene.layout.Priority
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils._
import scalafx.geometry.Pos
import scalafx.scene.text.{ Font, FontWeight, Text }
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.wizard.util._
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.wizard.component.items.serverconfig.tab._
import fr.proline.admin.gui.wizard.component.Item
import fr.proline.admin.gui.wizard.component.items.pgserverconfig._
import fr.proline.admin.gui.wizard.component.items.pgserverconfig.tab.PostgresConfigContentTab
import fr.proline.admin.gui.wizard.component.items.pgserverconfig.tab.PgHbaConfigContentTab

/**
 * Panel Edit/Update to postgreSQL access rights
 */

class PgServerConfig(val name: String) extends Item with LazyLogging {
  /**
   * component
   */
  val panelTitle = new Label("PostgreSQL Server Configuration") {
    font = Font.font("SanSerif", FontWeight.Bold, 14)
  }
  val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    onAction = handle {
      _openHelpDialog()
    }
  }

  val tabPane = new TabPane {
    prefWidth <== Wizard.configItemsPanel.width - 30
    prefHeight <== Wizard.stage.height - 30
  }

  //tab of right access to PostgreSQL 

  val pgHbaForm = new PgHbaConfigContentTab()
  val pgAccessRightTab = new Tab {
    text = "PG Access Right"
    content = pgHbaForm
    closable = false
  }

  //tab of optimization of PostgreSQL  

  val postgresForm = new PostgresConfigContentTab()
  val pgOptimazationTab = new Tab {
    text = "PG Optimization"
    content = postgresForm
    closable = false
  }

  tabPane.tabs.addAll(pgOptimazationTab, pgAccessRightTab)

  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = 1
  content = List(ScalaFxUtils.newVSpacer(minH = 20), new HBox {
    fillWidth = true
    content = Seq(new HBox {
      alignment = Pos.TOP_LEFT
      content = Seq(panelTitle)
    }, ScalaFxUtils.newHSpacer(minW = 60), new HBox {
      alignment = Pos.TOP_RIGHT
      content = Seq(headerHelpIcon)
    })
  }, ScalaFxUtils.newVSpacer(minH = 10), tabPane)

  // help 
  val helpTextBuilder = new StringBuilder("PostgreSQL optimization:\n\n\tThis tab contains: PostgreSQL server parameters.")
    .append("\n\tThe complete list of parameter names and allowed values can be found in the PostgreSQL documentation.\n\n")
    .append("PostgreSQL access right:\n\n\tThis tab controls: which hosts are allowed to connect, ")
    .append("how clients are authenticated,\n\t which PostgreSQL user names they can use, which databases they can access.\n")
  def _openHelpDialog() = PopupHelpWindow(
    wTitle = "Help",
    wText = helpTextBuilder.toString)

}