package fr.proline.admin.service.db

import java.io.File
import scala.collection.JavaConversions.mapAsJavaMap
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import com.typesafe.config.ConfigFactory
import com.weiglewilczek.slf4s.Logging

class SetupProlineTest extends Logging {

  @Rule // JUnit Rule annotation doesn't like static fields
  var tmpDirectory: TemporaryFolder = new TemporaryFolder()

  @Test
  def testProlineSetup() {
    val dataTmpDir = tmpDirectory.newFolder("proline_data_tmp_dir").getAbsolutePath()

    val inMemoryConfig = ConfigFactory.parseMap(
      Map(
        "proline-config.driver-type" -> "h2",
        "proline-config.data-directory" -> dataTmpDir,
        "h2-config.connection-properties.connectionMode" -> "MEMORY"))

    // Load application config and replace some properties values with the previous ones
    var classLoader = SetupProline.getClass().getClassLoader()
    val appConf = inMemoryConfig.withFallback(ConfigFactory.load(classLoader, "application"))

    val prolineSetupConfig = SetupProline.parseProlineSetupConfig(appConf)

    new SetupProline(prolineSetupConfig).run()
  }

}
