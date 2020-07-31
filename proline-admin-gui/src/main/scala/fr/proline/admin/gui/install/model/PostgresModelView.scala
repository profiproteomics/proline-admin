package fr.proline.admin.gui.install.model

import com.typesafe.scalalogging.LazyLogging

import fr.proline.admin.gui.Install
import fr.proline.admin.gui.process.PostgreSQLUtils
import java.io.File

/**
 * The postgreSQL model view.
 * Defines UI actions read and update parameters from postresql.conf and pg_hba.conf files.
 * @author aromdhani
 *
 */

class PostgresModelView(postgresDataDirPath: String) extends LazyLogging {

  /** Return pg_hba.conf file canonical path */
  def pgHbaFilePath: String = {
    val pgHbaFile = new File(Install.pgDataDirPath + File.separator + "pg_hba.conf")
    require(pgHbaFile.exists, "The pg_hba.conf file does not exists!")
    pgHbaFile.getCanonicalPath
  }

  /** Return postgesql.conf file canonical path */
  def postgresqlFilePath: String = {
    val postgresqlFile = new File(Install.pgDataDirPath + File.separator + "postgresql.conf")
    require(postgresqlFile.exists, "The postgresql.conf file does not exists!")
    postgresqlFile.getCanonicalPath
  }

  /** Return postgreSQL service name */
  def pgName(): Option[String] = {
    PostgreSQLUtils.name()
  }

  /** Return postgreSQL version */
  def pgVersionOpt(): Option[String] = {
    try {
      val pgServiceName = pgName()
      val pgVersionOpt = PostgreSQLUtils.version(pgServiceName)
      pgVersionOpt.foreach { pgVersion =>
        logger.debug(s"The version of postgreSQL :$pgVersion")
      }
      pgVersionOpt
    } catch {
      case ex: Exception =>
        logger.error("Error while trying to retrieve PostgrSQL version. Common configurations of postgreSQL will be setup!", ex.getMessage())
        None
    }
  }

  /** Restart PostgreSQL service after configuration updates */
  def restart(): Unit = {
    pgName().foreach { pgService => PostgreSQLUtils.restart(pgService) }
  }
}

