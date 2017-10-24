package fr.proline.admin.gui.wizard.component.items.file
import scalafx.scene.control.Label
import fr.proline.repository.DriverType
import fr.proline.admin.gui.process.DatabaseConnection
import fr.profi.util.scala.ScalaUtils

trait IPostgres {
  var userName, passWord, hostName: String = ""
  var port: Int = 5432
  val driver = DriverType.POSTGRESQL
  val hostLabel = new Label("Host: ")
  val portLabel = new Label("Port: ")
  val userLabel = new Label("User: ")
  val passWordLabel = new Label("Password: ")

  /** test connection to database server */
  def _testDbConnection(
    showSuccessPopup: Boolean = false,
    showFailurePopup: Boolean = false): Boolean = {
    DatabaseConnection.testDbConnectionToWizard(driver, userName, passWord, hostName, port, showSuccessPopup, showFailurePopup)
  }

  /** get database connection */
  def getInfos: String = {
    if (DatabaseConnection.testDbConnectionToWizard(driver, userName, passWord, hostName, port, false, false))
      s"""PostgreSQL: OK""" else s"""PostgreSQL: NOK"""
  }
}