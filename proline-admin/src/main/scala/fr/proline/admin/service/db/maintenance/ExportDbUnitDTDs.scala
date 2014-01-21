package fr.proline.admin.service.db.maintenance

import java.io.FileOutputStream

import org.dbunit.DataSourceDatabaseTester
import org.dbunit.database.DatabaseConfig
import org.dbunit.dataset.xml.FlatDtdDataSet

import com.typesafe.scalalogging.slf4j.Logging

import fr.proline.admin.service.ICommandWork
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository.IDatabaseConnector

/**
 * @author David Bouyssie
 *
 */
class ExportDbUnitDTDs( dsConnectorFactory: DataStoreConnectorFactory, dirPath: String ) extends ICommandWork with Logging {

  def doWork() {
    
    val outputDir = new java.io.File(dirPath)
    require( outputDir.exists(), "unexisting export directory: "+ dirPath)
    
    val udsDbDtd = new java.io.File(outputDir, "uds-dataset.dtd")
    val psDbDtd = new java.io.File(outputDir, "ps-dataset.dtd")
    val pdiDbDtd = new java.io.File(outputDir, "pdi-dataset.dtd")
    val msiDbDtd = new java.io.File(outputDir, "msi-dataset.dtd")
    //val lcmsDbDtd = new java.io.File(outputDir, "lcms-dataset.dtd")
    
    val udsDbConnector = dsConnectorFactory.getUdsDbConnector()
    val psDbConnector = dsConnectorFactory.getPsDbConnector()
    val pdiDbConnector = dsConnectorFactory.getPdiDbConnector()
    val msiDbConnector = dsConnectorFactory.getMsiDbConnector(1L) // we assume the first project exists
    //val lcmsDbConnector = dsConnectorFactory.getLcmsDbConnector(1L)
    
    this.writeDataSetDTD(udsDbConnector,udsDbDtd.toString)
    this.writeDataSetDTD(psDbConnector,psDbDtd.toString)
    this.writeDataSetDTD(pdiDbConnector,pdiDbDtd.toString)
    this.writeDataSetDTD(msiDbConnector,msiDbDtd.toString)
  }
  
  protected def writeDataSetDTD( dbConnector: IDatabaseConnector, dtdFileName: String) {
      
    val dbTester = new DataSourceDatabaseTester(dbConnector.getDataSource())
    val dbUnitConn = dbTester.getConnection()
    val dbCfg = dbUnitConn.getConfig()
    dbCfg.setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true)

    val fos = new FileOutputStream(dtdFileName)

    FlatDtdDataSet.write(dbUnitConn.createDataSet(), new FileOutputStream(dtdFileName))
  }

}

object ExportDbUnitDTDs {
  
  def apply( dsConnectorFactory: DataStoreConnectorFactory, exportDirPath: String ): Unit = {
    new ExportDbUnitDTDs(dsConnectorFactory,exportDirPath).doWork()
  }

}