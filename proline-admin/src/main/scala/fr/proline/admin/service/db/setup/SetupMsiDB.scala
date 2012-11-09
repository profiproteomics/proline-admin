package fr.proline.admin.service.db.setup

import javax.persistence.Persistence
import com.weiglewilczek.slf4s.Logging

import fr.proline.core.dal.DatabaseManagement
import fr.proline.core.orm.msi.{AdminInformation => MsiAdminInfos}
import fr.proline.core.orm.utils.JPAUtil

/**
 * @author David Bouyssie
 *
 */
class SetupMsiDB( val dbManager: DatabaseManagement,
                  val dbConfig: DatabaseSetupConfig,
                  val projectId: Int ) extends ISetupDB with Logging {

  protected def importDefaults() {
    
    val msiEMF = Persistence.createEntityManagerFactory(
                    JPAUtil.PersistenceUnitNames.MSI_Key.getPersistenceUnitName(),
                    dbManager.getMSIDatabaseConnector( projectId ).getEntityManagerSettings
                 )
    val msiEM = msiEMF.createEntityManager()
    
    // Begin transaction
    val msiTransaction = msiEM.getTransaction()    
    msiTransaction.begin()
    
    val udsAdminInfos = new MsiAdminInfos()
    udsAdminInfos.setModelVersion(dbConfig.schemaVersion)
    udsAdminInfos.setDbCreationDate(new java.sql.Timestamp(new java.util.Date().getTime))
    //udsAdminInfos.setModelUpdateDate()
    msiEM.persist( udsAdminInfos)
    
    // Commit transaction
    msiTransaction.commit()
    
  }
  
}