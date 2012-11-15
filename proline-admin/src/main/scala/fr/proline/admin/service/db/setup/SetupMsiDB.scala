package fr.proline.admin.service.db.setup

import javax.persistence.Persistence
import scala.collection.JavaConversions.{collectionAsScalaIterable}
import com.typesafe.config.Config
import com.weiglewilczek.slf4s.Logging

import fr.proline.core.dal.{DatabaseManagement,MsiDbScoringTable}
import fr.proline.core.orm.msi.{AdminInformation => MsiAdminInfos, Scoring => MsiScoring }
import fr.proline.core.orm.utils.JPAUtil

/**
 * @author David Bouyssie
 *
 */
class SetupMsiDB( val dbManager: DatabaseManagement,
                  val dbConfig: DatabaseSetupConfig,
                  val defaults: MsiDBDefaults,
                  val projectId: Int ) extends ISetupDB with Logging {
  
  lazy val msiEMF = Persistence.createEntityManagerFactory(
                        JPAUtil.PersistenceUnitNames.MSI_Key.getPersistenceUnitName(),
                        dbManager.getMSIDatabaseConnector( projectId ).getEntityManagerSettings
                     )
  lazy val msiEM = msiEMF.createEntityManager()
    
  protected def importDefaults() {
    
    // Begin transaction
    val msiTransaction = msiEM.getTransaction()    
    msiTransaction.begin()
    
    // Import Admin information
    this._importAdminInformation()
    this.logger.info( "Admin information imported !" )
    
    // Import scoring definitions
    this._importScorings( this.defaults.scorings )
    this.logger.info( "Scoring definitions imported !" )
    
    // Commit transaction
    msiTransaction.commit()
    
  }
  
  private def _importAdminInformation() {

    val udsAdminInfos = new MsiAdminInfos()
    udsAdminInfos.setModelVersion(dbConfig.schemaVersion)
    udsAdminInfos.setDbCreationDate(new java.sql.Timestamp(new java.util.Date().getTime))
    //udsAdminInfos.setModelUpdateDate()
    msiEM.persist( udsAdminInfos)

  }
  
  private def _importScorings( scorings: java.util.List[Config] ) {
    
    val scoringCols = MsiDbScoringTable.columns
    
    // Store scorings
    for( scoring <- scorings ) {
      
      // Create new scoring
      val msiScoring = new MsiScoring()
      msiScoring.setSearchEngine( scoring.getString(scoringCols.searchEngine) )
      msiScoring.setName( scoring.getString(scoringCols.name) )
      msiScoring.setDescription( scoring.getString(scoringCols.description) )   
      
      msiEM.persist(msiScoring)
      
    }
  
  }
  
}