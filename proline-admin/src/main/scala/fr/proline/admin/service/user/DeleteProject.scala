package fr.proline.admin.service.user

import javax.persistence.EntityTransaction
import javax.persistence.FlushModeType

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import com.typesafe.scalalogging.LazyLogging

import fr.proline.admin.service.ICommandWork
import fr.proline.context._
import fr.proline.core.dal.DoJDBCWork
import fr.proline.core.orm.uds.IdentificationDataset
import fr.proline.core.orm.uds.{ Project => UdsProject }
import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.repository.DatasetRepository
import fr.proline.core.orm.uds.repository.ExternalDbRepository
import fr.proline.repository._

/**
 *  Delete project
 */
class DeleteProject(
  dsConnectorFactory: IDataStoreConnectorFactory,
  projectId: Long,
  dropDatabases: Boolean
) extends ICommandWork with LazyLogging {

  def doWork() {

    // Open a connection to the UDSdb

    val udsDbConnector = dsConnectorFactory.getUdsDbConnector
    val udsDbCtx = new UdsDbConnectionContext(udsDbConnector)
    val udsEM = udsDbCtx.getEntityManager
    var localUdsTransaction: EntityTransaction = null
    udsEM.setFlushMode(FlushModeType.COMMIT)
    
    try {

      if (!udsDbCtx.isInTransaction) {
        localUdsTransaction = udsEM.getTransaction()
        localUdsTransaction.begin()
      }

      // Retrieve the project
      val project = udsEM.find(classOf[Project], projectId)
      if (project != null) {
        
        // Remove externalDb type='MSI' and projectId
        val externalDbMsi = ExternalDbRepository.findExternalByTypeAndProject(udsEM, fr.proline.repository.ProlineDatabaseType.MSI, project)
        udsEM.remove(externalDbMsi)

        // Remove externalDb type LCMS and projectId
        val externalDbLcms = ExternalDbRepository.findExternalByTypeAndProject(udsEM, fr.proline.repository.ProlineDatabaseType.LCMS, project)
        udsEM.remove(externalDbLcms)

        //delete from dataset 
        // DBO: this manual deletion is useless because of the delete cascade constraints on:
        // - project_data_set_fk
        // - data_set_run_identification_fk

        /*val dataSets = DatasetRepository.findDatasetsByProject(udsEM, projectId)
        dataSets.toList.foreach { dataSet =>
          if (dataSet != null) {
            //delete run_identification 
            val runIdentification = udsEM.find(classOf[IdentificationDataset], dataSet.getId())
            if (runIdentification != null) {
              udsEM.remove(runIdentification)
            }
            //delete dataSet 
            udsEM.remove(dataSet)
          }
        }*/
        
        // Delete  project 
        udsEM.remove(project)
        logger.info(s"Project #$projectId has been deleted.")
      } else {
        logger.error(s"Project #$projectId does not exist in uds_db.")
      }
      
      if (localUdsTransaction != null) {
        localUdsTransaction.commit()
      }
      
      // Drop databases (MSI and LCMS)
      if (dropDatabases) {
        
        logger.debug(s"Dropping MSI and LCMS databases of project #$projectId....")

        try {
          DoJDBCWork.withEzDBC(udsDbCtx) { ezDBC =>
            ezDBC.execute("DROP DATABASE IF EXISTS msi_db_project_" + projectId)
            ezDBC.execute("DROP DATABASE IF EXISTS lcms_db_project_" + projectId)
          }
        } catch {
          case t: Throwable => logger.error("Error while dropping MSI and LCMS databases", t)
        }

      }

    } finally {
      udsEM.setFlushMode(FlushModeType.AUTO)
      udsDbCtx.close()
      udsDbConnector.close()

    }
  }

}

object DeleteProject {

  def apply(dsConnectorFactory: IDataStoreConnectorFactory, projectId: Long, dropDatabases: Boolean): Unit = {
    new DeleteProject(dsConnectorFactory, projectId, dropDatabases).doWork()
  }

}
