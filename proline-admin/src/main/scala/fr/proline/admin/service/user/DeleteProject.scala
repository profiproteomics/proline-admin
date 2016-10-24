package fr.proline.admin.service.user
import javax.persistence.EntityTransaction
import com.typesafe.scalalogging.LazyLogging
import fr.profi.jdbc.easy.{ date2Formattable, int2Formattable, string2Formattable }
import fr.proline.admin.service.db.{ CreateProjectDBs, SetupProline }
import fr.proline.admin.service.ICommandWork
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.uds.{ Dataset => UdsDataset, Project => UdsProject, UserAccount => UdsUser }
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository.DriverType
import fr.proline.core.orm.uds.ExternalDb
import fr.proline.core.orm.uds.Dataset
import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.IdentificationDataset
import javax.persistence.Persistence;
import javax.persistence.Query;
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import fr.proline.context._
import fr.proline.core.dal.DoJDBCReturningWork
import fr.proline.repository._
import fr.proline.core.orm.uds.repository.ExternalDbRepository
import fr.proline.repository.ProlineDatabaseType
import fr.proline.core.orm.uds.Dataset
import fr.proline.core.orm.uds.repository.DatasetRepository
import fr.proline.core.orm.uds.IdentificationDataset
import javax.persistence.FlushModeType

/**
 *  Delete project 
 */
class DeleteProject( dsConnectorFactory: IDataStoreConnectorFactory,projectId:Long,dropDatabases:String) extends ICommandWork with LazyLogging {
  
 
  def doWork() {

 
    // Open a connection to the UDSdb
    
    val udsDbConnector = dsConnectorFactory.getUdsDbConnector
    val udsDbCtx = new UdsDbConnectionContext(udsDbConnector)
    val udsEM = udsDbCtx.getEntityManager
    var localUdsTransaction: EntityTransaction = null
    var udsTransacOK: Boolean = false
    udsEM.setFlushMode(FlushModeType.COMMIT)
    try {
 
      if(!udsDbCtx.isInTransaction){
        localUdsTransaction=udsEM.getTransaction()
        localUdsTransaction.begin()
        udsTransacOK=false
       }
      
      // Remove externalDb type='MSI' and projectId
      
        val project =udsEM.find(classOf[Project],projectId)
        if(project!=null){
          val externalDbMsi=ExternalDbRepository.findExternalByTypeAndProject(udsEM,fr.proline.repository.ProlineDatabaseType.MSI,project)
          udsEM.remove(externalDbMsi)
    
          //Remove externalDb type LCMS and projectId
     
          val externalDbLcms=ExternalDbRepository.findExternalByTypeAndProject(udsEM,fr.proline.repository.ProlineDatabaseType.LCMS,project)
          udsEM.remove(externalDbLcms)
  
           //delete from dataset 
      
          val dataSets=DatasetRepository.findDatasetsByProject(udsEM,projectId)
          dataSets.toList.foreach{dataSet =>
            if(dataSet!=null){
               //delete run_identification 
               val runIdentification =udsEM.find(classOf[IdentificationDataset],dataSet.getId())
               if(runIdentification!=null){
                 udsEM.remove(runIdentification)
               }
               //delete dataSet 
                udsEM.remove(dataSet)
              }
            }
            //delete  project 
            udsEM.remove(project)
            logger.info("project #"+projectId+" has been deleted.")
        }else{
          logger.error("project #"+projectId+" does not exist in uds_db.")
        }
      //drop databases (MSI and LCMS)
      if(dropDatabases=="true"){
        
        try{
         udsEM.createNativeQuery("DROP DATABASE  IF EXISTS  msi_db_project_"+projectId).executeUpdate()
         udsEM.createNativeQuery("DROP DATABASE  IF EXISTS  lcms_db_project_"+projectId).executeUpdate()
         
        }
         catch {
          case ex: Exception => logger.error("Error to drop databases", ex)
        }

       }
      if (localUdsTransaction != null) {
        localUdsTransaction.commit()
        udsTransacOK = true
      }
      
    } finally {

      udsDbCtx.close()
      udsDbConnector.close()
 
    }
  }
  
}

object DeleteProject  {

  def apply(dsConnectorFactory: IDataStoreConnectorFactory,projectId: Long,dropDatabases:String): Unit = {
     new DeleteProject(dsConnectorFactory,projectId,dropDatabases).doWork()
  }
        
}
