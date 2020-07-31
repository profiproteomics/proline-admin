package fr.proline.admin.gui.install

import java.io.File
import java.util.Date

import com.typesafe.scalalogging.LazyLogging
import fr.profi.util.scala.ScalaUtils
import fr.proline.admin.gui.process.config.{AdminConfig, AdminConfigFile}
import org.junit.{Before, Test}

class InstallTest extends LazyLogging {
  var tmpDir, serverDir, dataDir, confFile: File = null

  @Before
  def createProlineDir(): Unit = {
    val now = new Date()
    val tmpDirName = String.format("temp_%tY%<tm%<td%<tH%<tM%<tS_%<tL", now)
    tmpDir = new File(tmpDirName)
    tmpDir.mkdir()
    serverDir = new File((tmpDir.getAbsolutePath) + File.separator + "server")
    dataDir = new File((tmpDir.getAbsolutePath) + File.separator + "data")
    confFile = new File(serverDir + File.separator + "config" + File.separator + "application.conf")
    tmpDir.deleteOnExit()
  }

  @Test
  def testIsValidFile() {
    ScalaUtils.isConfFile(confFile.getAbsolutePath)
  }

  @Test
  def writeAdminConfFile() {
    val adminConfig = new AdminConfigFile(confFile.getAbsolutePath)
    val adminConfigModel = AdminConfig(confFile.getAbsolutePath, Some(""), Some(""), Some(""), Some(""), Some(fr.proline.repository.DriverType.POSTGRESQL), Some(dataDir.getAbsolutePath), Some("proline"), Some("proline"), Some("localhost"), Some(5432))

  }
}