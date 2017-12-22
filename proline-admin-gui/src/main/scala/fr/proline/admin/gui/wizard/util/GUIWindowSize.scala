package fr.proline.admin.gui.wizard.util

import javafx.geometry.Rectangle2D
import javafx.stage.Screen

object GUIWindowSize {
  
  val primaryScreenBounds: Rectangle2D = Screen.getPrimary().getVisualBounds()
  val prefWitdh = (primaryScreenBounds.getWidth() / 2)
  val prefHeight = (primaryScreenBounds.getHeight() / 1.15)
  
}