package fr.proline.admin.gui.component.panel

import fr.proline.admin.gui.Main
import fr.proline.admin.gui.component.dialog.ConfFileChooser

import scalafx.Includes.handle
import scalafx.scene.control.Menu
import scalafx.scene.control.MenuBar
import scalafx.scene.control.MenuItem
import scalafx.scene.layout.Priority

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
            new MenuItem("Refresh") {
              onAction = handle { ButtonsPanel.updateBooleans() }
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