package fr.proline.admin.service.db.maintenance

import java.io.FileOutputStream

import org.dbunit.DataSourceDatabaseTester
import org.dbunit.database.DatabaseSequenceFilter
import org.dbunit.dataset.FilteredDataSet
import org.dbunit.dataset.xml.FlatXmlDataSet

import com.weiglewilczek.slf4s.Logging

import fr.proline.admin.service.ICommandWork
import fr.proline.admin.service.db.SetupProline
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository.IDatabaseConnector

/**
 * @author David Bouyssie
 *
 */
class DumpDatabase( dbConnector: IDatabaseConnector, outputFilePath: String ) extends ICommandWork with Logging {

  def doWork() {
    
    val dbTester = new DataSourceDatabaseTester(dbConnector.getDataSource())
    val con = dbTester.getConnection()
    val filter = new DatabaseSequenceFilter(con)
    val fullDataSet = new FilteredDataSet(filter, con.createDataSet())
    
    val out = new FileOutputStream(outputFilePath)

    FlatXmlDataSet.write(fullDataSet, out)
  }

}

object DumpDatabase {
  
  def apply( dbConnector: IDatabaseConnector, filePath: String ): Unit = {
    new DumpDatabase(dbConnector,filePath).doWork()
  }

}