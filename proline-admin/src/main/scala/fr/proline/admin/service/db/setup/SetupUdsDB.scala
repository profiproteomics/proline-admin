package fr.proline.admin.service.db.setup

import javax.persistence.EntityManager
import scala.collection.JavaConversions.{ collectionAsScalaIterable, setAsJavaSet }
import scala.collection.mutable.ArrayBuffer
import com.typesafe.config.Config
import com.typesafe.scalalogging.slf4j.Logging
import fr.proline.context.DatabaseConnectionContext
import fr.proline.util.resources._
import fr.proline.util.sql.getTimeAsSQLTimestamp
import fr.proline.core.dal.tables.uds._
import fr.proline.core.om.model.msi.{
  ChargeConstraint,
  FragmentationSeriesRequirement,
  InstrumentConfig,
  Fragmentation,
  FragmentationRule,
  FragmentIonRequirement
}
import fr.proline.core.orm.uds.{
  Activation => UdsActivation,
  AdminInformation => UdsAdminInfos,
  Aggregation => UdsAggregation,
  Enzyme => UdsEnzyme,
  EnzymeCleavage => UdsEnzymeCleavage,
  ExternalDb => UdsExternalDb,
  Fractionation => UdsFractionation,
  FragmentationRule => UdsFragmentationRule,
  FragmentationSeries => UdsFragmentationSeries,
  Instrument => UdsInstrument,
  InstrumentConfiguration => UdsInstrumentConfig,
  PeaklistSoftware => UdsPeaklistSoft,
  QuantitationLabel => UdsQuantLabel,
  QuantitationMethod => UdsQuantMethod,
  SpectrumTitleParsingRule => UdsSpecTitleParsingRule
}
import fr.proline.module.parser.mascot.{
  EnzymeDefinition,
  MascotEnzymeParser,
  MascotFragmentation,
  MascotFragmentationRuleParser
}
import fr.proline.repository.IDatabaseConnector
import javax.persistence.EntityTransaction
import javax.persistence.EntityManager

/**
 * @author David Bouyssie
 *
 */

/* Cannot use DatabaseConnectionContext here because Db Shema does not exist yet ! */
class SetupUdsDB(val dbConnector: IDatabaseConnector,
                 val dbConfig: DatabaseSetupConfig,
                 val prolineConfig: ProlineSetupConfig) extends ISetupDB with Logging {

  // Retrieve defaults config
  protected val defaults = prolineConfig.udsDBDefaults

  protected def importDefaults() {
    val udsEMF = dbConnector.getEntityManagerFactory()

    var udsTransaction: EntityTransaction = null
    var udsTransacOK: Boolean = false

    val udsEM = udsEMF.createEntityManager()

    try {
      // Begin transaction
      udsTransaction = udsEM.getTransaction
      udsTransaction.begin()
      udsTransacOK = false

      // Import Admin information
      _importAdminInformation(udsEM)
      logger.info("Admin information imported !")

      // Import external DBs connections
      _importExternalDBs(udsEM)
      logger.info("External databases connection settings imported !")

      // Retrieve Mascot configuration resources
      val mascotResources = defaults.resources.getConfig("mascot-config")
      val mascotEnzymeFilePath = mascotResources.getString("enzymes_file")
      val mascotEnzymeIS = pathToStreamOrResourceToStream(mascotEnzymeFilePath, getClass())
      val mascotFragRuleFilePath = mascotResources.getString("fragmentation_rules_file")
      val mascotFragRuleIS = pathToStreamOrResourceToStream(mascotFragRuleFilePath, getClass())

      // Import activation types
      val udsActivationByType = _importActivationTypes(udsEM)
      logger.info("Activation types imported !")

      // Import fractionation types
      _importFractionationTypes(udsEM)
      logger.info("Fractionation types imported !")

      // Import aggregation child natures 
      _importAggregationChildNatures(udsEM)
      logger.info("Aggregation child natures imported !")

      // Import fragmentataion series
      val udsFragSeriesByKey = _importFragmentationSeries(udsEM)
      logger.info("Fragmentation series imported !")

      // Import Mascot fragmentation rules
      val udsFragRuleByDesc = _importMascotFragmentationRules(udsEM, MascotFragmentation.rules, udsFragSeriesByKey)
      logger.info("Mascot fragmentation rules imported !")

      // Import enzyme definitions
      val enzymeDefs = MascotEnzymeParser.getEnzymeDefinitions(mascotEnzymeIS)
      _importMascotEnzymeDefinitions(udsEM, enzymeDefs)
      logger.info("Enzyme definitions imported !")

      // Import Mascot instrument configurations
      val mascotInstConfigs = MascotFragmentationRuleParser.getInstrumentConfigurations(mascotFragRuleIS)
      _importMascotInstrumentConfigs(udsEM, mascotInstConfigs, udsActivationByType, udsFragRuleByDesc)
      logger.info("Mascot instrument configurations imported !")

      // Import Proline instrument configurations
      _importInstrumentConfigs(udsEM, defaults.instruments, udsActivationByType)
      logger.info("Proline instrument configurations imported !")

      // Import Peaklist software
      _importPeaklistSoftware(udsEM, defaults.peaklistSoftware)
      logger.info("Peaklist software imported !")

      // Import quantitative methods
      _importQuantMethods(udsEM, defaults.quantMethods)
      logger.info("Quantitative methods imported !")

      // Commit transaction
      udsTransaction.commit()
      udsTransacOK = true
    } finally {

      if ((udsTransaction != null) && !udsTransacOK) {
        logger.info("Rollbacking UDS Db EntityTransaction")

        try {
          udsTransaction.rollback()
        } catch {
          case ex: Exception => logger.error("Error rollbacking UDS Db EntityTransaction")
        }

      }

      if (udsEM != null) {
        try {
          udsEM.close()
        } catch {
          case exClose: Exception => logger.error("Error closing UDS Db EntityManager")
        }
      }

    }

  }

  private def _importAdminInformation(udsEM: EntityManager) {

    val udsAdminInfos = new UdsAdminInfos()
    udsAdminInfos.setModelVersion(dbConfig.schemaVersion)
    udsAdminInfos.setDbCreationDate(getTimeAsSQLTimestamp)
    //udsAdminInfos.setModelUpdateDate()
    udsAdminInfos.setConfiguration("""{}""")

    udsEM.persist(udsAdminInfos)

  }

  private def _importExternalDBs(udsEM: EntityManager) {

    // Store PSdb connection settings
    udsEM.persist(prolineConfig.psDBConfig.toUdsExternalDb())

    // Store PDIdb connection settings
    udsEM.persist(prolineConfig.pdiDBConfig.toUdsExternalDb())
  }

  private def _importActivationTypes(udsEM: EntityManager): Map[String, UdsActivation] = {

    val activationByType = Map.newBuilder[String, UdsActivation]

    for (activationType <- UdsActivation.ActivationType.values) {
      val udsActivation = new UdsActivation()
      udsActivation.setType(activationType)
      udsEM.persist(udsActivation)

      activationByType += activationType.toString -> udsActivation
    }

    activationByType.result
  }

  private def _importFractionationTypes(udsEM: EntityManager) {

    for (fractionationType <- UdsFractionation.FractionationType.values) {
      val udsFractionation = new UdsFractionation()
      udsFractionation.setType(fractionationType)
      udsEM.persist(udsFractionation)
    }

  }

  private def _importAggregationChildNatures(udsEM: EntityManager) {

    for (aggregationChildNature <- UdsAggregation.ChildNature.values) {
      val udsAggregation = new UdsAggregation()
      udsAggregation.setChildNature(aggregationChildNature)
      udsEM.persist(udsAggregation)
    }

  }

  private def _importFragmentationSeries(udsEM: EntityManager): Map[String, UdsFragmentationSeries] = {

    val udsFragSeriesByKey = Map.newBuilder[String, UdsFragmentationSeries]

    for (ionType <- Fragmentation.defaultIonTypes) {

      val udsFragSeries = new UdsFragmentationSeries()
      udsFragSeries.setName(ionType.ionSeries.toString)
      if (ionType.neutralLoss != None)
        udsFragSeries.setNeutralLoss(ionType.neutralLoss.get.toString)

      udsEM.persist(udsFragSeries)

      udsFragSeriesByKey += ionType.toString -> udsFragSeries
    }

    udsFragSeriesByKey.result
  }

  private def _importMascotFragmentationRules(udsEM: EntityManager,
                                              fragRules: Seq[FragmentationRule],
                                              udsFragSeriesByKey: Map[String, UdsFragmentationSeries]): Map[String, UdsFragmentationRule] = {

    val udsFragRuleByDesc = Map.newBuilder[String, UdsFragmentationRule]

    // Store fragmentation rules
    for (fragRule <- fragRules) {

      val udsFragRule = new UdsFragmentationRule()
      udsFragRule.setDescription(fragRule.description)

      fragRule match {
        // If fragmentation rule is a charge constraint
        case cc: ChargeConstraint => {
          udsFragRule.setFragmentCharge(cc.fragmentCharge)
          if (cc.precursorMinCharge != None)
            udsFragRule.setPrecursorMinCharge(cc.precursorMinCharge.get)
        }
        // If fragmentation rule contains a series requirement
        case fsr: FragmentationSeriesRequirement => {

          if (fsr.requiredSeries != null) {
            val ionSeriesName = fsr.requiredSeries.toString
            val requiredQualityLevel = fsr.requiredSeriesQualityLevel.toString
            //ionFullName .= '-'.requiredSerie.neutralLoss if defined requiredSerie.neutralLoss

            val udsFragSeries = udsFragSeriesByKey(ionSeriesName)
            udsFragRule.setRequiredSeriesId(udsFragSeries.getId)
            udsFragRule.setRequiredSeriesQualityLevel(requiredQualityLevel)
          }

          // If fragmentation rule is a fragment ion requirement
          fsr match {
            case fragRequirement: FragmentIonRequirement => {
              val ionType = fragRequirement.ionType.toString
              val udsFragSeries = udsFragSeriesByKey(ionType)

              udsFragRule.setFragmentationSeries(udsFragSeries)

              if (fragRequirement.fragmentMaxMoz != None)
                udsFragRule.setFragmentMaxMoz(fragRequirement.fragmentMaxMoz.get)

              if (fragRequirement.residueConstraint != None)
                udsFragRule.setFragmentResidueConstraint(fragRequirement.residueConstraint.get)
            }
            case _ => {}
          }
        }
      }

      udsEM.persist(udsFragRule)

      udsFragRuleByDesc += udsFragRule.getDescription -> udsFragRule
    }

    udsFragRuleByDesc.result
  }

  private def _importMascotEnzymeDefinitions(udsEM: EntityManager, enzymeDefs: Iterable[EnzymeDefinition]) {

    // Store enzymes
    for (enzymeDef <- enzymeDefs) {

      val udsEnzyme = new UdsEnzyme()
      udsEnzyme.setName(enzymeDef.name)
      udsEnzyme.setIsIndependant(enzymeDef.independent)
      udsEnzyme.setIsSemiSpecific(enzymeDef.semiSpecific)

      udsEM.persist(udsEnzyme)

      // Store enzyme cleavages
      for (cleavage <- enzymeDef.cleavages) {
        val site = if (cleavage.isNterm) "N-term" else "C-term"

        val udsEnzymeCleavage = new UdsEnzymeCleavage()
        udsEnzymeCleavage.setEnzyme(udsEnzyme)
        udsEnzymeCleavage.setSite(site)
        udsEnzymeCleavage.setResidues(cleavage.residues)

        if (cleavage.restrict != None)
          udsEnzymeCleavage.setRestrictiveResidues(cleavage.restrict.get)

        udsEM.persist(udsEnzymeCleavage)
      }
    }

  }

  private def _importMascotInstrumentConfigs(udsEM: EntityManager, instConfigs: Seq[InstrumentConfig],
                                             udsActivationByType: Map[String, UdsActivation],
                                             udsFragRuleByDesc: Map[String, UdsFragmentationRule]) {

    // Iterate over instrument configs
    for (instConfig <- instConfigs) {

      val instrument = instConfig.instrument

      // Create new instrument
      val udsInstrument = new UdsInstrument()
      udsInstrument.setName(instrument.name)
      udsInstrument.setSource(instrument.source)
      udsEM.persist(udsInstrument)

      // Retrieve activation type and fragmentation rules
      val udsActivation = udsActivationByType(instConfig.activationType)
      val udsFragRules = instConfig.fragmentationRules.get.map(fr => udsFragRuleByDesc(fr.description)).toSet

      // Create new instrument config
      val udsInstrumentConfig = new UdsInstrumentConfig()
      udsInstrumentConfig.setName(instConfig.name)
      udsInstrumentConfig.setMs1Analyzer(instConfig.ms1Analyzer)
      udsInstrumentConfig.setMsnAnalyzer(instConfig.msnAnalyzer)
      udsInstrumentConfig.setSerializedProperties("""{"is_hidden":true}""")
      udsInstrumentConfig.setActivation(udsActivation)
      udsInstrumentConfig.setInstrument(udsInstrument)
      udsInstrumentConfig.setFragmentationRules(udsFragRules)

      udsEM.persist(udsInstrumentConfig)

    }

  }

  private def _importInstrumentConfigs(udsEM: EntityManager, instruments: java.util.List[Config],
                                       udsActivationByType: Map[String, UdsActivation]) {

    val instrumentCols = UdsDbInstrumentTable.columns
    val instConfigCols = UdsDbInstrumentConfigTable.columns

    // Store instruments
    for (instrument <- instruments) {

      // Create new instrument
      val udsInstrument = new UdsInstrument()
      udsInstrument.setName(instrument.getString(instrumentCols.NAME))
      udsInstrument.setSource(instrument.getString(instrumentCols.SOURCE))
      udsEM.persist(udsInstrument)

      // Store instrument configurations
      val instConfigs = instrument.getConfigList("configurations").asInstanceOf[java.util.List[Config]]

      for (instConfig <- instConfigs) {

        val udsActivation = udsActivationByType(instConfig.getString(instConfigCols.ACTIVATION_TYPE))

        val udsInstrumentConfig = new UdsInstrumentConfig()
        udsInstrumentConfig.setName(instConfig.getString(instConfigCols.NAME))
        udsInstrumentConfig.setMs1Analyzer(instConfig.getString(instConfigCols.MS1_ANALYZER))
        udsInstrumentConfig.setMsnAnalyzer(instConfig.getString(instConfigCols.MSN_ANALYZER))
        udsInstrumentConfig.setSerializedProperties("""{"is_hidden":false}""")
        udsInstrumentConfig.setActivation(udsActivation)
        udsInstrumentConfig.setInstrument(udsInstrument)

        udsEM.persist(udsInstrumentConfig)
      }
    }

  }

  private def _importPeaklistSoftware(udsEM: EntityManager, peaklistSoftware: java.util.List[Config]) {

    val peaklistSoftCols = UdsDbPeaklistSoftwareTable.columns
    val parsingRuleCols = UdsDbSpecTitleParsingRuleTable.columns
    val parsingRuleColsList = UdsDbSpecTitleParsingRuleTable.columnsAsStrList.filter { _ != "id" }

    for (peaklistSoft <- peaklistSoftware) {

      // Create a peaklist software
      val udsPeaklistSoft = new UdsPeaklistSoft()
      var parsingRuleName = peaklistSoft.getString(peaklistSoftCols.NAME)
      udsPeaklistSoft.setName(parsingRuleName)

      if (peaklistSoft.hasPath(peaklistSoftCols.VERSION)) {
        val version = peaklistSoft.getString(peaklistSoftCols.VERSION)
        udsPeaklistSoft.setVersion(version)
        parsingRuleName += " " + version
      }

      // Store spectrum title parsing rule
      val parsingRule = peaklistSoft.getConfig("parsing_rule")

      val valueByName = parsingRuleColsList.map { c =>
        if (parsingRule.hasPath(c)) c -> parsingRule.getString(c)
        else c -> null
      } toMap

      val udsParsingRule = new UdsSpecTitleParsingRule()
      udsParsingRule.setName(parsingRuleName)
      udsParsingRule.setRawFileName(valueByName(parsingRuleCols.RAW_FILE_NAME))
      udsParsingRule.setFirstCycle(valueByName(parsingRuleCols.FIRST_CYCLE))
      udsParsingRule.setLastCycle(valueByName(parsingRuleCols.LAST_CYCLE))
      udsParsingRule.setFirstScan(valueByName(parsingRuleCols.FIRST_SCAN))
      udsParsingRule.setLastScan(valueByName(parsingRuleCols.LAST_SCAN))
      udsParsingRule.setFirstTime(valueByName(parsingRuleCols.FIRST_TIME))
      udsParsingRule.setLastTime(valueByName(parsingRuleCols.LAST_TIME))

      udsEM.persist(udsParsingRule)

      // Store peaklist software
      udsPeaklistSoft.setSpecTitleParsingRule(udsParsingRule)
      udsEM.persist(udsPeaklistSoft)

    }

  }

  private def _importQuantMethods(udsEM: EntityManager, quantMethods: java.util.List[Config]) {

    val quantMethodCols = UdsDbQuantMethodTable.columns
    val quantLabelCols = UdsDbQuantLabelTable.columns

    // Store quantitative methods
    for (quantMethod <- quantMethods) {

      // Create new quantitative method
      val udsQuantMethod = new UdsQuantMethod()
      udsQuantMethod.setName(quantMethod.getString(quantMethodCols.NAME))
      udsQuantMethod.setType(quantMethod.getString(quantMethodCols.TYPE))
      udsQuantMethod.setAbundanceUnit(quantMethod.getString(quantMethodCols.ABUNDANCE_UNIT))

      udsEM.persist(udsQuantMethod)

      // Store quant labels if they are defined
      if (quantMethod.hasPath("quant_labels")) {
        val quantLabels = quantMethod.getConfigList("quant_labels").asInstanceOf[java.util.List[Config]]

        for (quantLabel <- quantLabels) {

          val udsQuantLabel = new UdsQuantLabel()
          udsQuantLabel.setName(quantLabel.getString(quantLabelCols.NAME))
          udsQuantLabel.setType(quantMethod.getString(quantMethodCols.TYPE))
          udsQuantLabel.setSerializedProperties(quantLabel.getString(quantLabelCols.SERIALIZED_PROPERTIES))
          udsQuantLabel.setMethod(udsQuantMethod)

          udsEM.persist(udsQuantLabel)
        }
      }
    }

  }

}
