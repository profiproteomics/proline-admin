package fr.proline.admin.gui.install.model

import com.typesafe.scalalogging.LazyLogging
import fr.profi.util.StringUtils
import fr.proline.admin.gui.Install
import fr.proline.admin.gui.component.configuration.file.ProlineConfigFileChooser
import fr.proline.admin.gui.process.config._
import fr.proline.admin.gui.util.{AdminGuide, ExitPopup}
import fr.proline.repository.DriverType
import scalafx.scene.control.{CheckBox, Label, TextField}

import java.io.File

/**
 * The view model home panel. Defines UI actions: select Proline items and load configuration files.
 *
 * @author aromdhani
 *
 */

class HomePanelViewModel(adminConfigFilePath: String) extends LazyLogging {

  /** Read Proline-Admin configuration file */
  def adminConfigOpt(): Option[AdminConfig] = {
    try {
      val adminConf = new AdminConfigFile(adminConfigFilePath)
      adminConf.read()
    } catch {
      case ex: Exception =>
        logger.error("Error while trying to read Proline-Admin GUI configuration file", ex.getMessage())
        None
    }
  }

  /** Browse server configuration file */
  def browseServerConfigFile(path: String, txtField: TextField) {
    try {
      ProlineConfigFileChooser.setForProlineServerConf(path)
      val filePath = ProlineConfigFileChooser.showIn(Install.stage)
      if (filePath != null) txtField.text = filePath
    } catch {
      case ex: Exception => logger.error("Error while trying to browse server configuration file", ex.getMessage())
    }
  }

  /** Browse sequence Repository configuration file */
  def browseSeqReposConfigFile(path: String, txtField: TextField) {
    try {
      ProlineConfigFileChooser.setForSeqReposConf(path)
      val filePath = ProlineConfigFileChooser.showIn(Install.stage)
      if (filePath != null) txtField.text = filePath
    } catch {
      case ex: Exception => logger.error("Error while trying to browse sequence repository configuration file", ex.getMessage())
    }
  }

  /** Browse PWX configuration file */
//  def browsePwxConfigFile(path: String, txtField: TextField) {
//    try {
//      ProlineConfigFileChooser.setForPwxConf(path)
//      val filePath = ProlineConfigFileChooser.showIn(Install.stage)
//      if (filePath != null) txtField.text = filePath
//    } catch {
//      case ex: Exception => logger.error("Error while trying to browse PWX configuration file", ex.getMessage)
//    }
//  }


  /** Reset and write default Proline Admin configuration values */
  def defaultAdminConfig(): Option[AdminConfig] = {
    val defaultAdminConfig = AdminConfig(
      adminConfigFilePath,
      Some(""),
      Some(""),
      Some(""),
      Some(DriverType.POSTGRESQL),
      Some("<path/to/proline/data>"),
      Some("<db_user>"),
      Some("<db_password>"),
      Some("<db_host>"),
      Some(5432))
    new AdminConfigFile(adminConfigFilePath).write(defaultAdminConfig)
    Option(defaultAdminConfig)
  }

  /** Check configuration file path */
  def isConfFile(path: String): Boolean = {
    if (!StringUtils.isEmpty(path) && new File(path).exists && new File(path).getName == """application.conf""") true else false
  }

  /** Check selected configuration item and show notification */
  def isValidConfigItem(chBox: CheckBox, txtField: TextField, label: Label): Boolean = {
    var isValidItem = if (chBox.isSelected) {
      if (isConfFile(txtField.text())) { label.visible = false; true }
      else { label.visible = true; false }
    } else {
      label.visible = false;
      false
    }
    isValidItem
  }


  /** Open Proline-Admin GUI guide */
  def openAdminGuide() {
    AdminGuide.openUrl(Install.targetPath + File.separator + "classes" + File.separator + "documentation" + File.separator + "Proline_AdminGuide_2.0.pdf")(Install.stage)
  }

  /** Exit and close Proline-Admin GUI application */
  def exit() {
    ExitPopup("Exit", "Are you sure you want to exit Proline-Admin-GUI Install?", Some(Install.stage), false)
  }
}

