package fr.proline.admin.service.db.setup

import com.weiglewilczek.slf4s.Logging
import fr.proline.core.dal.DatabaseManagement
import fr.proline.core.orm.ps.{AdminInformation => PsAdminInfos}
import fr.proline.module.rm.unimod.UnimodImporter

/**
 * @author David Bouyssie
 *
 */
class SetupPsDB( val dbManager: DatabaseManagement,
                 val dbConfig: DatabaseSetupConfig ) extends ISetupDB with Logging {

  protected def importDefaults() {
    
    val psEM = dbManager.psEMF.createEntityManager()
    
    // Begin transaction
    val psTransaction = psEM.getTransaction()    
    psTransaction.begin()
    
    val psAdminInfos = new PsAdminInfos()
    psAdminInfos.setModelVersion(dbConfig.schemaVersion)
    psAdminInfos.setDbCreationDate(new java.sql.Timestamp(new java.util.Date().getTime))
    //psAdminInfos.setModelUpdateDate()    
    psEM.persist( psAdminInfos)
    
    this.logger.info("Admin information imported !")
  
    // Create Unimod file input stream
    val unimodIS = this.getClass().getResourceAsStream("/mascot_config/unimod.xml")
    
    // Import the Unimod definitions
    new UnimodImporter().importUnimodStream(unimodIS, psEM, false)
    
    // Commit transaction
    psTransaction.commit()
    
    this.logger.info("Unimod definitions imported !")
    
    // Close entity manager
    psEM.close()
    
  }
  
}