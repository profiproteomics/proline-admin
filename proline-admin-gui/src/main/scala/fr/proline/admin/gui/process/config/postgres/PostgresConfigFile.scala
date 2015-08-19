package fr.proline.admin.gui.process.config.postgres

import com.typesafe.scalalogging.slf4j.Logging

import fr.proline.admin.gui.process.config._

/**
 * ***************************************************** *
 * PostgreSQL implementation : postgres.conf, pg_hba.conf *
 * **************************************************** *
 */
class PostgresConfigFile(val filePath: String) extends KVConfigFileIndexing with Logging {

  /* Define the keys we look for */
  val paramKeys: Seq[String] = PostgresOptimizableParamEnum.getParamConfigKeys()
  //println("Look for " + paramKeys.length + " keys")

  /* Define parsing patterns */
  //WARNING:
  //(The "=" is optional.)  Whitespace may be used.  Comments are introduced with "#" anywhere on a line.

  val keyValuePattern = """^(#?)(\w+)\s*=?\s*([\w\.]+)\s*(#.+)*$""".r

  /** Model line with ConfigFileKVLine **/
  protected def parseLine(line: String, lineIdx: Int): Option[ConfigFileKVLine] = {
    
    keyValuePattern.findFirstMatchIn(line).map { kvMatch =>
      
      /* Filter: keep line only if it contains a desired key */
      val key = kvMatch.group(2)
      if (paramKeys.contains(key) == false ) return None
      
      /* Return KV line */
      ConfigFileKVLine(
        line = line,
        index = lineIdx,
        key = key,
        valueString = kvMatch.group(3),
        valueStartIdx = kvMatch.start(3),
        valueEndIdx = kvMatch.end(3),
        commented= kvMatch.group(1) == "#"
      )
    }
  }
}