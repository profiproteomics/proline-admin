package fr.proline.admin.service.db.setup

import com.weiglewilczek.slf4s.Logging

import fr.profi.jdbc.easy._
import fr.profi.util.serialization.ProfiJson

import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.DoJDBCWork
import fr.proline.core.dal.tables.lcms.{ LcmsDbFeatureScoringTable, LcmsDbPeakelFittingModelTable, LcmsDbPeakPickingSoftwareTable }
import fr.proline.core.om.model.lcms.FeatureScoring
import fr.proline.core.om.model.lcms.PeakPickingSoftware
import fr.proline.core.om.model.lcms.PeakelFittingModel
import fr.proline.repository.IDatabaseConnector

/**
 * @author David Bouyssie
 *
 */
class SetupLcmsDB( val dbConnector: IDatabaseConnector,
                   val dbContext: DatabaseConnectionContext,
                   val dbConfig: DatabaseSetupConfig
                 ) extends ISetupDB with Logging {
  
  //lazy val lcmsEM = dbContext.getEntityManager()
  
  protected def importDefaults() {
    
    this.logger.info("no default values at the moment")
    
    // Begin transaction
    dbContext.beginTransaction()
    
    DoJDBCWork.withEzDBC(dbContext, { ezDBC =>
      
      val lcmsFtScoring = new FeatureScoring(
        id = 1,
        name = "no scoring",
        description = ""
      )
      
      val fitting = new PeakelFittingModel(
        id = 1,
        name ="no fitting"
      )
      
      val pps = new PeakPickingSoftware(
        id = 1,
        name = "unknown peak picker",
        version = "unknown",
        algorithm = "unknown"
      )
      
      ezDBC.executePrepared(LcmsDbFeatureScoringTable.mkInsertQuery,false) { statement =>
        statement.executeWith(
          lcmsFtScoring.id,
          lcmsFtScoring.name,
          lcmsFtScoring.description,
          lcmsFtScoring.properties.map( ProfiJson.serialize(_) )
        )
      }
      
      ezDBC.executePrepared(LcmsDbPeakelFittingModelTable.mkInsertQuery,false) { statement =>
        statement.executeWith(
          fitting.id,
          fitting.name,
          fitting.properties.map( ProfiJson.serialize(_) )
        )
      }
      
      ezDBC.executePrepared(LcmsDbPeakPickingSoftwareTable.mkInsertQuery,false) { statement =>
        statement.executeWith(
          pps.id,
          pps.name,
          pps.version,
          pps.algorithm,
          pps.properties.map( ProfiJson.serialize(_) )
        )
      }
      
    })
    
    dbContext.commitTransaction()
    
  }
  
}