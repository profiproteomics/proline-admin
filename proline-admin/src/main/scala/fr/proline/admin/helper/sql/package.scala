package fr.proline.admin.helper

import java.sql.Connection
import java.sql.DriverManager

import org.dbunit.DataSourceDatabaseTester
import org.dbunit.database.DatabaseConfig
import org.dbunit.database.DatabaseSequenceFilter
import org.dbunit.dataset.FilteredDataSet
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder
import org.dbunit.operation.DatabaseOperation
import org.postgresql.Driver
import org.postgresql.util.PSQLException

import com.typesafe.scalalogging.slf4j.Logger
import com.typesafe.scalalogging.slf4j.Logging

import fr.proline.admin.service.db.setup.DatabaseSetupConfig
import fr.proline.repository.DatabaseUpgrader
import fr.proline.repository.DriverType
import fr.proline.repository.IDatabaseConnector
import fr.proline.util.StringUtils

/**
 * @author David Bouyssie
 *
 */
package object sql extends Logging {
  
  def initDbSchema( dbConnector: IDatabaseConnector, dbConfig: DatabaseSetupConfig ): Boolean = {

    // Create database if driver type is PostgreSQL
    if (dbConfig.driverType == DriverType.POSTGRESQL) {
      createPgDatabase(dbConnector, dbConfig, Some(logger))
    }

    // Initialize database schema
    // TODO: find an other way to handle the SCHEMA VERSION
    dbConfig.schemaVersion = "0.1"

    val upgradeStatus = if (DatabaseUpgrader.upgradeDatabase(dbConnector) > 0) true else false

    upgradeStatus
  }
  
  def setupDbFromDataset( dbConfig: DatabaseSetupConfig, datasetName: String ) {
    
    // Create connector
    val connector = dbConfig.toNewConnector()
    
    setupDbFromDataset( connector, dbConfig, datasetName )
    
    connector.close()
  }
  
  // TODO: retrieve the datasetName from the config ?
  // Inspired from: http://www.marcphilipp.de/blog/2012/03/13/database-tests-with-dbunit-part-1/
  def setupDbFromDataset( dbConnector: IDatabaseConnector, dbConfig: DatabaseSetupConfig, datasetName: String ) {
    
    if( initDbSchema( dbConnector, dbConfig ) ) {
      logger.info(s"schema initiated for database '${dbConfig.dbName}'")
      
      // Load the dataset
      //val dataLoader = new FlatXmlDataFileLoader()
      //val dataSet = dataLoader.load(datasetName)
      val datasetBuilder = new FlatXmlDataSetBuilder()
      datasetBuilder.setColumnSensing(true)
      
      val dsInputStream = this.getClass().getResourceAsStream(datasetName)
      val dataSet = datasetBuilder.build(dsInputStream)
      
      // Connect to the data source
      val dataSource = dbConnector.getDataSource()
      val dbTester = new DataSourceDatabaseTester(dbConnector.getDataSource())
      val dbUnitConn = dbTester.getConnection()
      
      // Tell DbUnit to be case sensitive
      dbUnitConn.getConfig.setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true)
      
      // Filter the dataset if the driver is not SQLite
      val filteredDS = if( dbConfig.driverType == DriverType.SQLITE ) dataSet
      else {
        new FilteredDataSet( new DatabaseSequenceFilter(dbUnitConn) , dataSet)
      }
      
      // Import the dataset
      dbTester.setDataSet(filteredDS)
      dbTester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT)
      dbTester.onSetup()
      
      dbUnitConn.close()
      
      logger.info("database '" + dbConfig.dbName + "' successfully set up !")
    }
    
  }

  protected def createPgDatabase(pgDbConnector: IDatabaseConnector, dbConfig: DatabaseSetupConfig, logger: Option[Logger] = None) {
    
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