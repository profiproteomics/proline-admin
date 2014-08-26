package fr.proline.admin.service.db.maintenance

import java.io.FileOutputStream
import java.util.Arrays
import java.util.Collection

import org.dbunit.DataSourceDatabaseTester
import org.dbunit.database.DatabaseConfig
import org.dbunit.database.DatabaseSequenceFilter
import org.dbunit.database.IDatabaseConnection
import org.dbunit.dataset.FilteredDataSet
import org.dbunit.dataset.datatype.DefaultDataTypeFactory
import org.dbunit.dataset.xml.FlatXmlDataSet
import org.dbunit.ext.h2.H2DataTypeFactory
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory

import com.typesafe.scalalogging.slf4j.Logging

import fr.proline.admin.service.ICommandWork
import fr.proline.repository._

/**
 * @author David Bouyssie
 *
 */
class DumpDatabase( dbConnector: IDatabaseConnector, outputFilePath: String ) extends ICommandWork with Logging {

  def doWork() {
    
    val dataSource = dbConnector.getDataSource()
    //val dbTester = new DataSourceDatabaseTester(dataSource)
    val dbTester = createDatabaseTester( dataSource, dbConnector.getDriverType )
    val dbUnitConn = dbTester.getConnection()
    
    // Load the dataset
    val fullDataSet = dbUnitConn.createDataSet()
    
    // Filter the dataset if the driver is not SQLite and the DB is not the LcMSDb one (cyclic ref)
    val sortedDS = if( 
      dbConnector.getDriverType == DriverType.SQLITE ||
      dbConnector.getProlineDatabaseType == ProlineDatabaseType.LCMS
    ) {
      fullDataSet
    }
    else {
      new FilteredDataSet( new DatabaseSequenceFilter(dbUnitConn) , fullDataSet)
    }
    
    // TODO: find a workaround for cyclic references
    // => parse exception message and provide tables names to DatabaseSequenceFilter without the conflicting ones
    
    // Remove Flyway "schema_version" table from the datset
    val tableNames = sortedDS.getTableNames()
    val filteredTableNames = tableNames.filter(t => t !="schema_version" && t != "SCHEMA_VERSION" )
    //println( scala.runtime.ScalaRunTime.stringOf(ft))
    //ft.foreach( t => println( scala.runtime.ScalaRunTime.stringOf(dataSet.getTableMetaData(t))) )
    
    val filteredDS = new FilteredDataSet(filteredTableNames, sortedDS)
    
    // TODO: remove empty tables (use QueryDataSet or do manual SQL queries to get records statistics)
    
    val out = new FileOutputStream(outputFilePath)

    FlatXmlDataSet.write(filteredDS, out)
  }
  
  // TODO: remove code redudancy with same method in fr.proline.repository.util.DatabaseTestConnector
  def createDatabaseTester( dataSource: javax.sql.DataSource, driverType: DriverType ) = {
    new DataSourceDatabaseTester(dataSource) {
      
      import DriverType._
      
      override def getConnection(): IDatabaseConnection = {
        val dbUnitConn = super.getConnection()
        
        // Retrieve the IDataTypeFactory corresponding to the DriverType
        val dataTypeFactory = driverType match {
          case H2 => new H2DataTypeFactory()
          case POSTGRESQL => new PostgresqlDataTypeFactory();
          case SQLITE => new DefaultDataTypeFactory() {
              override def getValidDbProducts(): Collection[_] = {
                Arrays.asList( Array("sqlite") )
              }
            }
        }
        
        // Apply the created IDataTypeFactory to the connection config
        val dbUnitConnConfig = dbUnitConn.getConfig()
        dbUnitConnConfig.setProperty(
          DatabaseConfig.PROPERTY_DATATYPE_FACTORY, dataTypeFactory
        )
        
        // Tell Dbunit to be case sensitive
        dbUnitConnConfig.setProperty(
          DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true
        )
        
        dbUnitConn
      }
    }
  }

}

object DumpDatabase {
  
  def apply( dbConnector: IDatabaseConnector, filePath: String ): Unit = {
    new DumpDatabase(dbConnector,filePath).doWork()
  }

}
