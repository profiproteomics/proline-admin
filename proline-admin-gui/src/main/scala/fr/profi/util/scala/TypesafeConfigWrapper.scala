package fr.profi.util.scala

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import fr.profi.util.scala.ScalaUtils.isEmpty

/**
 * *************************** *
 * Wrapper for Typesafe Config *
 * *************************** *
 */
object TypesafeConfigWrapper {
  
  implicit class TypesafeConfigWrapper(config: Config) extends LazyLogging {
    //TODO: move to play.implicits

    def getStringOrElse(key: String, replacement: String): String = {
      //TODO: getIntOrElse(Int), ...
      try {
        config.getString(key)
      } catch {
        case e: Throwable => logger.warn(s"Cannot find configuration string for key '$key'", e); replacement;
      }
    }

    def getIntOrElse(key: String, replacement: Int): Int = {
      //TODO: getIntOrElse(Int), ...
      try {
        config.getInt(key)
      } catch {
        case e: Throwable => logger.warn(s"Cannot find configuration string for key '$key'", e); replacement;
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
        case e: Throwable => logger.warn(s"Cannot find configuration Int for key '$key'", e); None;
      }
    }
      def getBooleanOpt(key: String): Option[Boolean] = {
      try {
        Option(config.getBoolean(key))
      } catch {
        case e: Throwable => logger.warn(s"Cannot find configuration Boolean for key '$key'", e); None;
      }
    }
  }
}