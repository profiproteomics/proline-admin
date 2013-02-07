package fr.proline.admin.service.db

import java.io.File
import scala.collection.JavaConversions.mapAsJavaMap
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import com.typesafe.config.ConfigFactory
import com.weiglewilczek.slf4s.Logging
import org.junit.Before
import java.util.Date

class SetupProlineTest extends Logging {

  /* JUnit @Rule annotation does not work in Scala : 
   * http://stackoverflow.com/questions/7352087/how-can-i-use-junit-expectedexception-in-scala
   */
  var tmpDir: File = null

  @Before
  def createTmpDir() {
    val now = new Date()
    val tmpDirName = String.format("temp_%tY%<tm%<td%<tH%<tM%<tS_%<tL", now)

    tmpDir = new File(tmpDirName)
    tmpDir.mkdir()

    tmpDir.deleteOnExit()
  }

  @Test
  def testProlineSetup() {
    val dataTmpDir = tmpDir.getAbsolutePath

    val inMemoryConfig = ConfigFactory.parseMap(
      Map(
        "proline-config.driver-type" -> "h2",
        "proline-config.data-directory" -> dataTmpDir,
        "h2-config.connection-properties.connectionMode" -> "MEMORY"))

    // Load application config and replace some properties values with the previous ones
    var classLoader = SetupProline.getClass().getClassLoader()
    val appConf = inMemoryConfig.withFallback(ConfigFactory.load(classLoader, "application"))

    SetupProline.setConfigParams(appConf)
    
    SetupProline()
  }

}
