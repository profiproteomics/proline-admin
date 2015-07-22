package fr.proline.admin.gui.process.config.postgres

import com.typesafe.scalalogging.slf4j.Logging

import fr.proline.admin.gui.process.config._

/**
 * ***************************************************** *
 * PostgreSQL implementation : postgres.conf, pg_hba.conf *
 * **************************************************** *
 */
class PostgresConfigFile(val filePath: String) extends KVConfigFileIndexing with Logging {

  /* Define parsing patterns */
  //WARNING:
  //(The "=" is optional.)  Whitespace may be used.  Comments are introduced with "#" anywhere on a line.

  val keyValuePattern = """^([^#]\w+)\s*=?\s*([\w\.]+)\s*(#.+)*$""".r

  /** Model line with ConfigFileKVLine **/
  protected def parseLine(line: String, lineIdx: Int): Option[ConfigFileKVLine] = {

    keyValuePattern.findFirstMatchIn(line).map { kvMatch =>
      ConfigFileKVLine(line, lineIdx, kvMatch.group(1), kvMatch.group(2), kvMatch.start(2), kvMatch.end(2))
    }

  }
}