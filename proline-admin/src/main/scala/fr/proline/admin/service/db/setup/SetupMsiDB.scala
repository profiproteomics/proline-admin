package fr.proline.admin.service.db.setup

import javax.persistence.Persistence
import scala.collection.JavaConversions.{collectionAsScalaIterable}
import com.typesafe.config.Config
import com.weiglewilczek.slf4s.Logging
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.tables.msi.{MsiDbScoringTable,MsiDbObjectTreeSchemaColumns}
import fr.proline.core.orm.msi.{AdminInformation => MsiAdminInfos, Scoring => MsiScoring, ObjectTreeSchema => MsiSchema }
import fr.proline.util.sql.getTimeAsSQLTimestamp
import fr.proline.repository.IDatabaseConnector

/**
 * @author David Bouyssie
 *
 */
class SetupMsiDB( val dbConnector: IDatabaseConnector,
                  val dbContext: DatabaseConnectionContext,  
                  val dbConfig: DatabaseSetupConfig,
                  val defaults: MsiDBDefaults
                 ) extends ISetupDB with Logging {
  
  lazy val msiEM = dbContext.getEntityManager()
    
  protected def importDefaults() {
    
    //val wasEmOpened = msiEM.isOpen()
    
    // Begin transaction
    val msiTransaction = msiEM.getTransaction()    
    msiTransaction.begin()
    
    // Import Admin information
    this._importAdminInformation()
    this.logger.info( "Admin information imported !" )
    
    // Import scoring definitions
    this._importScorings( this.defaults.scorings )
    this.logger.info( "Scoring definitions imported !" )
    
    // Import schemata
    this._importSchemata( this.defaults.schemata )
    this.logger.info( "Schemata imported !" )
    
    // Commit transaction
    msiTransaction.commit()
    
    // Close entity manager
    //if( !wasEmOpened ) msiEM.close()
  }
  
  private def _importAdminInformation() {

    val msiAdminInfos = new MsiAdminInfos()
    msiAdminInfos.setModelVersion(dbConfig.schemaVersion)
    msiAdminInfos.setDbCreationDate(getTimeAsSQLTimestamp)
    //udsAdminInfos.setModelUpdateDate()
    msiEM.persist( msiAdminInfos)

  }
  
  private def _importScorings( scorings: java.util.List[Config] ) {
    
    val scoringCols = MsiDbScoringTable.columns
    
    // Store scorings
    for( scoring <- scorings ) {
      
      // Create new scoring
      val msiScoring = new MsiScoring()
      msiScoring.setSearchEngine( scoring.getString(scoringCols.SEARCH_ENGINE) )
      msiScoring.setName( scoring.getString(scoringCols.NAME) )
      msiScoring.setDescription( scoring.getString(scoringCols.DESCRIPTION) )   
      
      msiEM.persist(msiScoring)
      
    }
  
  }
  
  private def _importSchemata( schemata: java.util.List[Config] ) {
    
    val otsCols = MsiDbObjectTreeSchemaColumns
    
    // Store schemata
    for( schema <- schemata ) {
      
      // Create new scoring
      val msiSchema = new MsiSchema()
      msiSchema.setName(schema.getString(otsCols.NAME))
      msiSchema.setType(schema.getString(otsCols.TYPE))
      msiSchema.setIsBinaryMode(schema.getBoolean(otsCols.IS_BINARY_MODE))
      msiSchema.setVersion(schema.getString(otsCols.VERSION))
      msiSchema.setSchema("")
      msiSchema.setSerializedProperties(schema.getString(otsCols.SERIALIZED_PROPERTIES))
      
      msiEM.persist(msiSchema)
      
    }
  
  }
  
}