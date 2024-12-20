package fr.proline.admin.service.db.migration

import fr.profi.util.StringUtils
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.msi.{ObjectTreeSchema, Scoring}
import fr.proline.core.orm.msi.PeaklistSoftware

import scala.collection.JavaConverters._


/**
 * @author David Bouyssie
 *
 */
class UpgradeMsiDbDefinitions(
  val dbCtx: DatabaseConnectionContext
) extends IUpgradeDB {

  protected def upgradeDefinitions() {
    
    val msiEM = dbCtx.getEntityManager

    /*** Upgrade peaklist software properties ***/
    // Load existing peaklist software
    val oldPklSofts = msiEM.createQuery("SELECT s FROM fr.proline.core.orm.msi.PeaklistSoftware s", classOf[PeaklistSoftware]).getResultList
    val oldPklSoftByName = oldPklSofts.asScala.view.map { soft =>
      val version = soft.getVersion

      val fullSoftName = if (StringUtils.isEmpty(version)) soft.getName
        else soft.getName + ' ' + soft.getVersion
      fullSoftName -> soft
    }.toMap



    // Index parsing rules by corresponding peaklist software
    for(nextSoft <-  fr.proline.core.orm.uds.PeaklistSoftware.SoftwareRelease.values()) {
      val oldPklSoftOpt = oldPklSoftByName.get(nextSoft.toString)
      if( oldPklSoftOpt.isDefined && oldPklSoftOpt.get.getSerializedProperties != nextSoft.getProperties) {
          val oldPklSoft= oldPklSoftOpt.get
          oldPklSoft.setSerializedProperties(nextSoft.getProperties)
          logger.info("Updating peaklist software named " + nextSoft.toString)
          msiEM.merge(oldPklSoft)
      }
    }

    /*** Upgrade scorings ***/

    // Load existing scorings
    val oldScorings = msiEM.createQuery("SELECT s FROM fr.proline.core.orm.msi.Scoring s", classOf[Scoring]).getResultList

    // Index scorings by their type
    val oldScoringByType = oldScorings.asScala.view.map { scoring =>
      scoring.getSearchEngine + ':' + scoring.getName -> scoring
    }.toMap
    
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
      
      if( scoring.getDescription != scoringType.getDescription ) {
        scoring.setDescription(scoringType.getDescription)
      }
      
    }
    
    /*** Insert missing object tree schemas ***/
    
    // Load existing schemas
    val oldSchemas = msiEM.createQuery("SELECT s FROM fr.proline.core.orm.msi.ObjectTreeSchema s", classOf[ObjectTreeSchema]).getResultList

    // Index schemas by their name
    val oldSchemaByName = oldSchemas.asScala.view.map { schema =>
      schema.getName -> schema
    }.toMap
    
    // Iterate over the enumeration of ObjectTree SchemaNames
    for (schemaName <- ObjectTreeSchema.SchemaName.values()) {
      val schemaNameAsStr = schemaName.toString()
      
      if( !oldSchemaByName.contains(schemaNameAsStr) ) {
        logger.info("Inserting new object tree schema: " + schemaNameAsStr)
        
        // Create new schema
        val s = new ObjectTreeSchema()
        s.setName(schemaNameAsStr)
        s.setType("JSON")
        s.setIsBinaryMode(false) // FIXME: add to the enum
        s.setVersion("0.1")
        s.setSchema("")
        
        msiEM.persist(s)
      }

    }
    
    // Flush current entity manager
    msiEM.flush()
  }

  protected def checkUpgradeNeeded() : Boolean = {

    val msiEM = dbCtx.getEntityManager

    /** * Check Upgrade peaklist software properties ** */
    val oldPklSofts = msiEM.createQuery("SELECT s FROM fr.proline.core.orm.msi.PeaklistSoftware s", classOf[PeaklistSoftware]).getResultList
    val oldPklSoftByName = oldPklSofts.asScala.view.map { soft =>
      val version = soft.getVersion

      val fullSoftName = if (StringUtils.isEmpty(version)) soft.getName
      else soft.getName + ' ' + soft.getVersion
      fullSoftName -> soft
    }.toMap

    // Index parsing rules by corresponding peaklist software
    for (nextSoft <- fr.proline.core.orm.uds.PeaklistSoftware.SoftwareRelease.values()) {
      val oldPklSoftOpt = oldPklSoftByName.get(nextSoft.toString)
      if (oldPklSoftOpt.isDefined && oldPklSoftOpt.get.getSerializedProperties != nextSoft.getProperties) {
        logger.info(" Incorrect PeakList Software properties "+oldPklSoftOpt.get.getName )
        return true
      }
    }

    /** * Check Upgrade scorings ** */

    // Load existing scorings
    val oldScorings = msiEM.createQuery("SELECT s FROM fr.proline.core.orm.msi.Scoring s", classOf[Scoring]).getResultList

    // Index scorings by their type
    val oldScoringByType = oldScorings.asScala.view.map { scoring =>
      scoring.getSearchEngine + ':' + scoring.getName -> scoring
    }.toMap

    // Iterate over the enumeration of Scoring Types
    for (scoringType <- Scoring.Type.values()) {
      val oldScoringOpt = oldScoringByType.get(scoringType.toString())

      val scoring = if (oldScoringOpt.isDefined) {
        if(oldScoringOpt.get.getDescription != scoringType.getDescription) {
          logger.info(" Incorrect Scoring description "+oldScoringOpt.get.getName )
          return true
        }
      } else {
        logger.info(" Undefined Scoring  "+scoringType.toString() )
        return true
      }
    }

    /** * Check missing object tree schemas ** */

    // Load existing schemas
    val oldSchemas = msiEM.createQuery("SELECT s FROM fr.proline.core.orm.msi.ObjectTreeSchema s", classOf[ObjectTreeSchema]).getResultList

    // Index schemas by their name
    val oldSchemaByName = oldSchemas.asScala.view.map { schema =>
      schema.getName -> schema
    }.toMap

    // Iterate over the enumeration of ObjectTree SchemaNames
    for (schemaName <- ObjectTreeSchema.SchemaName.values()) {
      val schemaNameAsStr = schemaName.toString()

      if (!oldSchemaByName.contains(schemaNameAsStr)) {
        logger.info(" Undefined schemaName  "+schemaNameAsStr )
        return true
      }
    }

    false
  }
}
