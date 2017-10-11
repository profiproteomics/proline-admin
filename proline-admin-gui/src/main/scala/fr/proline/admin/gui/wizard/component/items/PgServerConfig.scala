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
import scalafx.scene.layout.Priority
import scalafx.geometry.Pos

import java.io.File
import java.io.File.separator

import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.wizard.util._
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.wizard.component.Item
import fr.proline.admin.gui.wizard.component.items.pgserverconfig._
import fr.proline.admin.gui.wizard.component.items.pgserverconfig.tab.PostgresConfigContent
import fr.proline.admin.gui.wizard.component.items.pgserverconfig.tab.PgHbaConfigContent
import fr.proline.admin.gui.wizard.util.ItemName._

import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils._
/**
 * builds a panel with the postgreSQL properties: access rights and optimization
 *
 */

class PgServerConfig(val name: ItemName) extends Item with LazyLogging {
  /* postgreSQL panel components */
  val panelTitle = new Label("PostgreSQL Server Configuration") {
    styleClass = List("item")
  }
  val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    onAction = handle {
       _openUserGuide()
    }
  }

  val tabPane = new TabPane {
    prefWidth <== Wizard.configItemsPanel.width - 30
    prefHeight <== Wizard.stage.height - 30
  }

  /* access right tab */
  val workingFilePgHbaConf = Wizard.pgDataDirPath + File.separator + "pg_hba.conf"
  val pgHbaForm = new PgHbaConfigContent(workingFilePgHbaConf, Wizard.stage)
  val pgAccessRightTab = new Tab {
    text = "PG Access Right"
    content = pgHbaForm
    closable = false
  }

  /* optimization tab */
  val workingFilePostgresConf = Wizard.pgDataDirPath + File.separator + "postgresql.conf"
  val postgresForm = new PostgresConfigContent(workingFilePostgresConf, Wizard.stage)
  val pgOptimazationTab = new Tab {
    text = "PG Optimization"
    content = postgresForm
    closable = false
  }

  tabPane.tabs.addAll(pgOptimazationTab, pgAccessRightTab)
  /* Layout */
  alignment = Pos.Center
  alignmentInParent = Pos.Center
  prefHeight <== Wizard.stage.height - 50
  spacing = 1
  children = List(ScalaFxUtils.newVSpacer(minH = 20), new HBox {
    fillWidth = true
    children = Seq(new HBox {
      spacing = 15
      children = Seq(new HBox {
        children = Seq(FxUtils.newImageView(IconResource.SETTING))
      }, panelTitle)
    }, ScalaFxUtils.newHSpacer(minW = 45), new HBox {
      children = Seq(headerHelpIcon)
    })
  }, ScalaFxUtils.newVSpacer(minH = 10), tabPane)
}