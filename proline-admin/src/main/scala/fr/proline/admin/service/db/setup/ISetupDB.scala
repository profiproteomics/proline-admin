package fr.proline.admin.service.db.setup

import java.io.File
import scala.io.Source
import com.googlecode.flyway.core.Flyway
import com.weiglewilczek.slf4s.Logging
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
    
    if( this.initSchema( dbConfig.connector, dbConfig.scriptDirectory ) ) {
      // TODO: store Admin Information
      
      this.importDefaults()
    }
    
    this._executed = true
  }
  
  protected def initSchema( connector: DatabaseConnector, scriptDirectory: File ): Boolean = {
    
    // If driver type is SQLite (flyway doesn't support SQLite at the moment)
    if( dbConfig.driverType == "sqlite" ) {
      val firstScript = scriptDirectory.listFiles.filter(_.getName.endsWith(".sql")).first
      createSQLiteDB(connector,firstScript)      
    } else {
      val flyway = new Flyway()
      flyway.setBaseDir(scriptDirectory.toString())
      flyway.setDataSource(connector.getDataSource)
      flyway.migrate()
      
      true
    }

  }
  
  protected def createSQLiteDB( connector: DatabaseConnector, scriptFile: File ): Boolean = {
    
    // Check that database script exists and as a valid extension
    require( scriptFile.exists && scriptFile.isFile() && scriptFile.getName.endsWith(".sql") )
    
    // If connection mode is file
    var createDB = true
    if( dbConfig.connectionConfig.getString("connectionMode") == "FILE" ) {
      
      val dbPath = dbConfig.dataDirectory + "/"+ dbConfig.connectionConfig.getString("dbName")
      
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
      
      Source.fromFile(scriptFile).eachLine(";", sqlQuery => {
        stmt.executeUpdate(sqlQuery)
      })
      
      stmt.close()
      dbConn.close()
    }
    
    createDB
          
  }
  
}
