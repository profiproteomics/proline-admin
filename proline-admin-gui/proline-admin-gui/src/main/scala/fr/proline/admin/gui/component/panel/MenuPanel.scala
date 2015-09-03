package fr.proline.admin.gui.component.panel

import scalafx.Includes._
import scalafx.scene.control.Menu
import scalafx.scene.control.MenuBar
import scalafx.scene.control.MenuItem
import scalafx.scene.layout.Priority

import fr.proline.admin.gui.Main
import fr.proline.admin.gui.component.dialog.ProlineConfigForm
import fr.proline.admin.gui.component.dialog.SelectPostgresDataDirDialog
import fr.proline.admin.gui.component.dialog.SelectProlineConfigFilesDialog
import fr.proline.admin.gui.process.DatabaseConnection
import fr.proline.admin.gui.process.ProlineAdminConnection
import fr.proline.admin.gui.process.config.AdminConfigFile

/**
 *  Create the menu bar and its items.
 */
object MenuPanel {
  //TODO: remove me?

  def apply(): MenuBar = {
    new MenuBar {
      //id = "menuBar"
      hgrow = Priority.Always

      menus = List(

        new Menu("Menu") {
          style = "-fx-font-fill:white;"

          items = List(

            /* Change Proline configuration files selection (ProlineAdmine, Proline server = Core) */
            new MenuItem("Select configuration files") {
              onAction = handle {

                /* Open dialog for file selection */
                val saveAfterClose = SelectProlineConfigFilesDialog()

                /* Unless operation was canceled, check if config is valid, redirect to setup if not */
                if (saveAfterClose) {

                  val adminConfigOpt = new AdminConfigFile(Main.adminConfPath).read()
                  require(adminConfigOpt.isDefined, "admin config is undefined")
                  val adminConfig = adminConfigOpt.get

                  var isConfigValid = DatabaseConnection.testDbConnection(adminConfig, showPopup = false)
                  // if config is valid, try to load it
                  if (isConfigValid) isConfigValid = ProlineAdminConnection.loadProlineConf(verbose = false)
                  // if it's not, or made invalid by the previous step, open configuration form
                  if (isConfigValid == false) new ProlineConfigForm().showAndWait()
                }
              }
            },

            /* Change PostgreSQL data dir */
            new MenuItem("Select PostgreSQL data directory") {
              onAction = handle {
                SelectPostgresDataDirDialog()
              }
            },


            /** Refresh enabled/disabled buttons */
            new MenuItem("Refresh (DEBUG)") { //TODO: comment me
              onAction = handle {
                println("Refresh (DEBUG)")
                ProlineAdminConnection.loadProlineConf()

                //ButtonsPanel.computeButtonsAvailability()
                println("Refreshed")
              }
            },


            /** Exit Proline Admin */
            new MenuItem("Exit") {
              onAction = handle { Main.stage.close() }
            }
          )
        }
      )
    }

  }
}