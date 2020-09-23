package fr.proline.admin.gui.process.config

import java.io.{File, FileWriter}

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import fr.profi.util.scala.TypesafeConfigWrapper._

/**
 * Jms-node configuration file
 * @author aromdhani
 *
 */

class NodeConfigFile(val path: String) extends LazyLogging {
  require(path != null && path.isEmpty() == false, "jms-node configuration file must not be null nor empty")
  private val jmsNodeFile = new File(path)
  def getTypeSafeConfig(): Config = ConfigFactory.parseFile(jmsNodeFile)

  /** Read node configurations */
  def read: Option[NodeConfig] = {
    try {
      val nodeConfig = getTypeSafeConfig()
      Some(
        NodeConfig(
          jmsServerHost = nodeConfig.getStringOpt("node_config.jms_server_host"),
          jmsServePort = nodeConfig.getIntOpt("node_config.jms_server_port"),
          requestQueueName = nodeConfig.getStringOpt("node_config.proline_service_request_queue_name"),
          serviceThreadPoolSize = nodeConfig.getIntOpt("node_config.service_thread_pool_size"),
          //xicPoolSize = nodeConfig.getIntOpt("node_config.xic_files_pool_size"),
          enableImport = nodeConfig.getBooleanOpt("node_config.enable_imports")))

    } catch {
      case t: Throwable => {
        logger.error("Error occured while reading jms-node configuration file", t.getMessage())
        None
      }
    }
  }

  /** Write node configurations */
  def write(nodeConfig: NodeConfig): Unit = synchronized {
    val nodeConfigTemplate = s""" 
 node_config {
  jms_server_host ="${nodeConfig.jmsServerHost.getOrElse("localhost")}"
  jms_server_port=${nodeConfig.jmsServePort.getOrElse(5445)}
  proline_service_request_queue_name="${nodeConfig.requestQueueName.getOrElse("ProlineServiceRequestQueue")}"
  service_thread_pool_size=${nodeConfig.serviceThreadPoolSize.getOrElse(-1)}
  enable_imports=${nodeConfig.enableImport.getOrElse(true)}
 }
    """
    synchronized {
      val out = new FileWriter(jmsNodeFile)
      try { out.write(nodeConfigTemplate) }
      finally { out.close }
    }
  }
}

/**
 * Node configuration model
 *
 */
case class NodeConfig(
  var jmsServerHost: Option[String] = None,
  var jmsServePort: Option[Int] = None,
  var requestQueueName: Option[String] = None,
  var serviceThreadPoolSize: Option[Int] = None,
  //var xicPoolSize: Option[Int] = None,
  var enableImport: Option[Boolean] = Option(true))