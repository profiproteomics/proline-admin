package fr.proline.admin.gui.wizard.util

import javafx.geometry.Rectangle2D
import javafx.stage.Screen

object GUIWindowSize {
  val primaryScreenBounds: Rectangle2D = Screen.getPrimary().getVisualBounds()
  var prefWitdh = (primaryScreenBounds.getWidth() / 2)
  var prefHeight = (primaryScreenBounds.getHeight() / 1.5)
}