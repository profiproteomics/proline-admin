package fr.proline.admin.service.db.maintenance

import java.io.FileOutputStream

import org.dbunit.DataSourceDatabaseTester
import org.dbunit.database.DatabaseConfig
import org.dbunit.dataset.xml.FlatDtdDataSet

import com.typesafe.scalalogging.LazyLogging

import fr.proline.admin.helper.sql.createDatabaseTester
import fr.proline.admin.service.ICommandWork
import fr.proline.repository._

/**
 * @author David Bouyssie
 *
 */
class ExportDbUnitDTDs( dsConnectorFactory: IDataStoreConnectorFactory, dirPath: String ) extends ICommandWork with LazyLogging {

  def doWork() {
    
    val outputDir = new java.io.File(dirPath)
    require( outputDir.exists(), "unexisting export directory: "+ dirPath)
    
    val udsDbDtd = new java.io.File(outputDir, "uds-dataset.dtd")
    val psDbDtd = new java.io.File(outputDir, "ps-dataset.dtd")
    val pdiDbDtd = new java.io.File(outputDir, "pdi-dataset.dtd")
    val msiDbDtd = new java.io.File(outputDir, "msi-dataset.dtd")
    val lcmsDbDtd = new java.io.File(outputDir, "lcms-dataset.dtd")
    
    val udsDbConnector = dsConnectorFactory.getUdsDbConnector()
    val psDbConnector = dsConnectorFactory.getPsDbConnector()
    val pdiDbConnector = dsConnectorFactory.getPdiDbConnector()
    val msiDbConnector = dsConnectorFactory.getMsiDbConnector(1L) // we assume the first project exists
    val lcmsDbConnector = dsConnectorFactory.getLcMsDbConnector(1L)
    
    this.writeDataSetDTD(udsDbConnector,udsDbDtd.toString)
    this.writeDataSetDTD(psDbConnector,psDbDtd.toString)
    this.writeDataSetDTD(pdiDbConnector,pdiDbDtd.toString)
    this.writeDataSetDTD(msiDbConnector,msiDbDtd.toString)
    this.writeDataSetDTD(lcmsDbConnector,lcmsDbDtd.toString)
  }
  
  protected def writeDataSetDTD( dbConnector: IDatabaseConnector, dtdFileName: String) {
      
    //val dbTester = new DataSourceDatabaseTester(dbConnector.getDataSource())
    val dbTester = createDatabaseTester( dbConnector.getDataSource(), dbConnector.getDriverType )
    val dbUnitConn = dbTester.getConnection()
    val dbCfg = dbUnitConn.getConfig()
    dbCfg.setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true)

    val fos = new FileOutputStream(dtdFileName)

    FlatDtdDataSet.write(dbUnitConn.createDataSet(), new FileOutputStream(dtdFileName))
  }

}

object ExportDbUnitDTDs {
  
  def apply( dsConnectorFactory: IDataStoreConnectorFactory, exportDirPath: String ): Unit = {
    new ExportDbUnitDTDs(dsConnectorFactory,exportDirPath).doWork()
  }

}