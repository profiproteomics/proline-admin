package fr.proline.admin.service.db.setup

import javax.persistence.EntityManager
import scala.collection.JavaConversions.{collectionAsScalaIterable,setAsJavaSet}
import scala.collection.mutable.ArrayBuffer
import com.typesafe.config.Config
import com.weiglewilczek.slf4s.Logging
import fr.proline.admin.service.db.DatabaseConnectionContext
import fr.proline.util.resources._
import fr.proline.util.sql.getTimeAsSQLTimestamp
import fr.proline.core.dal.tables.uds._
import fr.proline.core.om.model.msi.{ChargeConstraint,
                                     FragmentationSeriesRequirement,
                                     InstrumentConfig,
                                     Fragmentation,
                                     FragmentationRule,
                                     FragmentIonRequirement
                                     }
import fr.proline.core.orm.uds.{Activation => UdsActivation,
                                AdminInformation => UdsAdminInfos,
                                Enzyme => UdsEnzyme,
                                EnzymeCleavage => UdsEnzymeCleavage,
                                ExternalDb => UdsExternalDb,
                                FragmentationRule => UdsFragmentationRule,
                                Instrument => UdsInstrument,
                                InstrumentConfiguration => UdsInstrumentConfig,
                                PeaklistSoftware => UdsPeaklistSoft,
                                QuantitationLabel => UdsQuantLabel,
                                QuantitationMethod => UdsQuantMethod,
                                SpectrumTitleParsingRule => UdsSpecTitleParsingRule,
                                TheoreticalFragment => UdsTheoFragment
                                }
import fr.proline.module.parser.mascot.{EnzymeDefinition,MascotEnzymeParser,
                                        MascotFragmentation,MascotFragmentationRuleParser}

/**
 * @author David Bouyssie
 *
 */
class SetupUdsDB( val udsDbContext: DatabaseConnectionContext,
                  val dbConfig: DatabaseSetupConfig,
                  val prolineConfig: ProlineSetupConfig ) extends ISetupDB with Logging {
  
  // Retrieve defaults config
  protected val defaults = prolineConfig.udsDBDefaults
  
  // Instantiate the UDSdb entity manager
  protected lazy val udsEM = udsDbContext.entityManager
  
  protected def importDefaults() {
    
    // Begin transaction
    udsEM.getTransaction().begin()
    
    // Import Admin information
    this._importAdminInformation()
    this.logger.info( "Admin information imported !" )
    
    // Import external DBs connections
    this._importExternalDBs()
    this.logger.info( "External databases connection settings imported !" )
    
    // Retrieve Mascot configuration resources
    val mascotResources = this.defaults.resources.getConfig("mascot-config")
    val mascotEnzymeFilePath = mascotResources.getString("enzymes_file")    
    val mascotEnzymeIS = pathToStreamOrResourceToStream(mascotEnzymeFilePath,this.getClass())
    val mascotFragRuleFilePath = mascotResources.getString("fragmentation_rules_file")    
    val mascotFragRuleIS = pathToStreamOrResourceToStream(mascotFragRuleFilePath,this.getClass())
    
    // Import enzyme definitions
    val enzymeDefs = MascotEnzymeParser.getEnzymeDefinitions(mascotEnzymeIS)
    this._importMascotEnzymeDefinitions( enzymeDefs )
    this.logger.info( "Enzyme definitions imported !" )
    
    // Import activation types
    val udsActivationByType = this._importActivationTypes()
    this.logger.info( "Activation types imported !" )
    
    // Import theoretical fragments
    val udsTheoFragByKey = this._importTheoreticalFragments()
    this.logger.info( "Theoretical fragments imported !" )
    
    // Import Mascot fragmentation rules
    val udsFragRuleByDesc = this._importMascotFragmentationRules( MascotFragmentation.rules, udsTheoFragByKey )
    this.logger.info( "Mascot fragmentation rules imported !" )
    
    // Import Mascot instrument configurations
    val mascotInstConfigs = MascotFragmentationRuleParser.getInstrumentConfigurations(mascotFragRuleIS)
    this._importMascotInstrumentConfigs( mascotInstConfigs, udsActivationByType, udsFragRuleByDesc )
    this.logger.info( "Mascot instrument configurations imported !" )
    
    // Import Proline instrument configurations
    this._importInstrumentConfigs( this.defaults.instruments, udsActivationByType )
    this.logger.info( "Proline instrument configurations imported !" )
    
    // Import Peaklist software
    this._importPeaklistSoftware( this.defaults.peaklistSoftware )
    this.logger.info( "Peaklist software imported !" )
    
    // Import quantitative methods
    this._importQuantMethods( this.defaults.quantMethods )
    this.logger.info( "Quantitative methods imported !" )
    
    // Commit transaction
    udsEM.getTransaction().commit()
    
    // Close entity manager
    udsEM.close()
    
  }
 
  private def _importAdminInformation() {

    val udsAdminInfos = new UdsAdminInfos()
    udsAdminInfos.setModelVersion(dbConfig.schemaVersion)
    udsAdminInfos.setDbCreationDate(getTimeAsSQLTimestamp)
    //udsAdminInfos.setModelUpdateDate()
    udsAdminInfos.setConfiguration("""{}""")
    
    this.udsEM.persist( udsAdminInfos)

  }
  
  
  private def _importExternalDBs() {

    // Store PSdb connection settings
    this.udsEM.persist( prolineConfig.psDBConfig.toUdsExternalDb() )
    
    // Store PDIdb connection settings
    this.udsEM.persist( prolineConfig.pdiDBConfig.toUdsExternalDb() )    
  }
  
  private def _importMascotEnzymeDefinitions( enzymeDefs: Iterable[EnzymeDefinition] ) {
    
    // Store enzymes
    for( enzymeDef <- enzymeDefs ) {
      
      val udsEnzyme = new UdsEnzyme()
      udsEnzyme.setName(enzymeDef.name)
      udsEnzyme.setIsIndependant( enzymeDef.independent )
      udsEnzyme.setIsSemiSpecific( enzymeDef.semiSpecific )
      
      udsEM.persist(udsEnzyme)
      
      // Store enzyme cleavages
      for( cleavage <- enzymeDef.cleavages) {          
        val site = if( cleavage.isNterm ) "N-term" else "C-term"          
        
        val udsEnzymeCleavage = new UdsEnzymeCleavage()
        udsEnzymeCleavage.setEnzyme(udsEnzyme)
        udsEnzymeCleavage.setSite(site)
        udsEnzymeCleavage.setResidues(cleavage.residues)
        
        if( cleavage.restrict != None )
          udsEnzymeCleavage.setRestrictiveResidues(cleavage.restrict.get)
          
        udsEM.persist(udsEnzymeCleavage)
      }
    }
  
  }
  
  private def _importActivationTypes(): Map[String,UdsActivation] = {
    
    val activationByType = Map.newBuilder[String,UdsActivation]
    
    for( activationType <- fr.proline.core.om.model.msi.Activation.values ) {
      val udsActivation = new UdsActivation()
      udsActivation.setType(activationType.toString)
      udsEM.persist(udsActivation)
      
      activationByType += activationType.toString -> udsActivation
    }
    
    activationByType.result
  }
  
  private def _importTheoreticalFragments(): Map[String,UdsTheoFragment] = {
    
    val udsTheoFragByKey = Map.newBuilder[String,UdsTheoFragment]
    
    for( ionType <- Fragmentation.defaultIonTypes ) {
      
      val udsTheoFrag = new UdsTheoFragment()
      udsTheoFrag.setType(ionType.ionSeries.toString)
      if( ionType.neutralLoss != None )
        udsTheoFrag.setNeutralLoss(ionType.neutralLoss.get.toString)
      
      udsEM.persist( udsTheoFrag )
      
      udsTheoFragByKey += ionType.toString -> udsTheoFrag
    }
    
    udsTheoFragByKey.result
  }
  
  private def _importMascotFragmentationRules(
                fragRules: Seq[FragmentationRule],
                udsTheoFragByKey: Map[String,UdsTheoFragment] ): Map[String,UdsFragmentationRule] = {
    
    val udsFragRuleByDesc = Map.newBuilder[String,UdsFragmentationRule]
    
    // Store fragmentation rules
    for( fragRule <- fragRules ) {
      
      val udsFragRule = new UdsFragmentationRule()
      udsFragRule.setDescription(fragRule.description)
      
      fragRule match {
        // If fragmentation rule is a charge constraint
        case cc: ChargeConstraint => {
          udsFragRule.setFragmentCharge(cc.fragmentCharge)
          if( cc.precursorMinCharge != None )
            udsFragRule.setPrecursorMinCharge(cc.precursorMinCharge.get)
        }
        // If fragmentation rule contains a series requirement
        case fsr: FragmentationSeriesRequirement => {
          
          if( fsr.requiredSeries != null ) {
            val ionSeriesName = fsr.requiredSeries.toString
            val requiredQualityLevel = fsr.requiredSeriesQualityLevel.toString
            //ionFullName .= '-'.requiredSerie.neutralLoss if defined requiredSerie.neutralLoss
            
            val udsTheoFrag = udsTheoFragByKey(ionSeriesName)
            udsFragRule.setRequiredSerieId(udsTheoFrag.getId)
            udsFragRule.setRequiredSerieQualityLevel(requiredQualityLevel)
          }
          
          // If fragmentation rule is a fragment ion requirement
          fsr match {
            case fragRequirement: FragmentIonRequirement => {
              val ionType = fragRequirement.ionType.toString
              val udsTheoFrag = udsTheoFragByKey(ionType)
              
              udsFragRule.setTheoreticalFragment(udsTheoFrag)
              
              if( fragRequirement.fragmentMaxMoz != None )
                udsFragRule.setFragmentMaxMoz(fragRequirement.fragmentMaxMoz.get)
              
              if( fragRequirement.residueConstraint != None )
                udsFragRule.setFragmentResidueConstraint(fragRequirement.residueConstraint.get)  
            }
            case _ => {}
          }
        }
      }
      
      udsEM.persist( udsFragRule )
      
      udsFragRuleByDesc += udsFragRule.getDescription -> udsFragRule
    }
    
    udsFragRuleByDesc.result
  }
  
  private def _importMascotInstrumentConfigs( instConfigs: Seq[InstrumentConfig],
                                              udsActivationByType: Map[String,UdsActivation],
                                              udsFragRuleByDesc: Map[String,UdsFragmentationRule]) {
    
    // Iterate over instrument configs
    for( instConfig <- instConfigs ) {
      
      val instrument = instConfig.instrument
      
      // Create new instrument
      val udsInstrument = new UdsInstrument()
      udsInstrument.setName( instrument.name )
      udsInstrument.setSource( instrument.source )
      udsEM.persist(udsInstrument)      

      // Retrieve activation type and fragmentation rules
      val udsActivation = udsActivationByType(instConfig.activationType)
      val udsFragRules = instConfig.fragmentationRules.get.map( fr => udsFragRuleByDesc(fr.description) ).toSet
      
      // Create new instrument config
      val udsInstrumentConfig = new UdsInstrumentConfig()
      udsInstrumentConfig.setName( instConfig.name )
      udsInstrumentConfig.setMs1Analyzer( instConfig.ms1Analyzer )
      udsInstrumentConfig.setMsnAnalyzer( instConfig.msnAnalyzer )
      udsInstrumentConfig.setSerializedProperties("""{"is_hidden":true}""")
      udsInstrumentConfig.setActivation(udsActivation)
      udsInstrumentConfig.setInstrument(udsInstrument)
      udsInstrumentConfig.setFragmentationRules( udsFragRules )
      
      udsEM.persist(udsInstrumentConfig)
      
      
    }
    
  }
    
  private def _importInstrumentConfigs( instruments: java.util.List[Config],
                                        udsActivationByType: Map[String,UdsActivation] ) {
    
    val instrumentCols = UdsDbInstrumentTable.columns
    val instConfigCols = UdsDbInstrumentConfigTable.columns
    
    // Store instruments
    for( instrument <- instruments ) {
      
      // Create new instrument
      val udsInstrument = new UdsInstrument()
      udsInstrument.setName( instrument.getString(instrumentCols.name) )
      udsInstrument.setSource( instrument.getString(instrumentCols.source) )   
      udsEM.persist(udsInstrument)
      
      // Store instrument configurations
      val instConfigs = instrument.getConfigList("configurations").asInstanceOf[java.util.List[Config]]
      
      for( instConfig <- instConfigs ) {
        
        val udsActivation = udsActivationByType(instConfig.getString(instConfigCols.activationType))
        
        val udsInstrumentConfig = new UdsInstrumentConfig()
        udsInstrumentConfig.setName( instConfig.getString(instConfigCols.name) )
        udsInstrumentConfig.setMs1Analyzer( instConfig.getString(instConfigCols.ms1_analyzer) )
        udsInstrumentConfig.setMsnAnalyzer( instConfig.getString(instConfigCols.msnAnalyzer) )
        udsInstrumentConfig.setSerializedProperties("""{"is_hidden":false}""")
        udsInstrumentConfig.setActivation(udsActivation)
        udsInstrumentConfig.setInstrument(udsInstrument)
        
        udsEM.persist(udsInstrumentConfig)
      }
    }
  
  }

  private def _importPeaklistSoftware( peaklistSoftware: java.util.List[Config] ) {
    
    val peaklistSoftCols = UdsDbPeaklistSoftwareTable.columns
    val parsingRuleCols = UdsDbSpecTitleParsingRuleTable.columns
    val parsingRuleColsList = UdsDbSpecTitleParsingRuleTable.columnsAsStrList.filter { _ != "id" }
    
    for( peaklistSoft <- peaklistSoftware ) {
      
      // Create a peaklist software
      val udsPeaklistSoft = new UdsPeaklistSoft()
      var parsingRuleName = peaklistSoft.getString(peaklistSoftCols.name)
      udsPeaklistSoft.setName(parsingRuleName)
      
      if( peaklistSoft.hasPath(peaklistSoftCols.version)) {
        val version = peaklistSoft.getString(peaklistSoftCols.version)
        udsPeaklistSoft.setVersion(version)
        parsingRuleName += " " + version
      }
      
      // Store spectrum title parsing rule
      val parsingRule = peaklistSoft.getConfig("parsing_rule")
      
      val valueByName = parsingRuleColsList.map { c =>
                          if( parsingRule.hasPath(c)) c -> parsingRule.getString(c)
                          else c -> null
                        } toMap
      
      val udsParsingRule = new UdsSpecTitleParsingRule()
      udsParsingRule.setName(parsingRuleName)
      udsParsingRule.setRawFileName(valueByName(parsingRuleCols.rawFileName))
      udsParsingRule.setFirstCycle(valueByName(parsingRuleCols.firstCycle))
      udsParsingRule.setLastCycle(valueByName(parsingRuleCols.lastCycle))
      udsParsingRule.setFirstScan(valueByName(parsingRuleCols.firstScan))
      udsParsingRule.setLastScan(valueByName(parsingRuleCols.lastScan))
      udsParsingRule.setFirstTime(valueByName(parsingRuleCols.firstTime))
      udsParsingRule.setLastTime(valueByName(parsingRuleCols.lastTime))
      
      udsEM.persist(udsParsingRule)
      
      // Store peaklist software
      udsPeaklistSoft.setSpecTitleParsingRule(udsParsingRule)
      udsEM.persist(udsPeaklistSoft)
      
    }
  
  }
  
  private def _importQuantMethods( quantMethods: java.util.List[Config] ) {
    
    val quantMethodCols = UdsDbQuantMethodTable.columns
    val quantLabelCols = UdsDbQuantLabelTable.columns
    
    // Store quantitative methods
    for( quantMethod <- quantMethods ) {
      
      // Create new quantitative method
      val udsQuantMethod = new UdsQuantMethod()
      udsQuantMethod.setName( quantMethod.getString(quantMethodCols.name) )
      udsQuantMethod.setType( quantMethod.getString(quantMethodCols.`type`) )
      udsQuantMethod.setAbundanceUnit( quantMethod.getString(quantMethodCols.abundanceUnit) )      
      
      udsEM.persist(udsQuantMethod)
      
      // Store quant labels if they are defined
      if( quantMethod.hasPath("quant_labels") ) {
        val quantLabels = quantMethod.getConfigList("quant_labels").asInstanceOf[java.util.List[Config]]
        
        for( quantLabel <- quantLabels ) {
          
          val udsQuantLabel = new UdsQuantLabel()
          udsQuantLabel.setName( quantLabel.getString(quantLabelCols.name) )
          udsQuantLabel.setType( quantMethod.getString(quantMethodCols.`type`) )
          udsQuantLabel.setSerializedProperties( quantLabel.getString(quantLabelCols.serializedProperties) )
          udsQuantLabel.setMethod( udsQuantMethod )
          
          udsEM.persist(udsQuantLabel)
        }
      }
    }
  
  }

  
}
