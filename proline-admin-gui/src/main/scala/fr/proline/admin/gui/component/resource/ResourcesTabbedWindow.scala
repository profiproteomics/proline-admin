package fr.proline.admin.gui.component.resource

import scalafx.Includes._
import scalafx.scene.control.Tab
import scalafx.scene.control.Tooltip.stringToTooltip

import fr.proline.admin.gui.component.configuration.tab._
import fr.proline.admin.gui.component.resource._
import fr.proline.admin.gui.util.AbstractTabbedWindow
import src.main.scala.fr.proline.admin.gui.component.resource._
/**
 * ********************************************************************************* *
 * Tabbed window for the management of Proline resources: users, projects, PTMs, ... *
 * ********************************************************************************* *
 */
class ResourcesTabbedWindow() extends AbstractTabbedWindow {

  /* Stage's properties */
  title = s"Resource management"

  val tabs: Seq[IResourceManagementTab] = Seq(
      new UsersTab(),
      new ProjectsTab()
      //TODO: new DataBasesTab()
      //TODO: enable me: new PtmsTab(),
      //TODO: enable me: new InstruConfigTab()
  )
  tabPanel.tabs = tabs

  /** Get this tabbed window **/
//  def apply() = this
  
  /** Simply close the widonw on 'OK' pressed **/
  protected def runOnOkPressed() {
    thisWindow.close()
  }
}