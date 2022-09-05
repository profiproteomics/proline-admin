package fr.proline.admin.gui.process.config.postgres

import fr.profi.util.scala.LabeledParamEnum
import fr.profi.util.scala.ScalaUtils._
import fr.proline.admin.gui.process.config._

/**
 * ************************************************** *
 * Enumerate optimizable fields in postgres.conf file *
 * ************************************************** *
 */
object PostgresOptimizableParamEnum extends LabeledParamEnum {

  val MAX_CONNECTIONS = Param("MAX_CONNECTIONS", "Max. connections")
  val TCP_KEEPALIVES_IDLE = Param("TCP_KEEPALIVES_IDLE", "TCP keepalives idle")
  val SHARED_BUFFERS = Param("SHARED_BUFFERS", "Shared buffers")
  //to replace by min_wal_size and max_wal_size since the version PostgreSQL 9.6
  //val CHECKPOINT_SEGMENTS = Param("CHECKPOINT_SEGMENTS","Checkpoint segments")
  val CHECKPOINT_COMPLETION_TARGET = Param("CHECKPOINT_COMPLETION_TARGET", "Checkpoint completion target")
  val TEMP_BUFFERS = Param("TEMP_BUFFERS", "Temp. buffers")
  val WORK_MEM = Param("WORK_MEM", "Working memory")
  val MAINTENANCE_WORK_MEM = Param("MAINTENANCE_WORK_MEM", "Maintenance working memory")
  val EFFECTIVE_CACHE_SIZE = Param("EFFECTIVE_CACHE_SIZE", "Effective cache size")

  def getParamConfigKey(param: PostgresOptimizableParamEnum.Param): String = {
    param.name.toLowerCase
    //    param match {
    //      case MAX_CONNECTIONS => "max_connections"
    //      case TCP_KEEPALIVES_IDLE =>
    //      case SHARED_BUFFERS =>
    //      case CHECKPOINT_SEGMENTS =>
    //      case CHECKPOINT_COMPLETION_TARGET =>
    //      case TEMP_BUFFERS =>
    //      case WORK_MEM =>
    //      case MAINTENANCE_WORK_MEM =>
    //      case EFFECTIVE_CACHE_SIZE =>
    //    }
  }

  def getParamConfigKeys(): Seq[String] = {
    this.params().map(getParamConfigKey)
  }
}

case class PostgresOptimizableConfigParam[T](name: PostgresOptimizableParamEnum.Value, valueRange: ConfigParamValueRange)

/**
 * ************************* *
 * Enumerate parameter types *
 * ************************* *
 */
object PgParamType extends Enumeration {
  val TIME = Value("TIME")
  val BYTES = Value("BYTES")
  val DECIMAL = Value("DECIMAL")
  val INTEGER = Value("INTEGER")
}

