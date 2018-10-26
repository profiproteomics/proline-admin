package fr.proline.admin.gui.process.config.postgres

import fr.profi.util.primitives._
import fr.profi.util.scala.ByteUtils._
import fr.proline.admin.gui.process.config._

import PgParamType._
import PostgresOptimizableParamEnum._


/**
 * ********************* *
 * Default configuration *
 * ********************* *
 */
trait IPostgresConfig {

  /** Get string from bytes **/

  /* To be implemented */
  def valueRangeByParam: Map[PostgresOptimizableParamEnum.Value, ConfigParamValueRange]
  
   /* Values formatters: from BigDecimal to string in config */
  //TODO ? BigDecimal rather than Double
  // currently using Double since it's the type returned by slider.value()
  
  //private val floatToStringFormatter = { v: AnyVal => "%.1f".format(v) }
  private val floatToStringFormatter = { d: Double => "%.1f".formatLocal(java.util.Locale.US, d) }
  //private val longToStringFormatter = { v: Double => v.toString }
  //private val longToIntegerStringFormatter = { v: BigDecimal => v.round(MathContext.DECIMAL64).toString() } //TODO: scala 2.11 offers bd.rounded()
  private val longToStringFormatter = { d: Double => toLong(d).toString() }
  
  
  private val bytesAmountFormatter = { d: Double => 
    formatBytesAmount(toLong(d), numberFormat = "%.0f%s")
    math.round(d / 1024) + "kB"
  }
  
  //private val minutesToSecondsFormatter = { v: BigDecimal => (toLong(v) * 60).toString }
  private val minutesFormatter = { d: Double => toLong(d) + "min" }

  /* Values readers: from string in config to BigDecimal */
  private val stringToBigDecimalParser = { s: String => BigDecimal(s) }

  //TODO: move relevant part to Utils
  private val bytesAmountParser = { s: String =>
    val (value, unit) = parseBytesAmount(s, defaultUnit = ByteUnit.KB)
    BigDecimal(getBytesAmount(value.doubleValue(), unit))
  }

  private val minutesParser = { s: String =>
    val pattern = """^(\d+)\s?(min)?""".r
    val matchOpt = pattern findFirstMatchIn(s)
    if (matchOpt.isEmpty) throw new Exception("""Value doesn't conform to format: '\d+min'""")
    
    BigDecimal(matchOpt.get.group(1))
  }

  /* Build map */
  val formatByParam: Map[PostgresOptimizableParamEnum.Value, PostgreSqlLineFormat] = Map(

    MAX_CONNECTIONS -> PostgreSqlLineFormat(
      formatter = longToStringFormatter,
      parser = stringToBigDecimalParser,
      paramType = INTEGER,
      unitString = "connections",
      toolTipText = """Number of concurrent SQL sessions:

Each Proline Server task can use 1 to 5 SQL sessions,
each Proline-Studio instance can use some SQL sessions.

Note: Increasing max_connections costs ~400 bytes of shared memory perconnection slot,
plus lock space (see max_locks_per_transaction).

Change requires restart.


See: https://wiki.postgresql.org/wiki/Tuning_Your_PostgreSQL_Server
See: http://fr.slideshare.net/xzilla/the-essentialpostgresconf
See: https://bioproj.extra.cea.fr/docs/proline/doku.php?id=setupguide:postgresqloptimization"""
    ),

    TCP_KEEPALIVES_IDLE -> PostgreSqlLineFormat(
      formatter = minutesFormatter,
      parser = minutesParser,
      paramType = TIME,
      toolTipText = """Number of seconds before sending a keepalive packet on an otherwise idle TCP connection:

Help with broken router / firewall and checking for dead peers.

Note: in file, 0 (without unit) selects the system default, which is 2 hours.


See: https://wiki.postgresql.org/wiki/Tuning_Your_PostgreSQL_Server
See: http://fr.slideshare.net/xzilla/the-essentialpostgresconf
See: https://bioproj.extra.cea.fr/docs/proline/doku.php?id=setupguide:postgresqloptimization"""
    ),

    SHARED_BUFFERS -> PostgreSqlLineFormat(
      formatter = bytesAmountFormatter,
      parser = bytesAmountParser,
      paramType = BYTES,
      toolTipText = """Amount of memory dedicated to PostgreSQL for caching data:

It is advised to use about 1/4 of physical memory dedicated to the PostgreSQL instance.

Change requires restart.


See: https://wiki.postgresql.org/wiki/Tuning_Your_PostgreSQL_Server
See: http://fr.slideshare.net/xzilla/the-essentialpostgresconf
See: https://bioproj.extra.cea.fr/docs/proline/doku.php?id=setupguide:postgresqloptimization"""
    ),

//    CHECKPOINT_SEGMENTS -> PostgreSqlLineFormat(
//      formatter = longToStringFormatter,
//      parser = stringToBigDecimalParser,
//      unitString = "segments",
//      paramType = INTEGER,
//      toolTipText = """Every time checkpoint_segments worth of these files have been written, a checkpoint occurs:
//
//As you generate transactions, Postgres puts data into the write-ahead log (WAL).
//The WAL is organized into segments that are typically 16MB each.
//Periodically, after the system finishes a checkpoint, the WAL data up to a certain point
//is guaranteed to have been applied to the database. At that point the old WAL files aren't
//needed anymore and can be reused. Checkpoints are generally caused by one of two things happening:
//* checkpoint_segments worth of WAL files have been written
//* more than checkpoint_timeout seconds have passed since the last checkpoint
//
//Default is 3;
//shared_buffers / 16 can be used is optimized;
//use at max. 64 or 256 for write-heavy bulk loading.
//
//
//See: https://wiki.postgresql.org/wiki/Tuning_Your_PostgreSQL_Server
//See: http://fr.slideshare.net/xzilla/the-essentialpostgresconf
//See: https://bioproj.extra.cea.fr/docs/proline/doku.php?id=setupguide:postgresqloptimization"""
//    ),

    CHECKPOINT_COMPLETION_TARGET -> PostgreSqlLineFormat(
      formatter = floatToStringFormatter,
      parser = stringToBigDecimalParser,
      paramType = DECIMAL,
      toolTipText = """Suggests how far along the system should aim to have finished the current checkpoint
relative to when the next one is expected.

The interval is 0.0 to 1.0 (percentage).
Use 0.9 for high value of checkpoint_segments.


See: https://wiki.postgresql.org/wiki/Tuning_Your_PostgreSQL_Server
See: http://fr.slideshare.net/xzilla/the-essentialpostgresconf
See: https://bioproj.extra.cea.fr/docs/proline/doku.php?id=setupguide:postgresqloptimization"""
    ),

    TEMP_BUFFERS -> PostgreSqlLineFormat(
      formatter = bytesAmountFormatter,
      parser = bytesAmountParser,
      paramType = BYTES,
      toolTipText = """Amount of bytes used per session for temporary tables creation.

See: https://bioproj.extra.cea.fr/docs/proline/doku.php?id=setupguide:postgresqloptimization"""
    ),

    WORK_MEM -> PostgreSqlLineFormat(
      formatter = bytesAmountFormatter,
      parser = bytesAmountParser,
      paramType = BYTES,
      toolTipText = """Amount of bytes used per sorting operation for hashing, sorting and IN operator when processing queries.


See: https://wiki.postgresql.org/wiki/Tuning_Your_PostgreSQL_Server
See: http://fr.slideshare.net/xzilla/the-essentialpostgresconf
See: https://bioproj.extra.cea.fr/docs/proline/doku.php?id=setupguide:postgresqloptimization"""
    ),

    MAINTENANCE_WORK_MEM -> PostgreSqlLineFormat(
      formatter = bytesAmountFormatter,
      parser = bytesAmountParser,
      paramType = BYTES,
      toolTipText = """Amount of bytes used for initial index creation and VACUUM operations.


See: https://wiki.postgresql.org/wiki/Tuning_Your_PostgreSQL_Server
See: http://fr.slideshare.net/xzilla/the-essentialpostgresconf
See: https://bioproj.extra.cea.fr/docs/proline/doku.php?id=setupguide:postgresqloptimization
"""
    ),

    EFFECTIVE_CACHE_SIZE -> PostgreSqlLineFormat(
      formatter = bytesAmountFormatter,
      parser = bytesAmountParser,
      paramType = BYTES,
      toolTipText = """Assumption about the effective size of the disk cache to optimize index use:
monitor physical memory allocated by system to disk cache operations.


See: https://wiki.postgresql.org/wiki/Tuning_Your_PostgreSQL_Server
See: http://fr.slideshare.net/xzilla/the-essentialpostgresconf
See: https://bioproj.extra.cea.fr/docs/proline/doku.php?id=setupguide:postgresqloptimization"""
    )
  )
}

/** Config for PostgreSQL v9.4 **/
// Online resources :
// - https://bioproj.extra.cea.fr/docs/proline/doku.php?id=setupguide:postgresqloptimization
// - http://fr.slideshare.net/xzilla/the-essentialpostgresconf
// - https://wiki.postgresql.org/wiki/Tuning_Your_PostgreSQL_Server
// - http://www.postgresql.org/docs/9.4/static/runtime-config-resource.html
// - http://www.postgresql.org/docs/9.4/static/runtime-config-wal.html
object PostgresConfigV9_4 extends IPostgresConfig {

  import PostgresConfigOptimizer._

  /* Access Physical memory size */

  /* Defaults map */
  // Following recommended memory sizes are given for a server with 16 GiB of physical memory 
  // and about 8 GiB dedicated for the PostgreSQL instance.

  val valueRangeByParam: Map[PostgresOptimizableParamEnum.Value, ConfigParamValueRange] = Map(

    //minValue, defaultValue, suggestedValue, maxValue

    /* Max connections */
    MAX_CONNECTIONS -> ConfigParamValueRange(10, 100, 400, 10000),
    // Number of concurrent SQL sessions
    // Each Proline Server task can use 1 to 5 SQL sessions, 
    // each Proline-Studio instance can use some SQL sessions.
    // Default value 100

    // max_connections = 100
    // Change requires restart
    // Note: Increasing max_connections costs ~400 bytes of shared memory per
    // connection slot, plus lock space (see max_locks_per_transaction).

    /* TCP keepalive idle
       Notes:
       - 0 means system default (2 hours)
       - units changed from seconds to minutes for GUI
    */
    TCP_KEEPALIVES_IDLE -> ConfigParamValueRange(0, 2*60 , 5, 4 * 60),
    // Number of seconds before sending a keepalive packet on an otherwise idle TCP connection.
    // Help with broken router / firewall and checking for dead peers.
    // Default value 0 (2 hours) → 300 (5 minutes)

    // tcp_keepalives_idle = 0
    // TCP_KEEPIDLE, in seconds;
    // 0 selects the system default.

    /* Shared buffers (bytes amount) */
    SHARED_BUFFERS -> new ConfigParamValueRange(128 kB, 128 MB, sharedBuffersOptimizedValue, sharedBuffersMaxValue),
    // Use about 1/4 of physical memory dedicated to the PostgreSQL instance.
    // Default value 32MB → 2048MB

    // shared_buffers = 128MB
    // min 128kB
    // Change requires restart.
    //TODO: quarterOfPhysicalMem / totalPhysicalMem, to String, use it

    /* Checkpoint segments */
    //CHECKPOINT_SEGMENTS -> ConfigParamValueRange(1, 3, checkpointSegmentsOptimizedValue, checkpointSegmentsMaxValue),
    // Use (shared_buffers / 16) ; max. 64 or 256 for write-heavy bulk loading.
    // Default value 3  → 128

    // checkpoint_segments = 3
    // in logfile segments, min 1, 16MB each

    /* Checkpoint completion target (percentage) */
    CHECKPOINT_COMPLETION_TARGET -> new ConfigParamValueRange(0, 0.5, 0.9, 1),
    //  0.9 for high value of checkpoint_segments.
    // Default value 0.5 → 0.9

    // checkpoint_completion_target = 0.5
    // checkpoint target duration, 0.0 - 1.0

    /* Temporary buffers - PER SESSION (bytes amount) */
    TEMP_BUFFERS -> new ConfigParamValueRange(800 kB, 8 MB, tempBuffersOptimizedValue, tempBuffersMaxValue),
    // Per session Used for temporary tables creation.
    // Default value 8MB → 512MB 

    // temp_buffers = 8MB
    // min 800kB

    /* Working memory - PER SORTING OPERATION (bytes amount) */
    WORK_MEM -> new ConfigParamValueRange(64 kB, 4 MB, 16 MB, 128 MB),
    // Used for hashing, sorting and IN operator when processing queries.
    // Default value 1MB → 4MB to 64MB

    // work_mem = 4MB
    // min 64kB

    /* Maintenance working memory (bytes amount) */
    MAINTENANCE_WORK_MEM -> new ConfigParamValueRange(1 MB, 64 MB, maintenanceWorkMemOptimizedValue, maintenanceWorkMemMaxValue),
    // Used for initial index creation and VACUUM operations.
    // Default value 16MB → 1024MB

    // maintenance_work_mem = 64MB
    // min 1MB

    /* Effective cache size (bytes amount) */
    EFFECTIVE_CACHE_SIZE -> new ConfigParamValueRange(128 MB, 4 GB, effectiveCacheSizeOptimizedValue, effectiveCacheSizeMaxValue)
    // Assumption about the effective size of the disk cache to optimize index use:
    // monitor physical memory allocated by system to disk cache operations.
    // Default value 128MB → 4096MB
  
    // effective_cache_size = 4GB
  )

}


/**
 * Some additional stuff per param
 */
case class PostgreSqlLineFormat(
  formatter: Double => String, //BigDecimal => String,
  parser: String => BigDecimal,
  paramType: PgParamType.Value,
  unitString: String = "",
  toolTipText: String = "" //TODO: move to dedicated place
)