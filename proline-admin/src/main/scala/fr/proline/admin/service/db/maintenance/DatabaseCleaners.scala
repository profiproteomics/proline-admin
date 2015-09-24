package fr.proline.admin.service.db.maintenance

import java.io.FileOutputStream

import org.dbunit.DataSourceDatabaseTester
import org.dbunit.database.DatabaseSequenceFilter
import org.dbunit.dataset.FilteredDataSet
import org.dbunit.dataset.xml.FlatXmlDataSet


import fr.proline.admin.service.ICommandWork
import fr.proline.repository.IDatabaseConnector

trait IDatabaseCleaner {
  def compact( dbConnector: IDatabaseConnector )
  def defrag( dbConnector: IDatabaseConnector )
  def reindex( dbConnector: IDatabaseConnector )
}

class H2DatabaseCleaner extends IDatabaseCleaner {
  
  def compact( dbConnector: IDatabaseConnector ) {
    // TODO: use SHUTDOWN COMPACT statement
    // Source: http://www.h2database.com/html/grammar.html#shutdown
    ()
  }
  
  def defrag( dbConnector: IDatabaseConnector ) {
    // TODO: use SHUTDOWN DEFRAG statement
    // Source: http://www.h2database.com/html/grammar.html#shutdown
    ()
  }
  
  def reindex( dbConnector: IDatabaseConnector ) {
    // DO NOTHING => NOT YET IMPLEMENTED (http://www.h2database.com/html/roadmap.html)
    ()
  }
}

class PgDatabaseCleaner extends IDatabaseCleaner {
  
  // Source:
  // http://wiki.postgresql.org/wiki/VACUUM_FULL
  // http://dba.stackexchange.com/questions/46780/postgresql-difference-between-vacuum-full-and-cluster
  
  def compact( dbConnector: IDatabaseConnector ) {
    // TODO: use VACUUM FULL statement
    // Source: http://www.postgresql.org/docs/9.1/static/sql-vacuum.html
  }
  
  def defrag( dbConnector: IDatabaseConnector ) {
    // TODO: use CLUSTER statement
    // Source: http://www.postgresql.org/docs/9.1/static/sql-cluster.html
    ()
  }
  
  def reindex( dbConnector: IDatabaseConnector ) {
    // TODO: use the REINDEX statement
    // Source : http://www.postgresql.org/docs/9.1/static/sql-reindex.html
    ()
  }
}

class SQLiteDatabaseCleaner extends IDatabaseCleaner {
  
  def compact( dbConnector: IDatabaseConnector ) {
    // TODO: use VACUUM statement
    // Source: http://www.sqlite.org/lang_vacuum.html
    ()
  }
  
  def defrag( dbConnector: IDatabaseConnector ) {
    // TODO: call the compact method
    ()
  }
  
  def reindex( dbConnector: IDatabaseConnector ) {
    // TODO: use the REINDEX statement
    // Source: http://www.sqlite.org/lang_reindex.html
    ()
  }
}
