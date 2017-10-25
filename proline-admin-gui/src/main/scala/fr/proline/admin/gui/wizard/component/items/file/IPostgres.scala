package fr.proline.admin.gui.wizard.component.items.file
import scalafx.scene.control.Label
import scalafx.scene.control.Button
import scalafx.scene.control.TextField
import fr.profi.util.scalafx.TitledBorderPane

trait IPostgres {
  
  val hostLabel = new Label("Host: ")
  val portLabel = new Label("Port: ")
  val userLabel = new Label("User: ")
  val passWordLabel = new Label("Password: ")

  val hostField: TextField
  val portField: TextField
  val userField: TextField
  val passwordTextField: TextField
  val testConnectionButton: Button
  val dbConnectionSettingPane: TitledBorderPane
  
}