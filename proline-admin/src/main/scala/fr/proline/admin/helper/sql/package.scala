package fr.proline.admin.helper

import javax.persistence.{EntityManagerFactory,Persistence}
import com.weiglewilczek.slf4s.Logger

import fr.proline.repository.IDatabaseConnector

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
  
  def createPgDatabase( pgDbConnector: IDatabaseConnector, dbName: String, logger: Option[Logger] = None ) {
    
    // Create database connection and statement
    val pgDbConn = pgDbConnector.getDataSource.getConnection
    val stmt = pgDbConn.createStatement
    
    // Create database if it doesn't exists
    if( _checkDbExists(stmt,dbName) == false ) {
      if( logger != None ) logger.get.info("creating database '%s'...".format(dbName))
      stmt.executeUpdate("CREATE DATABASE %s;".format(dbName) )
    }
    
    // Check that database has been created
    if( _checkDbExists(stmt,dbName) == false )
      throw new Exception( "can't create database '%s'".format(dbName) )
    
    // Close database connection and statement
    stmt.close()
    pgDbConn.close()   
  }
  
  private def _checkDbExists(stmt: java.sql.Statement, dbName: String): Boolean = {
    val jdbcRS = stmt.executeQuery("select count(*) from pg_catalog.pg_database where datname = '%s'"
                     .format(dbName))
    
    if( jdbcRS.next() && jdbcRS.getInt(1) == 0 ) false else true
  }

}