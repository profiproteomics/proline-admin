package fr.proline.admin.gui.wizard.monitor.component
/**
 * trait for Monitor dialogs
 * 
 */
trait IMonitorForm {
  def checkFields(): Boolean
  def validate(): Unit
  def exit(): Unit
}