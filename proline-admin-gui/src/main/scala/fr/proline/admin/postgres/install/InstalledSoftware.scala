package fr.proline.admin.postgres.install
import com.typesafe.scalalogging.LazyLogging
import scala.collection.mutable.ArrayBuffer
import fr.proline.admin.postgres.install._
import fr.proline.admin.gui.QuickStart
import java.io.IOException
import java.io._
import java.io.StringWriter
import scala.util.control.Breaks._
/**
 * check 
 *  -OS type
 *  -postgreSQL is installed
 *
 */

object CheckInstalledPostgres {

  private var OS: String = System.getProperty("os.name").toLowerCase()
  private var command: String = _

  /* check postgresql and alert if not installed  */

  def checkPostgres(): Boolean = {
    var isInstalled = true
    if (isWindows()) {

      if (!readRegistry("HKEY_LOCAL_MACHINE\\SOFTWARE\\", "PostgreSQL", "reg query")) {
        isInstalled = false
      }
    }

    if (isUnix()) {
      if (!readRegistry("psql", "psql", "which")) {
        isInstalled = false
      }
    }
    return isInstalled
  }

  /* get the operating system: Windows or Unix */

  def isWindows(): Boolean = {
    return (OS.indexOf("win") >= 0)
  }
  def isMac(): Boolean = {
    return (OS.indexOf("mac") >= 0)
  }
  def isUnix(): Boolean = {
    return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0)
  }
  def isSolaris(): Boolean = {
    return (OS.indexOf("sunos") >= 0);
  }

  /* warning message */

  def readRegistry(location: String, key: String, command: String): Boolean = {
    var registryExist: Boolean = false
    try {
      var process: Process = Runtime.getRuntime().exec(command + " " + location)
      val reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      var line: String = "";
      try {
        while ((line = reader.readLine()) != null) {
          if (line.indexOf(key) >= 0) {
            registryExist = true
          }
        }
      } catch {
        case e: IOException => e.printStackTrace()
      }
    } catch {
      case e: Exception =>
    }
    return registryExist
  }
}
