package fr.proline.admin.gui.component.configuration.form

import scalafx.Includes.handle
import scalafx.geometry.Pos
import scalafx.scene.control.Button
import scalafx.scene.layout.StackPane
import scalafx.scene.Node


/**
 * ****************************** *
 * Trait for each tab 'FormPanel' *
 * ****************************** *
 */
trait IConfigFilesForm extends Node {

  /* Apply utilities */
  val applyButton = new Button("Apply") {
    onAction = handle {
    
      try{
      if (checkForm()) saveForm()
      }
      catch {
        case ade :java.nio.file.AccessDeniedException  => System.out.println("[Error] - Access denied, you should have administrator rights to write in configuration files")
      }
    }
  }

  val wrappedApplyButton = new StackPane {
    content = applyButton
    alignment = Pos.BottomRight
  }

  /** Check if the form is correct **/
  def checkForm(): Boolean

  /** Save the form **/
  def saveForm(): Unit
}