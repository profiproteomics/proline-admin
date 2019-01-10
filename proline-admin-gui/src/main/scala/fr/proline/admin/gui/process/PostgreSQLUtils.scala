package fr.proline.admin.gui.process

import com.typesafe.scalalogging.LazyLogging

import fr.profi.util.system.OSInfo
import fr.profi.util.system.OSType
import fr.profi.util.system.OSType._
import sys.process._

/**
 * Proline-Admin PostgreSQL utilities
 * @author aromdhani
 */

object PostgreSQLUtils extends LazyLogging {

  /** Return PostreSQL service name */
  def name(): Option[String] = {
    var name: Option[String] = None
    OSInfo.getOSType() match {
      case (OSType.WINDOWS_X86 | OSType.WINDOWS_AMD64) => {
        execute(
          Seq("cmd.exe", "/c", "sc", "query", "|", "findstr", "/r", "/i", "SERVICE_NAME"),
          out =>
            {
              //Example: SERVICE_NAME : postgresql-x64-9.6
              if (out.indexOf("postgresql") > 0) {
                name = Option(out.split(":").apply(1).trim())
              }
            })
        name
      }
      case (OSType.LINUX_I386 | OSType.LINUX_AMD64) => name //TODO
      case (OSType.MAC_I386 | OSType.MAC_AMD64) => name //TODO
      case _ => name //TODO
    }
  }

  /** Return PostreSQL  version */
  def version(serviceName: Option[String]): Option[String] = serviceName match {
    case Some(name) => {
      val pattern = "([0-9](\\.[0-9])+)".r
      pattern.findFirstIn(name)
    }
    case _ => None
  }

  /** Restart PostgtreSQL service */
  def restart(serviceName: String): Unit = {
    try {
      logger.info(s"Trying to restart service: $serviceName")
      OSInfo.getOSType() match {
        case (OSType.WINDOWS_X86 | OSType.WINDOWS_AMD64) => {
          execute(Seq("cmd.exe", "/c", "sc", "stop ", serviceName), out => logger.debug(out))
          execute(Seq("cmd.exe", "/c", "sc", "start", serviceName), out => logger.debug(out))
        }
        case (OSType.LINUX_I386 | OSType.LINUX_AMD64) => //TODO
        case (OSType.MAC_I386 | OSType.MAC_AMD64) => //TODO
        case _ => //TODO
      }
    } catch {
      case ex: Exception => new Exception(s"Cannot restart service: $serviceName. Make sure that you have admin rights!")
    }
  }

  /** Execute a command as sequence of parameters */
  def execute(command: => Seq[String], action: String => Unit) = {
    var process: Process = null
    var exitCode = 1
    try {
      process = Process(command).run(ProcessLogger(out => action(out)))
      exitCode = process.exitValue
    } finally {
      if (process != null) {
        process.destroy
      }
    }
    if (exitCode != 0) {
      throw new Exception("Command has failed: " + command)
    }
  }
}

 