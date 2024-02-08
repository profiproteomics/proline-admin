package fr.proline.admin.service.db.migration

import fr.profi.util.StringUtils
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.msi.{ObjectTreeSchema, Scoring}
import fr.proline.core.orm.msi.PeaklistSoftware

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Map


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
    val oldPklSofts = msiEM.createNativeQuery("SELECT id, name, version, serialized_properties FROM peaklist_software").getResultList
    val softList = new ArrayBuffer[PeaklistSoftware]()
    val oldPklSoftByName = Map.empty[String, PeaklistSoftware]
    oldPklSofts.forEach( aRow => {
      val aSoft = new PeaklistSoftware()
      aSoft.setId(aRow.asInstanceOf[Array[Any]].apply(0).asInstanceOf[Long])
      aSoft.setName(aRow.asInstanceOf[Array[Any]].apply(1).toString)
      val aVersion = aRow.asInstanceOf[Array[Any]].apply(2)
      if(aVersion !=null)
        aSoft.setVersion(aVersion.toString)
      aSoft.setSerializedProperties( aRow.asInstanceOf[Array[Any]].apply(3).toString)
      val fullSoftName = if (aVersion ==null || StringUtils.isEmpty(aVersion.toString)) aSoft.getName
        else aSoft.getName + ' ' + aSoft.getVersion
      oldPklSoftByName += (fullSoftName -> aSoft)
    })


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
      
      val schema = if( oldSchemaByName.contains(schemaNameAsStr) == false ) {
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

}
