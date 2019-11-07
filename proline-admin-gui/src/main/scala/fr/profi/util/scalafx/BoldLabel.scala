package fr.profi.util.scalafx

import scalafx.scene.control.Label

/** Label whose text is bold */
class BoldLabel(txt: String, upperCase: Boolean = true) extends Label {

  text = if (upperCase) txt.toUpperCase() else txt
  style = "-fx-font-weight:bold;"

  def setText(txt: String) {
    text = if (upperCase) txt.toUpperCase() else txt
  }
}