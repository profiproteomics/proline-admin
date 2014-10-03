package fr.proline.admin.gui.process

import java.io.File

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.slf4j.Logging

import fr.proline.admin.gui.Main
import fr.proline.admin.gui.component.dialog.GetConfirmation
import fr.proline.admin.gui.component.panel.ButtonsPanel
import fr.proline.admin.service.db.SetupProline

import scalafx.application.Platform
import scalafx.scene.Cursor
import scalafx.scene.Cursor.sfxCursor2jfx

/**
 * All utilities to modify ProlineAdmin configuration
 */
object ProlineAdminConnection extends Logging {

  /**
   * Udapte Proline Admin config (processing + UI management)
   */
  def loadProlineConf() = {
    //BLOCKING! otherwise update config before (conf file changes || user's choice) (are||is) effective
    //FIXME: freezing

    val actionString = "<b>>> Loading Proline configuration...</b>"

    synchronized {
      Main.stage.scene().setCursor(Cursor.WAIT)
      println()
      println(actionString)

      try {
        this._setNewProlineConfig()

        logger.info(s"Action '$actionString' finished with success.")
        println(s"""[ $actionString : <b>success</b> ]""")

      } catch {

        case fxt: IllegalStateException => logger.warn(fxt.getLocalizedMessage()) //useful?

        case e: Throwable => {
          synchronized {
            logger.warn("Can't update Proline configuration :", e)
            println("ERROR - Can't update Proline configuration : " + e.getMessage())
            println(s"[ $actionString : finished with <b>error</b> ]")
          }
          //throw e // if re-thrown, system stops
        }

      } finally {
        ButtonsPanel.computeButtonsAvailability()
        Main.stage.scene().setCursor(Cursor.DEFAULT)
      }
    }
  }

  /**
   *  Update SetupProline config when CONF file changes
   */
  private def _setNewProlineConfig() {

    /** Reload CONF file */
    val newConfigFile = ConfigFactory.parseFile(new File(Main.confPath))

    /** Update displayed window, and Proline configuration if data directory already exists */
    val dataDir = newConfigFile.getConfig("proline-config").getString("data-directory")

    if (new File(dataDir).exists()) {

      synchronized {
        SetupProline.setConfigParams(newConfigFile)
        UdsRepository.setUdsDbConfig(SetupProline.getUpdatedConfig.udsDBConfig)

        Platform.runLater(Main.stage.title = s"Proline Admin @ $dataDir")
      }

    } else {
      /** Allow to create data folder if it doesn't exist */
      synchronized {
        logger.warn(s"""Unknown data directory : $dataDir""")
        println(s"""WARN - Unknown data directory : $dataDir""")
      }

      val isConfirmed =
        if (dataDir == """<path/to/proline/data>""" && Main.firstCallToDataDir) {
          Main.firstCallToDataDir = false //don't ask to create default dataDir on application start 
          false

        } else {
          GetConfirmation(
            text = "The databases directory you specified does not exist. Do you want to create it?\n(This involves a new installation of Proline.)",
            title = s"Unknown directory : $dataDir",
            yesText = "Yes",
            cancelText = "No"
          )
        }

      if (isConfirmed == true) {
        logger.info(s"Creating data directory : $dataDir")
        println(s"Creating databases directory : $dataDir ...")

        val successfullyCreated = new File(dataDir).mkdir()
        if (successfullyCreated == true) {
          logger.info("Data directory successfully created.")
          println("INFO - Databases directory successfully created.")

          SetupProline.setConfigParams(newConfigFile)
          UdsRepository.setUdsDbConfig(SetupProline.getUpdatedConfig.udsDBConfig)

          Platform.runLater(Main.stage.title = s"Proline Admin @ $dataDir")

        } else {
          /** If dataDir can't be created */
          Platform.runLater(Main.stage.title = s"Proline Admin (invalid configuration)")
          throw new Exception("Unknown data directory (problem in creation) " + dataDir)
        }

      } else {
        /** If user aborts dataDir creation */
        Platform.runLater(Main.stage.title = s"Proline Admin (invalid configuration)")
        throw new Exception("Unknown data directory (not created, user's choice) " + dataDir)
      }

      ///////////////////////// TRY ////////////////////////
      // FROM http://docs.scala-lang.org/overviews/core/futures.html
      //
      //      import scala.concurrent._
      //      import scala.concurrent.ExecutionContext.Implicits.global
      //
      //      val p = promise[Boolean]
      //      val f = p.future
      //
      //      val producer =
      //        future 
      //        {
      //          println("in promise")
      //          val isConfirmed = Platform.runLater(
      //            GetConfirmation(
      //              text = "The databases directory you specified does not exist. Do you want to create it?\n(This involves a new installation of Proline.)",
      //              title = s"Unknown directory : $dataDir"
      //            ))
      //          p success isConfirmed
      //          continueDoingSomethingUnrelated()
      //        }
      //
      //      val consumer = future {
      //
      //      logger.warn(s"Unknown data directory : $dataDir")
      //      println(s"WARN - Unknown data directory : $dataDir")
      //
      //      f onFailure { case e => logger.error(e.getMessage()); println("ERROR - " + e.getMessage()) }
      //      f onSuccess {
      //        case isConfirmed => {
      //
      //          println("WARN - isConfirmed : " + isConfirmed)
      //
      //          if (isConfirmed == true) {
      //            logger.info(s"Creating data directory : $dataDir")
      //            println(s"Creating databases directory : $dataDir ...")
      //
      //            val successfullyCreated = new File(dataDir).mkdir()
      //
      //            /** If it's created */
      //            if (successfullyCreated == true) {
      //              logger.info("Data directory successfully created.")
      //              println("Databases directory successfully created.")
      //
      //              SetupProline.setConfigParams(newConfigFile)
      //              UdsRepository.setUdsDbConfig(SetupProline.getUpdatedConfig.udsDBConfig)
      //
      //              Platform.runLater(Main.stage.title = s"Proline Admin @ $dataDir")
      //
      //              /** If it can't be created */
      //            } else {
      //              Platform.runLater(Main.stage.title = s"Proline Admin (invalid configuration)")
      //              throw new Exception("Unknown data directory (problem in creation) " + dataDir)
      //            }
      //
      //            /** If user doesn't want to create it */
      //          } else {
      //            Platform.runLater(Main.stage.title = s"Proline Admin (invalid configuration)")
      //            throw new Exception("Unknown data directory (not created, user's choice) " + dataDir)
      //          }
      //
      //        }
      //        }
      //      }
      ///////////////// END ////////////////////

    }
  }
}