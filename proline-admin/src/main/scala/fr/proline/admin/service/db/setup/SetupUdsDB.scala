package fr.proline.admin.service.db.setup

import javax.persistence.EntityManager
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import com.typesafe.config.Config
import com.weiglewilczek.slf4s.Logging

import fr.proline.admin.utils.resources._
import fr.proline.core.dal.{DatabaseManagement,
                            UdsDbInstrumentTable,UdsDbInstrumentConfigTable,
                            UdsDbPeaklistSoftwareTable,UdsDbSpecTitleParsingRuleTable}
import fr.proline.core.orm.uds.{Activation,Instrument,InstrumentConfiguration,
                                PeaklistSoftware,SpectrumTitleParsingRule,
                                Enzyme,EnzymeCleavage}

import fr.proline.module.parser.mascot.{EnzymeDefinition,MascotEnzymeParser}

/**
 * @author David Bouyssie
 *
 */
class SetupUdsDB( val dbManager: DatabaseManagement,
                  val config: DatabaseSetupConfig,
                  val defaults: UdsDBDefaults ) extends ISetupDB with Logging {
  
  // Instantiate hte UDSdb entity manager
  protected val udsEMF = dbManager.udsEMF
  protected val udsEM = udsEMF.createEntityManager()
    
  protected def importDefaults() {
    
    // Begin transaction
    udsEM.getTransaction().begin()
    
    // Retrieve Mascot configuration resources
    val mascotResources = this.defaults.resources.getConfig("mascot-config")
    val mascotEnzymeFilePath = mascotResources.getString("enzymes_file")    
    val mascotEnzymeFile = pathToFileOrResourceToFile(mascotEnzymeFilePath,this.getClass())
    
    // Import enzyme definitions
    val enzymeDefs = MascotEnzymeParser.getEnzymeDefinitions(mascotEnzymeFile)
    this._importMascotEnzymeDefinitions( enzymeDefs )
    this.logger.info( "enzyme definitions imported !" )
    
    // Import activation types
    val udsActivationByType = this._importActivationTypes()
    
    // Import fragmentation rules and instrument configurations
    /*val instrumentConfigSource = ParseConfig( this.instrumentConfigfile )
    if( instrumentConfigSource(type) == 'mascot' )
      {
      require Pairs::Msi::Helper::Mascot
      val mascotHelper = new Pairs::Msi::Helper::Mascot()
      val fragRules = this.importFragmentationRules( mascotHelper.getFragmentationRules )
      
      require Pairs::Msi::Parser::Mascot::FragmentationRules
      val fragRuleParser = new Pairs::Msi::Parser::Mascot::FragmentationRules()
      val instrumentFragmentationRules = fragRuleParser.getInstrumentFragmentationRules( instrumentConfigSource(file) )  
      this.importInstrumentConfigurations( instrumentFragmentationRules, fragRules )
      
      print "mascot instrument configurations imported !\n" if this.verbose
      }
    else { croak "! yet implemented !" }*/
    
    // Import other instrument configurations
    this._importInstrumentConfigs( this.defaults.instruments, udsActivationByType )
    this.logger.info( "instrument configurations imported !" )
    
    // Import spectrum title parsing rules
    this._importPeaklistSoftware( this.defaults.peaklistSoftware )
    this.logger.info( "spectrum title parsing rules imported !" )
    
    // Commit transaction
    udsEM.getTransaction().commit()
    
    // Close entity manager
    udsEM.close()
    
  }
  
  private def _importMascotEnzymeDefinitions( enzymeDefs: Iterable[EnzymeDefinition] ) {
    
    // Store enzymes
    for( enzymeDef <- enzymeDefs ) {
      
      val udsEnzyme = new Enzyme()
      udsEnzyme.setName(enzymeDef.name)
      udsEnzyme.setIsIndependant( enzymeDef.independent )
      udsEnzyme.setIsSemiSpecific( enzymeDef.semiSpecific )
      
      udsEM.persist(udsEnzyme)
      
      // Store enzyme cleavages
      for( cleavage <- enzymeDef.cleavages) {          
        val site = if( cleavage.isNterm ) "N-term" else "C-term"          
        
        val udsEnzymeCleavage = new EnzymeCleavage()
        udsEnzymeCleavage.setEnzyme(udsEnzyme)
        udsEnzymeCleavage.setSite(site)
        udsEnzymeCleavage.setResidues(cleavage.residues)
        
        if( cleavage.restrict != None )
          udsEnzymeCleavage.setRestrictiveResidues(cleavage.restrict.get)
          
        udsEM.persist(udsEnzymeCleavage)
      }
    }
  
  }
  
  private def _importActivationTypes(): Map[String,Activation] = {
    
    val activationTypes = Array("CID","HCD","ETD","ECD","PSD")
    val activationByType = Map.newBuilder[String,Activation]
    
    for( activationType <- activationTypes ) {
      val udsActivation = new Activation()
      udsActivation.setType(activationType)
      udsEM.persist(udsActivation)
      
      activationByType += activationType -> udsActivation
    }
    
    activationByType.result
  }
  
  private def _importMascotInstrumentConfigs(
                 instruments: java.util.List[Config],
                 udsActivationByType: Map[String,Activation] ) {
    
  }
    
  private def _importInstrumentConfigs( instruments: java.util.List[Config],
                                        udsActivationByType: Map[String,Activation] ) {
    
    val instrumentCols = UdsDbInstrumentTable.columns
    val instConfigCols = UdsDbInstrumentConfigTable.columns
    
    // Store instruments
    for( instrument <- instruments ) {
  
      // Create new instrument
      val udsInstrument = new Instrument()
      udsInstrument.setName( instrument.getString(instrumentCols.name) )
      udsInstrument.setSource( instrument.getString(instrumentCols.source) )   
      udsEM.persist(udsInstrument)
      
      // Store instrument configurations
      val instConfigs = instrument.getConfigList("configurations").asInstanceOf[java.util.List[Config]]
      
      for( instConfig <- instConfigs ) {
        
        val udsActivation = udsActivationByType(instConfig.getString(instConfigCols.activationType))
        
        val udsInstrumentConfig = new InstrumentConfiguration()
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
    val parsingRuleColsList = UdsDbSpecTitleParsingRuleTable.getColumnsAsStrList().filter { _ != "id" }
    
    for( peaklistSoft <- peaklistSoftware ) {
      
      // Create a peaklist software
      val udsPeaklistSoft = new PeaklistSoftware()
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
      
      val udsParsingRule = new SpectrumTitleParsingRule()
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

/*

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Method: import_fragmentation_rules()
//
def importFragmentationRules (): Unit = {
  val( this, fragmentationRules ) = _
  
  val sdbi = this.msiSdbi
  
  ////// Load MS2 observable ions
  require Pairs::Msi::Helper::MassSpec
  val massSpecHelper = new Pairs::Msi::Helper::MassSpec()
  val ms2_observableIons = massSpecHelper.getMs2_observableIons()
  
  ////// Save MS2 observable ions
  val theoFragIdByName
  while( val( ionFullName, ms2_observableIon ) = each(ms2_observableIons) ) {
    
    sdbi.query('INSERT INTO theoretical_fragment VALUES (??)',
                  undef,
                  ms2_observableIon.type,
                  ms2_observableIon.neutralLoss,
                  undef
                  ) or die sdbi.error    
    
    theoFragIdByName(ionFullName) = sdbi.lastInsertId("","","","")
  }
  
  ////// Save fragmentation rules
  val fragmentationRules
  for( fragmentationRule <- fragmentationRules ) {
    
    val( fragRule ) = ( description = fragmentationRule.description )
    val fragRuleClass = blessed( fragmentationRule )
    
    ////// Import rule paramaters 
    if( fragRuleClass =~ /ChargeConstraint/ )
      {
      fragRule(precursor_min_charge) = fragmentationRule.precursorMinCharge
      fragRule(fragment_charge) = fragmentationRule.fragmentCharge
      }
    else
      {
      val requiredSerie = fragmentationRule.requiredSerie
      if( defined requiredSerie )
        {
        val ionSerieName = requiredSerie.asString
        //ionFullName .= '-'.requiredSerie.neutralLoss if defined requiredSerie.neutralLoss
        
        val theoFragId = theoFragIdByName(ionSerieName)
        fragRule(required_serie_id) = theoFragId
        fragRule(required_serie_quality_level) = fragmentationRule.requiredSerieQualityLevel
        }
        
      if( fragRuleClass =~ /TheoreticalMs2Ion/ )
        {
        val ionName = fragmentationRule.theoreticalIon.asString
        val theoFragId = theoFragIdByName(ionName)
        
        fragRule(theoretical_fragment_id) = theoFragId      
        fragRule(fragment_max_moz) = fragmentationRule.fragmentMaxMoz
        fragRule(fragment_residue_constraint) = fragmentationRule.residueConstraint      
        }
      }
      
    //die Dumper fragRule
    
    sdbi.query('INSERT INTO fragmentation_rule VALUES (??)',
                  undef,
                  fragRule(description),
                  fragRule(precursor_min_charge),
                  fragRule(fragment_charge),
                  fragRule(fragment_max_moz),
                  fragRule(fragment_residue_constraint),
                  fragRule(required_serie_quality_level),
                  undef,
                  fragRule(theoretical_fragment_id),
                  fragRule(required_serie_id),
                  ) or die sdbi.error
      
    fragRule(id) = sdbi.lastInsertId("","","","")
    
    push( fragmentationRules, fragRule )
  }
  
  return fragmentationRules
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Method: import_instrument_configurations()
//
def importInstrumentConfigurations (): Unit = {
  val( this, instrumentConfigs, fragmentationRules ) = _
  
  val sdbi = this.msiSdbi
  
  ////// Create activation types
  sdbi.query('INSERT INTO activation VALUES (?)', _) for qw/CID HCD ETD ECD PSD/
  
  ////// Map fragmentation rules by their description
  val fragRuleByDesc = map { _(description) = _ } fragmentationRules
  
  ////// Iterate over instrument fragmentation rules
  while( val( instrumentType, fragRuleDefs ) = each(instrumentConfigs) ) {
    val( instrumentSource, analyzer1, activationType, analyzer2 ) = ( 'ESI', undef, 'CID', undef )
    
    ////// Note: this is mascot specifc = TODO: put this in a Mascot class
    ////// Retrieve source
    val instrumentParts = split('-', instrumentType )
    next if scalar(instrumentParts) == 1 ////// skip Default and All
    
    if( instrumentParts(0) == 'ESI' or instrumentParts(0) == 'MALDI' ) { instrumentSource = instrumentParts(0) }
    else if( instrumentParts(0) == 'ETD' ) { activationType = 'ETD' }
  
    ////// Retrieve activation type
    if( defined instrumentParts(1) and instrumentParts(1) == 'ECD' ) { activationType = 'ECD' }
    else if( defined instrumentParts(2) and instrumentParts(2) == 'PSD' ) { activationType = 'PSD' }
    
    if( instrumentType =~ /TOF-TOF/ ) { ( analyzer1, analyzer2 ) = ( 'TOF', 'TOF') }
    else if( instrumentType =~ /QUAD-TOF/ ) { ( analyzer1, analyzer2 ) = ( 'QUAD', 'TOF') }
    else if( instrumentType == 'FTMS-ECD' ) { ( analyzer1, analyzer2 ) = ( 'FTMS', 'FTMS') }
    else { ( analyzer1, analyzer2 ) = ( instrumentParts(1), instrumentParts(1)) }  
    
    ////// Create new instrument
    sdbi.query('INSERT INTO instrument VALUES (??)', undef, instrumentType, instrumentSource, undef ) or die sdbi.error
    val instrumentId = sdbi.lastInsertId("","","","")
    
    ////// Create new instrument config
    val instrumentConfigName = sprintf("s (A1=s F=s A2=s)", instrumentType, analyzer1, activationType, analyzer2 )
    sdbi.query('INSERT INTO instrument_config VALUES (??)',
                 undef,
                 instrumentConfigName,
                 analyzer1,
                 analyzer2,
                 '{"is_hidden":true}',
                 instrumentId,
                 activationType                 
                 ) or die sdbi.error
    val instrumentConfigId = sdbi.lastInsertId("","","","")
    
    ////// Iterate over fragmentation rules
    for( fragRuleDef <- fragRuleDefs ) {
      val fragRule = fragRuleByDesc(fragRuleDef.description)
      
      sdbi.query('INSERT INTO instrument_config_fragmentation_rule_map VALUES (??)',
                   instrumentConfigId,
                   fragRule(id)
                  ) or die sdbi.error
    }
  }
  
}

*/
  
}