package fr.proline.admin.service.db.maintenance

import java.io.FileOutputStream
import com.typesafe.scalalogging.slf4j.Logging
import org.dbunit.DataSourceDatabaseTester
import org.dbunit.database.DatabaseConfig
import org.dbunit.database.DatabaseSequenceFilter
import org.dbunit.dataset.FilteredDataSet
import org.dbunit.dataset.xml.FlatXmlDataSet
import fr.proline.admin.service.ICommandWork
import fr.proline.admin.service.db.SetupProline
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.core.dal.ProlineEzDBC
import fr.proline.repository._


/**
 * @author David Bouyssie
 *
 */
class DumpDatabase( dbConnector: IDatabaseConnector, outputFilePath: String ) extends ICommandWork with Logging {

  def doWork() {
    
    val dataSource = dbConnector.getDataSource()
    val dbTester = new DataSourceDatabaseTester(dataSource)
    val dbUnitConn = dbTester.getConnection()
    
    /*if( disableFKs ) {
      val ezDBC = ProlineEzDBC( dbUnitConn.getConnection(), dbConnector.getDriverType )
      dbConnector.getDriverType match {
        case DriverType.H2 => ezDBC.execute("SET REFERENTIAL_INTEGRITY FALSE")
        case _ => throw new Exception("NYI")
      }
    }*/
    
    // Tell dbunit to be case sensitive 
    dbUnitConn.getConfig.setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true)
    
    val dataSet = dbUnitConn.createDataSet()
    //val tableNames = dataSet.getTableNames()
    /*val ft = tableNames //.filter(t => t !="schema_version" && t != "SCHEMA_VERSION" )
    //println( scala.runtime.ScalaRunTime.stringOf(ft))
    ft.foreach( t => println( scala.runtime.ScalaRunTime.stringOf(dataSet.getTableMetaData(t))) )*/
    
    //val filter = new DatabaseSequenceFilter(dbunitConn,ft)
    //val fullDataSet = new FilteredDataSet(tableNames, dataSet)
    
    val out = new FileOutputStream(outputFilePath)

    FlatXmlDataSet.write(dataSet, out)
  }

}

object DumpDatabase {
  
  def apply( dbConnector: IDatabaseConnector, filePath: String ): Unit = {
    new DumpDatabase(dbConnector,filePath).doWork()
  }

}
