package fr.proline.admin.service.db.maintenance

import java.io.File
import java.io.PrintWriter

import scala.collection.JavaConverters._

import com.typesafe.scalalogging.LazyLogging

import fr.proline.admin.service.ICommandWork
import fr.proline.context._
import fr.proline.core.dal.DoJDBCReturningWork
import fr.proline.core.orm.uds.repository.ProjectRepository
import fr.proline.repository._

/**
 * @author David Bouyssie
 *
 */
class ExportMsiDbStats( dsConnectorFactory: IDataStoreConnectorFactory, dirPath: String ) extends ICommandWork with LazyLogging {

  def doWork() {
    
    val outputDir = new java.io.File(dirPath)
    require( outputDir.exists(), "unexisting export directory: "+ dirPath)
    
    val writer = new PrintWriter(new File(dirPath + "/msqueries_stats.tsv"))
    writer.println( Array("name","modification_timestamp","queries_count").mkString(ExportMsiDbStats.SEP_CHAR))                                                                                        

    // Open a connection to the UDSdb
    val udsDbConnector = dsConnectorFactory.getUdsDbConnector
    val udsDbCtx = new UdsDbConnectionContext(udsDbConnector)
    val udsEM = udsDbCtx.getEntityManager
    
    /* Upgrade all Projects (MSI and LCMS) Dbs */
    val projectIds = ProjectRepository.findAllProjectIds(udsEM)
    
    if ((projectIds != null) && projectIds.isEmpty() == false) {

      for (projectId <- projectIds.asScala) {
        logger.info(s"Connecting to MSIdb of Project #$projectId")

        /* Upgrade MSI Db */
        val msiDbConnector = try {
          dsConnectorFactory.getMsiDbConnector(projectId)
        } catch {
          case t: Throwable => {
            logger.error(s"Can't compute stats for porject #$projectId")
            null
          }
        }
        
        if( msiDbConnector != null ) {
          val msiDbCtx = new MsiDbConnectionContext(msiDbConnector)
          
          try {
            DoJDBCReturningWork.withEzDBC(msiDbCtx) { msiEzDBC =>
              msiEzDBC.select(ExportMsiDbStats.queriesCountSqlQuery) { r =>
                val strings = Array(r.nextString,r.nextString,r.nextString)
                writer.println(strings.mkString(ExportMsiDbStats.SEP_CHAR))
              }
            }
          } finally {
            msiDbCtx.close()
            msiDbConnector.close()
          }
        }
      }
    }
    
    udsDbCtx.close()
    udsDbConnector.close()
    
    writer.close()
  }

}

object ExportMsiDbStats {
  
  private val SEP_CHAR = "\t"
  
  private val queriesCountSqlQuery = "SELECT result_set.name, result_set.modification_timestamp, msi_search.queries_count "+
  "FROM result_set, msi_search WHERE result_set.msi_search_id = msi_search.id AND result_set.type = 'SEARCH'"
  
  private val totalQueriesCountSqlQuery = "SELECT sum(msi_search.queries_count) FROM result_set, msi_search " +
  "WHERE result_set.msi_search_id = msi_search.id AND result_set.type = 'SEARCH'"
  
  def apply( dsConnectorFactory: IDataStoreConnectorFactory, exportDirPath: String ): Unit = {
    new ExportMsiDbStats(dsConnectorFactory,exportDirPath).doWork()
  }

}