package fr.proline.admin.helper

import java.sql.Connection
import java.sql.DriverManager

import org.postgresql.Driver
import org.postgresql.util.PSQLException

import com.typesafe.scalalogging.slf4j.Logger

import fr.proline.admin.service.db.setup.DatabaseSetupConfig
import fr.proline.repository.IDatabaseConnector
import fr.proline.util.StringUtils

/**
 * @author David Bouyssie
 *
 */
package object sql {

  /*def buildEmfUsingPgWorkaround( connector: DatabaseConnector,
                                 db: ProlineRepository.Databases ): EntityManagerFactory = {
    
    val emSettings = connector.getEntityManagerSettings()
    println( emSettings.get("hibernate.dialect") )
  
    /* Custom Dialect with one sequence / Entity (Table) */
    emSettings.put("hibernate.dialect", "fr.proline.core.orm.utils.TableNameSequencePostgresDialect")    
    
    Persistence.createEntityManagerFactory(
      JPAUtil.PersistenceUnitNames.getPersistenceUnitNameForDB(db), emSettings
    )
  }*/

  def createPgDatabase(pgDbConnector: IDatabaseConnector, dbConfig: DatabaseSetupConfig, logger: Option[Logger] = None) {
    
    // Create database connection and statement
    val pgDbConn = {
      try {
        pgDbConnector.getDataSource.getConnection
      } catch {
        case psqle: PSQLException => {
          val pgClass = classOf[org.postgresql.Driver]
          
          val connConfig = dbConfig.connectionConfig
          val host = connConfig.getString("host")
          val port = connConfig.getString("port")
          require( StringUtils.isNotEmpty(port), "missing port value" )
          val portAsInteger = port.toInt
          
          val templateURL = if( portAsInteger >= 0 && portAsInteger <= 65535 )
            s"jdbc:postgresql://${host}:${port}/template1"
          else
            s"jdbc:postgresql://${host}/template1"
          
          logger.map( _.info(s"creating database from template '${templateURL}'...") )

          val pgTemplateConn = DriverManager.getConnection(
            templateURL,
            connConfig.getString("user"),
            connConfig.getString("password")
          )
          val stmt = pgTemplateConn.createStatement
          
          // Create database if it doesn't exists
          if (_checkDbExists(stmt, dbConfig.dbName) == false) {
            logger.map( _.info(s"creating database '${dbConfig.dbName}'...") )
            stmt.executeUpdate(s"CREATE DATABASE ${dbConfig.dbName};")
          }

          // Close database connection and statement
          stmt.close()
          pgTemplateConn.close()
          
          pgDbConnector.getDataSource.getConnection
        }
      } 
    }.asInstanceOf[Connection]    

    val stmt = pgDbConn.createStatement
    
    // Check that database has been created
    if (_checkDbExists(stmt, dbConfig.dbName) == false)
      throw new Exception(s"can't create database '${dbConfig.dbName}'")

    // Close database connection and statement
    stmt.close()
    pgDbConn.close()
  }

  private def _checkDbExists(stmt: java.sql.Statement, dbName: String): Boolean = {
    val jdbcRS = stmt.executeQuery(s"SELECT count(*) FROM pg_catalog.pg_database WHERE datname = '${dbName}'")

    if (jdbcRS.next() && jdbcRS.getInt(1) == 0) false else true
  }

}