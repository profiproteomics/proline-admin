package fr.proline.admin.service.db.setup

import com.typesafe.scalalogging.slf4j.Logging
import fr.profi.jdbc.easy._
import fr.profi.util.serialization.ProfiJson
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.DoJDBCWork
import fr.proline.core.dal.tables.lcms.{ LcmsDbFeatureScoringTable, LcmsDbPeakelFittingModelTable, LcmsDbPeakPickingSoftwareTable }
import fr.proline.core.om.model.lcms.FeatureScoring
import fr.proline.core.om.model.lcms.PeakPickingSoftware
import fr.proline.core.om.model.lcms.PeakelFittingModel
import fr.proline.repository.IDatabaseConnector
import fr.proline.core.dal.ContextFactory

/**
 * @author David Bouyssie
 *
 */

/* Cannot use DatabaseConnectionContext here because Db Shema does not exist yet ! */
class SetupLcmsDB(val dbConnector: IDatabaseConnector,
                  val dbConfig: DatabaseSetupConfig) extends ISetupDB with Logging {

  protected def importDefaults() {
    logger.info("no default values at the moment")

    var lcMsTransacOK: Boolean = false

    val lcMsSqlContext = ContextFactory.buildDbConnectionContext(dbConnector, false)

    try {
      lcMsSqlContext.beginTransaction()
      lcMsTransacOK = false

      DoJDBCWork.withEzDBC(lcMsSqlContext, { ezDBC =>

        val lcmsFtScoring = new FeatureScoring(
          id = 1,
          name = "no scoring",
          description = ""
        )

        val fitting = new PeakelFittingModel(
          id = 1,
          name = "no fitting"
        )

        val pps = new PeakPickingSoftware(
          id = 1,
          name = "unknown peak picker",
          version = "unknown",
          algorithm = "unknown"
        )

        ezDBC.executePrepared(LcmsDbFeatureScoringTable.mkInsertQuery, false) { statement =>
          statement.executeWith(
            lcmsFtScoring.id,
            lcmsFtScoring.name,
            lcmsFtScoring.description,
            lcmsFtScoring.properties.map(ProfiJson.serialize(_))
          )
        }

        ezDBC.executePrepared(LcmsDbPeakelFittingModelTable.mkInsertQuery, false) { statement =>
          statement.executeWith(
            fitting.id,
            fitting.name,
            fitting.properties.map(ProfiJson.serialize(_))
          )
        }

        ezDBC.executePrepared(LcmsDbPeakPickingSoftwareTable.mkInsertQuery, false) { statement =>
          statement.executeWith(
            pps.id,
            pps.name,
            pps.version,
            pps.algorithm,
            pps.properties.map(ProfiJson.serialize(_))
          )
        }

      })

      lcMsSqlContext.commitTransaction()
      lcMsTransacOK = true
    } finally {

      if (!lcMsTransacOK) {
        logger.info("Rollbacking LC-MS Db EntityTransaction")

        try {
          lcMsSqlContext.rollbackTransaction()
        } catch {
          case ex: Exception => logger.error("Error rollbacking LC-MS Db EntityTransaction")
        }

      }

      if (lcMsSqlContext != null) {
        lcMsSqlContext.close()
      }

    }

  }

}
