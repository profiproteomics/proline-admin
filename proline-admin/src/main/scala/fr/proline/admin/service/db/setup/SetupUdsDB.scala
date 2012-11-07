package fr.proline.admin.service.db

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.mutable.ArrayBuffer
import com.typesafe.config.Config
import com.weiglewilczek.slf4s.Logging
import net.noerd.prequel.SQLFormatterImplicits._
import net.noerd.prequel.{StringFormattable,Transaction}

import fr.proline.admin.service.{DatabaseSetupConfig,UdsDBDefaults}
import fr.proline.admin.utils.resources._
import fr.proline.core.dal.SQLFormatterImplicits._
import fr.proline.core.dal.{NullFormattable,UdsDb,UdsDbInstrumentTable,UdsDbInstrumentConfigTable,
                            UdsDbSpecTitleParsingRuleTable,UdsDbEnzymeTable,UdsDbEnzymeCleavageTable}
import fr.proline.core.utils.sql.BoolToSQLStr
import fr.proline.module.parser.mascot.{EnzymeDefinition,MascotEnzymeParser}

/**
 * @author David Bouyssie
 *
 */
class SetupUdsDB( val config: DatabaseSetupConfig,
                  val defaults: UdsDBDefaults ) extends ISetupDB with Logging {
  
  lazy val udsDB = UdsDb(config.connector)
  
  def loadDefaults() {
    
    val udsDbTx = this.udsDB.getOrCreateTransaction()
    
    // Retrieve Mascot configuration resources
    val mascotResources = this.defaults.resources.getConfig("mascot-config")
    val mascotEnzymeFilePath = mascotResources.getString("enzymes_file")    
    val mascotEnzymeFile = pathToFileOrResourceToFile(mascotEnzymeFilePath,this.getClass())
    
    // Load enzyme definitions
    val enzymeDefs = MascotEnzymeParser.getEnzymeDefinitions(mascotEnzymeFile)
    this.importEnzymeDefinitions( udsDbTx, enzymeDefs )
    this.logger.info( "enzyme definitions imported !" )    
    
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
    this.importInstrumentConfigs( udsDbTx, this.defaults.instruments )
    this.logger.info( "instrument configurations imported !" )
    
    // Import spectrum title parsing rules
    this.importSpectrumParsingRules( udsDbTx, this.defaults.spectrumParsingRules )
    this.logger.info( "spectrum title parsing rules imported !" )
    
    // Commit transaction
    udsDbTx.commit
    
  }
  
  def importEnzymeDefinitions( udsDbTx: Transaction, enzymeDefs: Iterable[EnzymeDefinition] ) {
    
    // Build enzyme INSERT query
    val EnzymeTable = UdsDbEnzymeTable    
    val enzymeColsList = EnzymeTable.getColumnsAsStrList().filter { _ != "id" }
    val enzymeInsertQuery = EnzymeTable.makeInsertQuery( enzymeColsList )
    
    // Store enzymes
    val enzymeIds = new ArrayBuffer[Int]
    udsDbTx.executeBatch( enzymeInsertQuery ) { stmt =>
      for( enzymeDef <- enzymeDefs ) {        
        stmt.executeWith( 
          enzymeDef.name,
          Option.empty[String],
          BoolToSQLStr( enzymeDef.independent.getOrElse(false) ),
          BoolToSQLStr( enzymeDef.semiSpecific.getOrElse(false) )
        )
        
        enzymeIds += this.udsDB.extractGeneratedInt( stmt.wrapped )
      }
    }
    
    // Build enzyme cleavage INSERT query
    val CleavageTable = UdsDbEnzymeCleavageTable
    val cleavageColsList = CleavageTable.getColumnsAsStrList().filter { _ != "id" }
    val cleavageInsertQuery = CleavageTable.makeInsertQuery( cleavageColsList )
    
    // Store enzyme cleavage sites
    udsDbTx.executeBatch( cleavageInsertQuery ) { stmt =>
      
      var enzymeDefIdx = 0
      for( enzymeDef <- enzymeDefs ) {
        val enzymeId = enzymeIds(enzymeDefIdx)
        
        for( cleavage <- enzymeDef.cleavages) {          
          val site = if( cleavage.isNterm ) "N-term" else "C-term"          
          
          stmt.executeWith( 
            site,
            cleavage.residues,
            cleavage.restrict,
            enzymeId
          )
        }
        
        enzymeDefIdx += 1        
      }
    }
  
  }
  
  def importInstrumentConfigs( udsDbTx: Transaction, instruments: java.util.List[Config] ) {
    
    // Build instrument INSERT query
    val instrumentColsList = UdsDbInstrumentTable.getColumnsAsStrList( i => List(i.name,i.source)  )
    val instrumentInsertQuery = UdsDbInstrumentTable.makeInsertQuery( instrumentColsList )
    
    val instrumentIds = new ArrayBuffer[Int]
    udsDbTx.executeBatch( instrumentInsertQuery ) { stmt =>
    
      // Store instruments
      for( instrument <- instruments ) {
    
        // Create new instrument
        stmt.executeWith( instrument.getString(instrumentColsList(0)),
                          instrument.getString(instrumentColsList(1))
                        )
        instrumentIds += this.udsDB.extractGeneratedInt( stmt.wrapped )
      }
    }
    
    // Build instrument_config INSERT query
    val instConfigCols = UdsDbInstrumentConfigTable.columns
    val instConfigColsList = UdsDbInstrumentConfigTable.getColumnsAsStrList().filter { _ != "id" }
    val instConfigInsertQuery = UdsDbInstrumentConfigTable.makeInsertQuery( instConfigColsList )
    
    udsDbTx.executeBatch( instConfigInsertQuery ) { stmt =>
    
      // Store instruments
      var instrumentIdx = 0
      for( instrument <- instruments ) {
        val instrumentId = instrumentIds(instrumentIdx)
        val instConfigs = instrument.getConfigList("configurations").asInstanceOf[java.util.List[Config]]
        
        // Store property definitions
        for( instConfig <- instConfigs ) {
          
          stmt.executeWith( instConfig.getString(instConfigCols.name),
                            instConfig.getString(instConfigCols.ms1_analyzer),
                            instConfig.getString(instConfigCols.msnAnalyzer),
                            """{"is_hidden":false}""",
                            instrumentId,
                            instConfig.getString(instConfigCols.activationType)
                          )
        }
        
        instrumentIdx += 1
      }
    }
  
  }

  def importSpectrumParsingRules( udsDbTx: Transaction, parsingRules: java.util.List[Config] ) {
    
    // Build spec_title_parsing_rule INSERT query
    val ParsingRuleTable = UdsDbSpecTitleParsingRuleTable
    val parsingRuleColsList = ParsingRuleTable.getColumnsAsStrList().filter { _ != "id" }
    val parsingRuleInsertQuery = ParsingRuleTable.makeInsertQuery( parsingRuleColsList )
    
    udsDbTx.executeBatch( parsingRuleInsertQuery ) { stmt =>
    
      for( parsingRule <- parsingRules ) {
        
        val values = parsingRuleColsList.map { c =>
                        if( parsingRule.hasPath(c)) StringFormattable(parsingRule.getString(c))
                        else NullFormattable(Option(null))
                      }
        stmt.executeWith( values: _* )
      }
    }
  
  }

/*
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Method: run()
//
def run (): Unit = {
  val (this, arguments ) = _ ////// arguments contains aRGV
  
  ////// Retrieve application parameters
  val verbose = this.verbose
  
  ////// Open a database connection
  val sdbi = this.msiSdbi
  
  ////// Begin new transaction
  sdbi.beginWork or die sdbi.error
  
  ////// Import enzyme definitions
  val enzymeConfigSource = ParseConfig( this.enzymeConfigfile )
  if( enzymeConfigSource(type) == 'mascot' ) {
    require Pairs::Msi::Parser::Mascot::Enzymes
    val enzymeDefParser = new Pairs::Msi::Parser::Mascot::Enzymes()
    val enzymeDefs = enzymeDefParser.getEnzymeDefinitions( enzymeConfigSource(file) )
    this.importEnzymeDefinitions( enzymeDefs )
  }
  else { croak "! yet implemented !" }
  print "enzyme definitions imported !\n" if this.verbose
  
  ////// Import fragmentation rules and instrument configurations
  val instrumentConfigSource = ParseConfig( this.instrumentConfigfile )
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
  else { croak "! yet implemented !" }
  
  //////// Import other instrument configurations
  val otherInstruments = ParseConfig( this.otherInstrumentsConfigfile )
  otherInstruments(instrument) = ( otherInstruments(instrument) ) if ref(otherInstruments(instrument)) == 'HASH'
  this.importOtherInstrumentConfigs( otherInstruments )
  print "other instrument configurations imported !\n" if this.verbose
  
  ////// Import spectrum title parsing rules
  val spectrumParsingConfig = ParseConfig( this.spectrumParsingConfigfile )
  this.importSpectrumParsingRules( spectrumParsingConfig(parsing_rule) )
  print "spectrum title parsing rules imported !\n" if this.verbose
  
  ////// Commit transaction
  sdbi.commit
  
  return 1
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Method: import_enzyme_definitions()
//
def importEnzymeDefinitions (): Unit = {
  val( this, enzymeDefs ) = _
  
  val sdbi = this.msiSdbi
  
  while( val( enzymeName, enzymeDef) = each(enzymeDefs) ) {
    
    ////// Create new enzyme
    sdbi.query('INSERT INTO enzyme VALUES (??)',
                  undef,
                  enzymeName,
                  '',
                  enzymeDef(Independent) ? 't' : 'f',
                  enzymeDef(SemiSpecific) ? 't' : 'f'
                  ) or die sdbi.error
    val enzymeId = sdbi.lastInsertId("","","","")
    
    ////// Iterate over enzyme cleavage definitions
    foreach val enzymeCleavageDef (@{enzymeDef(cleavages)}) {
      val cleavageSite = enzymeCleavageDef(Cterm) ? 'C-term' : 'N-term'
      
      ////// Create new enzyme cleavage
      sdbi.query('INSERT INTO enzyme_cleavage VALUES (??)',
                    undef,
                    cleavageSite,
                    enzymeCleavageDef(Cleavage),
                    enzymeCleavageDef(Restrict),
                    enzymeId
                    ) or die sdbi.error
    }
  }

}

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