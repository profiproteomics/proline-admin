package fr.profi.util.scala

import com.typesafe.config.Config
import com.typesafe.scalalogging.slf4j.Logging

import fr.profi.util.scala.ScalaUtils.isEmpty

/**
 * *************************** *
 * Wrapper for Typesafe Config *
 * *************************** *
 */
object TypesafeConfigWrapper {
  
  implicit class TypesafeConfigWrapper(config: Config) extends Logging {
    //TODO: move to play.implicits

    def getStringOrElse(key: String, replacement: String): String = {
      //TODO: getIntOrElse(Int), ...
      try {
        config.getString(key)
      } catch {
        case e: Throwable => logger.warn(s"Cannot find configuration string for key '$key'\n", e); replacement;
      }
    }

    def getIntOrElse(key: String, replacement: Int): Int = {
      //TODO: getIntOrElse(Int), ...
      try {
        config.getInt(key)
      } catch {
        case e: Throwable => logger.warn(s"Cannot find configuration string for key '$key'\n", e); replacement;
      }
    }

    def getStringOpt(key: String): Option[String] = {
      val value: String = getStringOrElse(key, null)
      if (isEmpty(value)) None
      else Option(value)
    }

    def getIntOpt(key: String): Option[Int] = {
      try {
        Option(config.getInt(key))
      } catch {
        case e: Throwable => logger.warn(s"Cannot find configuration Int for key '$key'\n", e); None;
      }
    }
  }
}