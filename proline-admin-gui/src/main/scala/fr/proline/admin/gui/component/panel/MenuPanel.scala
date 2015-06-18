package fr.proline.admin.gui.component.panel

import scalafx.Includes._
import scalafx.scene.control.Menu
import scalafx.scene.control.MenuBar
import scalafx.scene.control.MenuItem
import scalafx.scene.layout.Priority

import fr.proline.admin.gui.Main
import fr.proline.admin.gui.component.dialog.ConfFileChooser
import fr.proline.admin.gui.process.ProlineAdminConnection

/**
 *  Create the menu bar and its items.
 */
object MenuPanel {
  //TODO: remove me?

  def apply(): MenuBar = {
    new MenuBar {
      //id = "menuBar"
      hgrow = Priority.ALWAYS

      menus = List(

        new Menu("Menu") {
          style = "-fx-font-fill:white;"

          items = List(

            /** Change application file selection */
            new MenuItem("Select configuration file") {
              onAction = handle { ConfFileChooser.showIn(Main.stage) }
            },


            /** Refresh enabled/disabled buttons */
            new MenuItem("Refresh (DEBUG)") { //TODO: comment me
              onAction = handle {
                println("Refresh (DEBUG)")
                ProlineAdminConnection.loadProlineConf()

                //                ButtonsPanel.computeButtonsAvailability()
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