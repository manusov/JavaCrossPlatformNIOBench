/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Parent class for all IO scenarios.
Note IO scenario is highest hierarchy level, IO task is lowest level.
IO scenario runs a group of IO tasks.
*/


/*

TODO.
REQUIRED DATA AND ADDRESS RANDOMIZATION SUPPORT, BY THIS CLASS,
FOR ALL SUB-CLASSES OF THIS CLASS.

TODO.
CAN REMOVE FIXED NAMES "MBPS" AT IO TASKS, BECAUSE WITH ADDRESS RANDOMIZATION
CAN MEASURE MBPS AND IOPS BOTH.

*/


package niobenchrefactoring.model;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.LinkedList;

public class IOscenario extends Thread
{
/*
Test phases constants definition
*/
public final static int READ_ID  = 0;
public final static int WRITE_ID = 1;
public final static int COPY_ID  = 2;
public final static int TOTAL_READ_ID  = 3;
public final static int TOTAL_WRITE_ID = 4;
public final static int TOTAL_COPY_ID  = 5;
public final static int ID_COUNT = 6;
// additional phases and states for indication
public final static int STARTING_ID  = 6;
public final static int DELAY_ID  = 7;
public final static int DELETE_ID  = 8;
public final static int COMPLETE_ID  = 9;
public final static int ERROR_ID  = 10;
public final static int HALTED_ID  = -1;
// duplicates ID
public final static int PACK_ID = READ_ID;
public final static int UNPACK_ID = COPY_ID;
public final static int TOTAL_PACK_ID = TOTAL_READ_ID;
public final static int TOTAL_UNPACK_ID = TOTAL_COPY_ID;

/*
Names for phases progress visualization
*/
public final static String READ_DELAY_NAME  = "Read pre-delay";
public final static String WRITE_DELAY_NAME = "Write pre-delay";
public final static String COPY_DELAY_NAME  = "Copy pre-delay";
public final static String DELETE_NAME      = "Delete temporary files";
/*
Options constants definitions, read-write modes    
*/
public final static int READ_WRITE = 0;
public final static int READ_ONLY  = 1;
public final static int WRITE_ONLY = 2;
/*
Address randonization modes
*/
public final static int ADDRESS_SEQUENTAL   = 0;
public final static int ADDRESS_BATTERFLY   = 1;
public final static int ADDRESS_RANDOM_SOFT = 2;
public final static int ADDRESS_RANDOM_HARD = 3;
/*
Data randonization modes
*/
public final static int DATA_ZERO        = 0;
public final static int DATA_ONE         = 1;
public final static int DATA_SEQUENTAL   = 2;
public final static int DATA_RANDOM_SOFT = 3;
public final static int DATA_RANDOM_HARD = 4;

/*
Default settings for options variables, part 1 = arguments passed to IOtask
*/
private final static String  DEFAULT_PATH_SRC     = "C:\\TEMP\\";
private final static String  DEFAULT_PREFIX_SRC   = "src";
private final static String  DEFAULT_POSTFIX_SRC  = ".bin";
private final static String  DEFAULT_PATH_DST     = "C:\\TEMP\\";
private final static String  DEFAULT_PREFIX_DST   = "dst";
private final static String  DEFAULT_POSTFIX_DST  = ".bin";
private final static int     DEFAULT_FILE_COUNT   = 20;
private final static int     DEFAULT_FILE_SIZE    = 100*1024*1024;
private final static int     DEFAULT_BLOCK_SIZE   = 10*1024*1024;
private final static int     DEFAULT_THREAD_COUNT = 1;  // 4;
private final static boolean DEFAULT_READ_SYNC    = false;
private final static boolean DEFAULT_WRITE_SYNC   = false;
private final static boolean DEFAULT_COPY_SYNC    = false;
private final static boolean DEFAULT_DATA_SPARSE  = false;
private final static boolean DEFAULT_FAST_COPY    = true;
/*
Default settings for options variables, part 2 = arguments used by IOscenario
*/
private final static int    DEFAULT_READ_WRITE_MODE = READ_WRITE;
private final static int    DEFAULT_ADDRESS_MODE    = ADDRESS_SEQUENTAL;
private final static int    DEFAULT_DATA_MODE       = DATA_ZERO;
private final static int    DEFAULT_READ_DELAY  = 0;
private final static int    DEFAULT_WRITE_DELAY = 0;
private final static int    DEFAULT_COPY_DELAY  = 0;
/*
Options variables, can't be private because used by child class,
part 1 = passed to IOtask
*/
final String pathSrc;
final String prefixSrc;
final String postfixSrc;
final String pathDst;
final String prefixDst;
final String postfixDst;
final int fileCount;
final int fileSize;
final int blockSize;
final int threadCount;
final boolean readSync;
final boolean writeSync;
final boolean copySync;
final boolean dataSparse;
final boolean fastCopy;
/*
part 1a = transit passed or generated by IOscenario, passed to IOtask
*/
final byte[] dataBlock;
/*
part 2 = used by IOscenario
*/
final int readWriteMode;
final int addressMode;
final int dataMode;
final int readDelay;
final int writeDelay;
final int copyDelay;
/*
part 3 = created by IOscenario
Scalar fields    
*/
final long totalSize;
final int bufferCount;
final int tailSize;
/*
Vector fields
*/
final FileSystem fileSystem;
final String[] namesSrc;
final String[] namesDst;
final Path[] pathsSrc;
final Path[] pathsDst;
/*
Read, Write, Copy, Delete helper and benchmarks results statistics support,
entries list (queue) for process synchronous monitoring.
*/
final StatisticsModel statistics;
final LinkedList<StateSync> syncQueue;
/*
Current executed phase id and name string, percentage, last error data,
this fields directly accessed and updated by IO tasks.
*/
int phaseID = -1;
String phaseName = "";
// double percentage = 0.0;
StatusEntry lastError;

/*
Constructor for options settings by internal defaults
*/    
public IOscenario()
    {
    pathSrc       = DEFAULT_PATH_SRC;
    prefixSrc     = DEFAULT_PREFIX_SRC;
    postfixSrc    = DEFAULT_POSTFIX_SRC;
    pathDst       = DEFAULT_PATH_DST;
    prefixDst     = DEFAULT_PREFIX_DST;
    postfixDst    = DEFAULT_POSTFIX_DST;
    fileCount     = DEFAULT_FILE_COUNT;
    fileSize      = DEFAULT_FILE_SIZE;
    blockSize     = DEFAULT_BLOCK_SIZE;
    threadCount   = DEFAULT_THREAD_COUNT;
    readSync      = DEFAULT_READ_SYNC;
    writeSync     = DEFAULT_WRITE_SYNC;
    copySync      = DEFAULT_COPY_SYNC;
    dataSparse    = DEFAULT_DATA_SPARSE;
    fastCopy      = DEFAULT_FAST_COPY;
    readWriteMode = DEFAULT_READ_WRITE_MODE;
    addressMode   = DEFAULT_ADDRESS_MODE;
    dataMode      = DEFAULT_DATA_MODE;
    readDelay     = DEFAULT_READ_DELAY;
    writeDelay    = DEFAULT_WRITE_DELAY;
    copyDelay     = DEFAULT_COPY_DELAY;
    dataBlock     = new byte[blockSize];
    for( int i=0; i<blockSize; i++ )
        dataBlock[i] = 0;
    
    totalSize = 
        (long)( fileCount & 0xFFFFFFFFL ) * (long)( fileSize & 0xFFFFFFFFL );
    bufferCount = fileSize / blockSize;
    tailSize    = fileSize % blockSize;
    fileSystem = FileSystems.getDefault();
    namesSrc = new String[fileCount];
    namesDst = new String[fileCount];
    pathsSrc = new Path[fileCount];
    pathsDst = new Path[fileCount];
    namesAndPathsInitHelper();
    
// note async channel is multi-thread (per files) even if thread count = 1
//    if ( threadCount < 2 )
//        {
//        statistics = new StatisticsModel( ID_COUNT );
//        }
//    else
//        {
    statistics = new StatisticsModel( fileCount, ID_COUNT );
//        }
    
    syncQueue = new LinkedList();
    lastError = new StatusEntry( true, "OK" );
    }

/*
Constructor for options settings by input parameters
*/
public IOscenario( String pathSrc, String prefixSrc, String postfixSrc,
                   String pathDst, String prefixDst, String postfixDst,
                   int fileCount, int fileSize, int blockSize, int threadCount,
                   boolean readSync, boolean writeSync, boolean copySync,
                   boolean dataSparse, boolean fastCopy, 
                   int readWriteMode, int addressMode, int dataMode,
                   int readDelay, int writeDelay, int copyDelay,
                   byte[] dataBlock )
    {
    this.pathSrc    = ( pathSrc == null    ) ? DEFAULT_PATH_SRC    : pathSrc;
    this.prefixSrc  = ( prefixSrc == null  ) ? DEFAULT_PREFIX_SRC  : prefixSrc;
    this.postfixSrc = ( postfixSrc == null ) ? DEFAULT_POSTFIX_SRC : postfixSrc;
    this.pathDst    = ( pathDst == null    ) ? DEFAULT_PATH_DST    : pathDst;
    this.prefixDst  = ( prefixDst == null  ) ? DEFAULT_PREFIX_DST  : prefixDst;
    this.postfixDst = ( postfixDst == null ) ? DEFAULT_POSTFIX_DST : postfixDst;
    this.fileCount  = ( fileCount == -1    ) ? DEFAULT_FILE_COUNT  : fileCount;
    this.fileSize   = ( fileSize == -1     ) ? DEFAULT_FILE_SIZE   : fileSize;
    this.blockSize  = ( blockSize == -1    ) ? DEFAULT_BLOCK_SIZE  : blockSize;
    this.threadCount =
        ( threadCount == -1 ) ? DEFAULT_THREAD_COUNT : threadCount;
    this.readSync   = readSync;
    this.writeSync  = writeSync;
    this.copySync   = copySync;
    this.dataSparse = dataSparse;
    this.fastCopy   = fastCopy;
    
    this.readWriteMode =
        ( readWriteMode == -1 ) ? DEFAULT_READ_WRITE_MODE : readWriteMode;
    this.addressMode =
        ( addressMode == -1 ) ? DEFAULT_ADDRESS_MODE : addressMode;
    this.dataMode =
        ( dataMode == -1 ) ? DEFAULT_DATA_MODE : dataMode;
    
    this.readDelay  = ( readDelay == -1    ) ? DEFAULT_READ_DELAY  : readDelay;
    this.writeDelay = ( writeDelay == -1   ) ? DEFAULT_WRITE_DELAY : writeDelay;
    this.copyDelay  = ( copyDelay == -1    ) ? DEFAULT_COPY_DELAY  : copyDelay;
    
    if ( dataBlock  == null )
        {
        this.dataBlock = new byte[blockSize];
        for( int i=0; i<blockSize; i++ )
            this.dataBlock[i] = 0;
        }
    else
        {
        this.dataBlock = dataBlock;
        }
    
    totalSize = 
        (long)( fileCount & 0xFFFFFFFFL ) * (long)( fileSize & 0xFFFFFFFFL );
    bufferCount = fileSize / blockSize;
    tailSize    = fileSize % blockSize;
    fileSystem = FileSystems.getDefault();
    namesSrc = new String[fileCount];
    namesDst = new String[fileCount];
    pathsSrc = new Path[fileCount];
    pathsDst = new Path[fileCount];
    namesAndPathsInitHelper();
    
//    if ( threadCount < 2 )
//        {
//        statistics = new StatisticsModel( ID_COUNT );
//        }
//    else
//        {
    statistics = new StatisticsModel( fileCount, ID_COUNT );
//        }

    syncQueue = new LinkedList();
    lastError = new StatusEntry( true, "OK" );
    }

/*
Helper for class constructor
*/    
private void namesAndPathsInitHelper()
    {
    for( int i=0; i<fileCount; i++ )
        {
        namesSrc[i] = pathSrc + prefixSrc + i + postfixSrc;
        namesDst[i] = pathDst + prefixDst + i + postfixDst;
        pathsSrc[i] = fileSystem.getPath( namesSrc[i] );
        pathsDst[i] = fileSystem.getPath( namesDst[i] );
        }
    }

/*
Get statistics
*/
public StatisticsModel getStatistics()
    {
    return statistics;
    }

/*
Asynchronous get I/O performance statistics
*/
public StateAsync[] getAsync()
    {
    StateAsync[] entries = new StateAsync[ID_COUNT];
    for( int i=0; i<ID_COUNT; i++ )
        {
        entries[i] = statistics.receive( i );
        }
    return entries;    
    }

/*
Asynchronous get current phase name
*/
public String getPhaseName()
    {
    return phaseName;
    }
/*
Asynchronous get current percentage
*/
// public double getPercentage()
//     {
//     return percentage;
//     }

/*
Asynchronous get last error
*/
public StatusEntry getLastError()
    {
    return lastError;
    }

/*
Get entry of synchronous statistics
*/
public StateSync getSync()
    {
    synchronized( syncQueue )
        {
        if ( syncQueue.isEmpty() )
            {
            return null;
            }
        else
            {
            return syncQueue.removeFirst();
            }
        }
    }

/*
Add entry of synchronous statistics, also used by child class
*/
void setSync( int count, StatusEntry se, int id, String name  )
    {
    if ( id >= 0 )
        {
        StateSync sc;
        if ( id < ID_COUNT )
            {
            StateAsync a = statistics.receive( id, count );  // TABLE BUG FIX
            if ( a != null )
                {
                sc = new StateSync( count, se, id, name,
                    a.current, a.min, a.max, a.average, a.median );
                }
            else
                {
                sc = new StateSync( count, se, id, name );
                }
            }
        else
            {
            sc = new StateSync( count, se, id, name );
            }
        synchronized( syncQueue )
            {
            syncQueue.add( sc );
            }
        }
    }

/*
SetSync method overload for parallel multi-thread execution support,
*/
final private int[] orderedCounts = new int[ID_COUNT];

synchronized void setSync( StatusEntry se, int id, String name  )
    {
    if ( ( id >= 0 )&&( id < ID_COUNT ) )
        {
        orderedCounts[id]++;
        setSync( orderedCounts[id], se, id, name );
        }
    }

/*
Clear synchronous statistics, also used by child class
*/
/*
void clearSync()
    {
    synchronized( syncQueue )
        {
        syncQueue.clear();
        }
    }
*/
}
