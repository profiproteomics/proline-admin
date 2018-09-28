package fr.proline.admin.service.db.maintenance

import java.io.FileOutputStream

import com.typesafe.scalalogging.LazyLogging

import org.dbunit.database.DatabaseSequenceFilter
import org.dbunit.dataset.FilteredDataSet
import org.dbunit.dataset.xml.FlatXmlWriter

import fr.proline.admin.helper.sql.createDatabaseTester
import fr.proline.admin.service.ICommandWork
import fr.proline.repository._

/**
 * @author David Bouyssie
 *
 */
class DumpDatabase(
  dbConnector: IDatabaseConnector,
  outputFilePath: String,
  excludedTableNames: Array[String] = Array("SCHEMA_VERSION"),
  docType: Option[String] = None
) extends ICommandWork with LazyLogging {

  def doWork() {
    
    val dataSource = dbConnector.getDataSource()
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
    val excludedTableNameSet = excludedTableNames.map(_.toUpperCase()).toSet
    val filteredTableNames = tableNames.filter(t => excludedTableNameSet.contains(t.toUpperCase()) == false )
    //println( scala.runtime.ScalaRunTime.stringOf(ft))
    //ft.foreach( t => println( scala.runtime.ScalaRunTime.stringOf(dataSet.getTableMetaData(t))) )
    
    val filteredDS = new FilteredDataSet(filteredTableNames, sortedDS)
    
    // Output filtered dataset
    val out = new FileOutputStream(outputFilePath)
    val datasetWriter = new FlatXmlWriter(out)
    if (docType.isDefined) datasetWriter.setDocType(docType.get)
    datasetWriter.setIncludeEmptyTable(false) // remove empty tables
    datasetWriter.write(filteredDS)
    out.close()
    
    ()
  }

}

object DumpDatabase {
  
  def apply( dbConnector: IDatabaseConnector, filePath: String ): Unit = {
    new DumpDatabase(dbConnector,filePath).doWork()
  }

}
