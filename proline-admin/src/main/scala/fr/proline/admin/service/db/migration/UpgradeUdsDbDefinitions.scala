package fr.proline.admin.service.db.migration

import com.typesafe.scalalogging.LazyLogging
import fr.profi.util.StringUtils
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.uds.{ObjectTreeSchema => UdsObjectTreeSchema}
import fr.proline.core.orm.uds.{PeaklistSoftware => UdsPeaklistSoftware}
import fr.proline.core.orm.uds.{SpectrumTitleParsingRule => UdsSpectrumTitleParsingRule}

import scala.collection.JavaConversions._

/**
 * @author David Bouyssie
 *
 */

class UpgradeUdsDbDefinitions(
  val dbCtx: DatabaseConnectionContext
) extends IUpgradeDB with LazyLogging {

  protected def upgradeDefinitions() {

    // TODO: update quant methods
    
    // Upgrade object tree schema
    val udsEM = dbCtx.getEntityManager
    
    /*** Upgrade peaklist software and spectrum title parsing rules ***/

    // Index parsing rules by corresponding peaklist software
    val parsingRuleByPeaklistSoft = UdsSpectrumTitleParsingRule.ParsingRule.values().view.map { parsingRule =>
      parsingRule.getPeaklistSoftware -> parsingRule
    } toMap
    
    // Load existing peaklist software
    val oldPklSofts = udsEM.createQuery("SELECT s FROM fr.proline.core.orm.uds.PeaklistSoftware s", classOf[UdsPeaklistSoftware]).getResultList
    
    // Index existing peaklist software by their name
    val oldPklSoftByName = oldPklSofts.view.map { soft =>
      val version = soft.getVersion
      
			val fullSoftName = if (StringUtils.isEmpty(version)) soft.getName
			else soft.getName + ' ' + soft.getVersion 
				
			fullSoftName-> soft
    } toMap
    
    // Iterate over the enumeration of Scoring Types
    for (softRelease <- UdsPeaklistSoftware.SoftwareRelease.values()) {
      val oldPklSoftOpt = oldPklSoftByName.get(softRelease.toString)
      
      // Retrieve the last parsing rule definition from the ORM to update id
      val parsingRule = parsingRuleByPeaklistSoft(softRelease)
      
      if( oldPklSoftOpt.isDefined ) {        
        val oldPklSoft = oldPklSoftOpt.get
        val oldRule = oldPklSoft.getSpecTitleParsingRule
        
        var updateRule = false
        if (oldRule.getFirstCycle != parsingRule.getFirstCycleRegex) {
          oldRule.setFirstCycle(parsingRule.getFirstCycleRegex)
          updateRule = true
        }
        if (oldRule.getLastCycle != parsingRule.getLastCycleRegex) {
          oldRule.setLastCycle(parsingRule.getLastCycleRegex)
          updateRule = true
        }
        if (oldRule.getFirstScan != parsingRule.getFirstScanRegex) {
          oldRule.setFirstScan(parsingRule.getFirstScanRegex)
          updateRule = true
        }
        if (oldRule.getLastScan != parsingRule.getLastScanRegex) {
          oldRule.setLastScan(parsingRule.getLastScanRegex)
          updateRule = true
        }
        if (oldRule.getFirstTime != parsingRule.getFirstTimeRegex) {
          oldRule.setFirstTime(parsingRule.getFirstTimeRegex)
          updateRule = true
        }
        if (oldRule.getLastTime != parsingRule.getLastTimeRegex) {
          oldRule.setLastTime(parsingRule.getLastTimeRegex)
          updateRule = true
        }
        if (oldRule.getRawFileIdentifier != parsingRule.getRawFileIdentifierRegex) {
          oldRule.setRawFileIdentifier(parsingRule.getRawFileIdentifierRegex)
          updateRule = true
        }
        
        if(updateRule) {
          logger.info("Updating parsing rule of peaklist software named " + softRelease.toString)
        }
        
      } else {
        logger.info("Inserting new Peaklist Software: " + softRelease.toString)
        
        val newSpecTitleParsingRule = new UdsSpectrumTitleParsingRule()
        newSpecTitleParsingRule.setFirstCycle(parsingRule.getFirstCycleRegex)
        newSpecTitleParsingRule.setLastCycle(parsingRule.getLastCycleRegex)
        newSpecTitleParsingRule.setFirstScan(parsingRule.getFirstScanRegex)
        newSpecTitleParsingRule.setLastScan(parsingRule.getLastScanRegex)
        newSpecTitleParsingRule.setFirstTime(parsingRule.getFirstTimeRegex)
        newSpecTitleParsingRule.setLastTime(parsingRule.getLastTimeRegex)
        newSpecTitleParsingRule.setRawFileIdentifier(parsingRule.getRawFileIdentifierRegex)
        udsEM.persist(newSpecTitleParsingRule)
        
        val newPklSoft = new UdsPeaklistSoftware()
        newPklSoft.setName(softRelease.getName)
        newPklSoft.setVersion(softRelease.getVersion)
        newPklSoft.setSpecTitleParsingRule(newSpecTitleParsingRule)        
        udsEM.persist(newPklSoft)
        
        newSpecTitleParsingRule
      }
      
    }
    
    /*** Insert missing object tree schemas ***/
    
    // Load existing schemas
    val oldSchemas = udsEM.createQuery("SELECT s FROM fr.proline.core.orm.uds.ObjectTreeSchema s", classOf[UdsObjectTreeSchema]).getResultList

    // Index schemas by their name
    val oldSchemaByName = oldSchemas.view.map { schema =>
      schema.getName -> schema
    } toMap
    
    // Iterate over the enumeration of ObjectTree SchemaNames
    for (schemaName <- UdsObjectTreeSchema.SchemaName.values()) {
      val schemaNameAsStr = schemaName.toString()
      
      if( !oldSchemaByName.contains(schemaNameAsStr) ) {
        logger.info("Inserting new object tree schema: " + schemaNameAsStr)
        
        // Create new schema
        val s = new UdsObjectTreeSchema()
        s.setName(schemaNameAsStr)
        s.setType("JSON")
        s.setIsBinaryMode(false) // FIXME: add to the enum
        s.setVersion("0.1")
        s.setSchema("")
        
        udsEM.persist(s)
      }

    }
    
    // Flush current entity manager
    udsEM.flush()

  }

}
