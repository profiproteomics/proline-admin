package fr.proline.admin.gui.util

import javafx.geometry.Rectangle2D
import javafx.stage.Screen
/**
 * builds initial window with preferred size
 */
object WindowSize {
  val primaryScreenBounds: Rectangle2D = Screen.getPrimary().getVisualBounds()
  val prefWitdh = primaryScreenBounds.getWidth() * 0.75
  val prefHeight = primaryScreenBounds.getHeight() * 0.75
}