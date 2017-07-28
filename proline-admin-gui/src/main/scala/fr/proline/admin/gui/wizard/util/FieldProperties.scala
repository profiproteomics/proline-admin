package fr.proline.admin.gui.wizard.util

import scalafx.scene.Node
import scalafx.scene.text.{ Font, FontWeight, Text }
import fr.proline.admin.gui.Wizard

/*
 * edit field Style 
 */

object FieldProperties {

  def setBorder(field: Node) {
    field.setStyle("-fx-text-box-border: red  ; -fx-focus-color: red ;")
  }
  def removeBorder(field: Node) {
    field.setStyle("")
  }
}