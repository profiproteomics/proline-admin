package fr.proline.admin.service.db.migration

import scala.collection.JavaConversions._
import com.typesafe.scalalogging.LazyLogging
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.context._
import fr.proline.core.orm.msi.ObjectTreeSchema
import fr.proline.core.orm.msi.Scoring

/**
 * @author David Bouyssie
 *
 */
class UpgradeMsiDbDefinitions(
  val dbCtx: DatabaseConnectionContext
) extends IUpgradeDB {

  protected def upgradeDefinitions() {
    
    val msiEM = dbCtx.getEntityManager
    
    /*** Upgrade scorings ***/

    // Load existing scorings
    val oldScorings = msiEM.createQuery("SELECT s FROM fr.proline.core.orm.msi.Scoring s", classOf[Scoring]).getResultList

    // Index scorings by their type
    val oldScoringByType = oldScorings.view.map { scoring =>
      scoring.getSearchEngine + ':' + scoring.getName -> scoring
    } toMap
    
    // Iterate over the enumeration of Scoring Types
    for (scoringType <- Scoring.Type.values()) {
      val oldScoringOpt = oldScoringByType.get(scoringType.toString())
      
      val scoring = if( oldScoringOpt.isDefined ) {
        oldScoringOpt.get
      } else {
        logger.info("Inserting new scoring definition: " + scoringType.toString)
        
        val s = new Scoring()
        s.setName(scoringType.getName)
        s.setSearchEngine(scoringType.getSearchEngine)
        
        msiEM.persist(s)
        
        s
      }
      
      scoring.setDescription(scoringType.getDescription)
    }
    
    /*** Insert missing object tree schemas ***/
    
    // Load existing schemas
    val oldSchemas = msiEM.createQuery("SELECT s FROM fr.proline.core.orm.msi.ObjectTreeSchema s", classOf[ObjectTreeSchema]).getResultList

    // Index scorings by their type
    val oldSchemaByName = oldSchemas.view.map { schema =>
      schema.getName -> schema
    } toMap
    
    // Iterate over the enumeration of ObjectTree SchemaNames
    for (schemaName <- ObjectTreeSchema.SchemaName.values()) {
      val schemaNameAsStr = schemaName.name()
      
      val schema = if( oldSchemaByName.contains(schemaNameAsStr) == false ) {
        logger.info("Inserting new object tree schema: " + schemaNameAsStr)
        
        // Create new schema
        val s = new ObjectTreeSchema()
        s.setName(schemaNameAsStr)
        s.setType("JSON")
        s.setIsBinaryMode(false) // FIXME: add to the enum
        s.setSchema("")
        
        msiEM.persist(s)
      }

    }
    
    // Flush current entity manager
    msiEM.flush()
  }

}
