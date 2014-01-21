package fr.proline.admin.service.db.setup

import com.typesafe.scalalogging.slf4j.Logging
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.ps.{AdminInformation => PsAdminInfos}
import fr.proline.module.rm.unimod.UnimodImporter
import fr.proline.util.sql.getTimeAsSQLTimestamp
import fr.proline.repository.IDatabaseConnector

/**
 * @author David Bouyssie
 *
 */
class SetupPsDB( val dbConnector: IDatabaseConnector,
                 val dbContext: DatabaseConnectionContext,
                 val dbConfig: DatabaseSetupConfig ) extends ISetupDB with Logging {

  protected def importDefaults() {
    
    val psEM = dbContext.getEntityManager()
    //val wasEmOpened = psEM.isOpen    
    
    // Begin transaction
    val psTransaction = psEM.getTransaction()    
    psTransaction.begin()
    
    val psAdminInfos = new PsAdminInfos()
    psAdminInfos.setModelVersion(dbConfig.schemaVersion)
    psAdminInfos.setDbCreationDate(getTimeAsSQLTimestamp)
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
    //if( !wasEmOpened ) psEM.close()
    
  }
  
}