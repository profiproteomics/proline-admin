package fr.proline.admin.service.db.setup

import java.io.{File,InputStream}
import scala.io.Source
import com.googlecode.flyway.core.Flyway
import com.weiglewilczek.slf4s.Logging

import fr.proline.admin.utils.resources._
import fr.proline.admin.utils.sql._
import fr.proline.core.dal.DatabaseManagement
import fr.proline.core.utils.io._
import fr.proline.repository.DatabaseConnector

/**
 * @author David Bouyssie
 *
 */
trait ISetupDB extends Logging {  
  
  val dbConfig: DatabaseSetupConfig
  val dbManager: DatabaseManagement
  private var _executed = false
  
  // Interface
  protected def importDefaults(): Unit
  
   /** Execution state. */
  def isExecuted = this._executed
  
  /** Execute the setup of the database. */
  def run() {
    if( _executed )
      throw new IllegalStateException("the setup has been already executed")
    
    if( this.initSchema( dbConfig.connector, dbConfig.scriptResourcePath ) ) {
      this.importDefaults()
    }
    
    this._executed = true
  }
  
  protected def initSchema( connector: DatabaseConnector, scriptResourcePath: String ): Boolean = {
    
    // If driver type is SQLite (flyway doesn't support SQLite at the moment)
    if( dbConfig.driverType == "sqlite" ) {
      
      val scriptIS = pathToStreamOrResourceToStream(scriptResourcePath,classOf[DatabaseConnector])
      createSQLiteDB(connector,scriptIS)
      
    } else {
      
      // Create database if driver type is PostgreSQL
      if( dbConfig.driverType == "postgresql" ) {
        val pgDbConnector = dbConfig.dbConnPrototype.toConnector("postgres")
        createPgDatabase( pgDbConnector, dbConfig.dbName, Some(this.logger) )
      }
      
      this.logger.info("updating database schema...")
      
      val flyway = new Flyway()
      // TODO: find a workaround for absolute paths
      flyway.setLocations( scriptResourcePath.toString() + "/" )
      flyway.setDataSource(connector.getDataSource)
      
      if( flyway.migrate() > 0 ) true
      else false
    }

  }
  
  protected def createSQLiteDB( connector: DatabaseConnector, scriptIS: InputStream ): Boolean = {
    
    // If connection mode is file
    var createDB = true
    if( dbConfig.connectionConfig.getString("connectionMode") == "FILE" ) {
      
      val dbPath = dbConfig.dbDirectory + "/"+ dbConfig.connectionConfig.getString("dbName")
      
      if( new File(dbPath).exists == true ) {
        this.logger.warn("database file already exists")
        createDB = false
      }
      else
        this.logger.info("create new database file: "+dbPath)
    }
    
    if( createDB ) {
      val dbConn = connector.getConnection
      val stmt = dbConn.createStatement
      
      this.logger.info("creating database schema...")
      Source.fromInputStream(scriptIS).eachLine(";", stmt.executeUpdate(_) )
      
      stmt.close()
      dbConn.close()
    }
    
    createDB
          
  }
  
}
