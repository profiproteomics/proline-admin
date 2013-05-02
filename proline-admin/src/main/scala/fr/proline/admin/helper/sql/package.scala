package fr.proline.admin.helper

import javax.persistence.{ EntityManagerFactory, Persistence }
import com.weiglewilczek.slf4s.Logger
import fr.proline.repository.IDatabaseConnector
import org.postgresql.util.PSQLException
import java.sql.Connection
import java.sql.DriverManager
import fr.proline.admin.service.db.setup.DatabaseSetupConfig

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

  def createPgDatabase(dbConfig: DatabaseSetupConfig, logger: Option[Logger] = None) {

    val pgDbConnector = dbConfig.toNewConnector
    
    // Create database connection and statement
    val pgDbConn = {
      try {
        pgDbConnector.getDataSource.getConnection
      } catch {
        case psqle: PSQLException =>
          {
            classOf[org.postgresql.Driver]
            val templateURL = "jdbc:postgresql://"+dbConfig.connectionConfig.getString("host")+":"++dbConfig.connectionConfig.getString("port")+"/template1"
            if (logger != None) logger.get.info("creating database from template '%s'...".format(templateURL))

            val pgTemplateConn = DriverManager.getConnection(templateURL, dbConfig.connectionConfig.getString("user"), dbConfig.connectionConfig.getString("password"))
            val stmt = pgTemplateConn.createStatement
            // Create database if it doesn't exists
            if (_checkDbExists(stmt, dbConfig.dbName) == false) {
              if (logger != None) logger.get.info("creating database '%s'...".format(dbConfig.dbName))
              stmt.executeUpdate("CREATE DATABASE %s;".format(dbConfig.dbName))
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
      throw new Exception("can't create database '%s'".format(dbConfig.dbName))

    // Close database connection and statement
    stmt.close()
    pgDbConn.close()
    pgDbConnector.close()
  }

  private def _checkDbExists(stmt: java.sql.Statement, dbName: String): Boolean = {
    val jdbcRS = stmt.executeQuery("select count(*) from pg_catalog.pg_database where datname = '%s'"
      .format(dbName))

    if (jdbcRS.next() && jdbcRS.getInt(1) == 0) false else true
  }

}