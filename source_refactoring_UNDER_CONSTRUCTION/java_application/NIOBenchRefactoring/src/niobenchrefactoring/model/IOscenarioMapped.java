/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO scenario class for Java NIO Memory-mapped files benchmark, include phases: 
write files, copy files, read files.
Note java version-specific handler used for close memory-mapped buffer after
mapping operation, otherwise files can't be deleted.
*/


/*

TODO. ADD ENABLE-DISABLE OPERATIONS F(MODE), INCLUDE READ-ONLY.
TODO. ADD DELAYS FOR WRITE/COPY/READ BY USER SETTINGS.
TODO. BUFFERS COUNT = THREADS COUNT < FILES COUNT CAN BE NOT THREAD SAFE,
CAN BE SOLUTION BY BUFFER.DUPLICATE.

TODO. MOVE THIS TO PARENT CLASS, REMOVE REPLICATIONS.

TODO. SHOW PHASE AND INTERVAL NAMES FOR DELAY INTERVALS

TODO. MAKE BYTE BUFFER [] SCALAR IF MULTI-THREAD
NOT USED FOR MEMORY MAPPED FILES,
OR IF VECTOR BUFFERS NOT USED FOR SELECTED SCENARIO

TODO. ADD ENABLE-DISABLE OPERATIONS F(MODE), INCLUDE READ-ONLY.
TODO. ADD DELAYS FOR WRITE/COPY/READ BY USER SETTINGS.
TODO. BUFFERS COUNT = THREADS COUNT < FILES COUNT CAN BE NOT THREAD SAFE,
CAN BE SOLUTION BY BUFFER.DUPLICATE.

*/


package niobenchrefactoring.model;

import static niobenchrefactoring.model.HelperDelay.delay;
import static niobenchrefactoring.model.IOscenario.READ_ONLY;
import static niobenchrefactoring.model.IOscenario.WRITE_ONLY;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class IOscenarioMapped extends IOscenario
{
/*
Required java version-specific handler for close memory-mapped buffer
*/
final HelperUnmap helperUnmap;
/*
Vector fields, by number of threads, required for parallel execution
*/
final ByteBuffer[] byteBuffer;
final ByteBuffer[] byteBufferTail;

/*
Default constructor
*/
public IOscenarioMapped()
    {
    super();
    helperUnmap = new HelperUnmap();
    byteBuffer = new ByteBuffer[threadCount];
    byteBufferTail = new ByteBuffer[threadCount];
    buffersInitHelper();
    }

/*
Constructor with parameters
*/
public IOscenarioMapped
    ( String pathSrc, String prefixSrc, String postfixSrc,
      String pathDst, String prefixDst, String postfixDst,
      int fileCount, int fileSize, int blockSize, int threadCount,
      boolean readSync, boolean writeSync, boolean copySync, 
      boolean dataSparse, boolean fastCopy,
      int readWriteMode, int addressMode, int dataMode,
      int readDelay, int writeDelay, int copyDelay,
      byte[] dataBlock )
    {
    super( pathSrc, prefixSrc, postfixSrc, pathDst, prefixDst, postfixDst,
           fileCount, fileSize, blockSize, threadCount,
           readSync, writeSync, copySync, dataSparse, fastCopy,
           readWriteMode, addressMode, dataMode,
           readDelay, writeDelay, copyDelay, dataBlock );
    helperUnmap = new HelperUnmap();
    byteBuffer = new ByteBuffer[threadCount];
    byteBufferTail = new ByteBuffer[threadCount];
    buffersInitHelper();
    }

/*
Helper for class constructor    
*/    
private void buffersInitHelper()
    {
    for( int i=0; i<threadCount; i++ )
        {
        byteBuffer[i] = ByteBuffer.allocateDirect( blockSize );
        byteBuffer[i].put( dataBlock );
        if ( tailSize != 0 )
            {
            byteBufferTail[i] = ByteBuffer.allocateDirect( tailSize );
            byteBufferTail[i].put( dataBlock, 0, tailSize );
            }
        }
    }

/*
Run performance scenario    
*/
@Override public void run()
    {
    
    IOtask iotWrite = new IOtaskMappedWrite( this );
    IOtask iotCopy  = new IOtaskMappedCopy( this );
    IOtask iotRead  = new IOtaskMappedRead( this );

    if ( readWriteMode != READ_ONLY )
        {
        delay( writeDelay );
        threadHelper( iotWrite );
        delay( copyDelay );
        threadHelper( iotCopy );
        }
    
    if ( readWriteMode != WRITE_ONLY )
        {
        delay( readDelay );
        threadHelper( iotRead );
        }
    
    if ( readWriteMode == READ_WRITE )
        {
        /*
        Phase = Delete, note about files not deleted in WRITE_ONLY mode.
        Note delete operation cycles for all files is not interruptable.
        */
        delete( namesSrc );
        delete( namesDst );
        }

    }

// TODO. MOVE THIS TO PARENT CLASS, REMOVE REPLICATIONS.
/*
Helper for run thread and wait it termination
*/
/*
private void threadHelper( Thread t )
    {
    t.start();
    int postCount = 3;
    while( postCount > 0 )
        {
        HelperDelay.delay( 150 );
        if ( ! t.isAlive() )
            postCount--;
        }
    }
*/

/*
Helpers for delete files
Array of paths
*/
StatusEntry delete( String[] names )
    {
    boolean statusFlag = true;
    String statusString = "OK";
    for ( String name : names )
        {
        StatusEntry entry = delete( name );
        if ( ! entry.flag )
            {
            statusFlag = entry.flag;
            statusString = entry.string;
            }
        }
    return new StatusEntry( statusFlag, statusString );    
    }
/*
Overloaded for single path
*/
StatusEntry delete( String name )
    {
    return unmapAndDelete( name, null );
    }
/*
Delete file, required java version-specific unmap buffer.
This variant with more detail error reporting and buffer unmap feature.
Can call this method with filePath = null for unmap only without delete file.
Can call this method with buffer = null for delete file without unmap buffer.
*/
StatusEntry unmapAndDelete( String name, MappedByteBuffer buffer )
    {
    StatusEntry entry = new StatusEntry( true, "N/A" );
    /*
    Unmap buffer, if not null, exit with status details if error
    */
    if ( buffer != null )
        {
        entry = helperUnmap.unmap( buffer );
        if ( ! entry.flag )
            {
            return entry;  // exit with status, if error
            }
        }
    /*
    Delete file with status string extraction, if error
    */
    if ( name != null )
        {
        FileSystem fs = FileSystems.getDefault();
        Path filePath = fs.getPath( name );
        boolean statusFlag = true;
        String statusString = "N/A";
        try 
            {
            Files.delete( filePath );
            }
        catch ( IOException e )
            {
            statusFlag = false;
            statusString = "Delete error: " + e.getMessage();
            }
        entry = new StatusEntry( statusFlag, statusString );
        }
    return entry;
    }
}
