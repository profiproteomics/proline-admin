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

object DatabaseDumper {
  
  // Retrieve Proline configuration
  val prolineConf = SetupProline.config
  
  // Instantiate a database manager
  val dsConnectorFactory = DataStoreConnectorFactory.getInstance()
  if( dsConnectorFactory.isInitialized == false ) dsConnectorFactory.initialize(prolineConf.udsDBConfig.connector)
  
  def dumpMsiDb( projectId: Int, filePath: String ): Unit = {
    new DumpDatabase(dsConnectorFactory.getMsiDbConnector(projectId),filePath).doWork()
  }
  
  def dumpPsDb( filePath: String ): Unit = {
    new DumpDatabase(dsConnectorFactory.getPsDbConnector(),filePath).doWork()    
  }
  
  def dumpUdsDb( filePath: String ): Unit = {
    new DumpDatabase(dsConnectorFactory.getUdsDbConnector(),filePath).doWork()    
  }

}
