/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO scenario class for Java NIO Scatter-Gather files benchmark, include phases: 
write files, copy files, read files.
*/

/*

TODO. ADD ENABLE-DISABLE OPERATIONS F(MODE), INCLUDE READ-ONLY.
TODO. ADD DELAYS FOR WRITE/COPY/READ BY USER SETTINGS.
TODO. BUFFERS COUNT = THREADS COUNT < FILES COUNT CAN BE NOT THREAD SAFE,
CAN BE SOLUTION BY BUFFER.DUPLICATE.

TODO. MOVE THREAD HELPER TO PARENT CLASS, REMOVE REPLICATIONS.

TODO. SHOW PHASE AND INTERVAL NAMES FOR DELAY INTERVALS

*/

package niobenchrefactoring.model;

import java.io.IOException;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import static niobenchrefactoring.model.IOscenario.*;

public class IOscenarioScatterGather extends IOscenario
{
private final static int SUB_BUFFER_SIZE = 4096;    

final GatheringByteChannel[] gatherWriters;
final ScatteringByteChannel[] scatterReaders;
final ByteBuffer[] multiBuffer;
final ByteBuffer[] multiBufferTail;

/*
Constructor with parameters
*/
public IOscenarioScatterGather
    ( String pathSrc, String prefixSrc, String postfixSrc,
      String pathDst, String prefixDst, String postfixDst,
      int fileCount, int fileSize, int blockSize, int threadCount,
      boolean readSync, boolean writeSync, boolean copySync, 
      boolean writeSyncSparse, boolean copySyncSparse, 
      boolean fastCopy,
      int readWriteMode, int addressMode, int dataMode,
      int readDelay, int writeDelay, int copyDelay,
      byte[] dataBlock )
    {
    super( pathSrc, prefixSrc, postfixSrc, pathDst, prefixDst, postfixDst,
           fileCount, fileSize, blockSize, threadCount,
           readSync, writeSync, copySync, writeSyncSparse, copySyncSparse,
           fastCopy,
           readWriteMode, addressMode, dataMode,
           readDelay, writeDelay, copyDelay, dataBlock );
    gatherWriters = new GatheringByteChannel[fileCount];
    scatterReaders = new ScatteringByteChannel[fileCount];
    /*
    Pre-check memory size for prevent out-of-memory exception    
    */
    Runtime r = Runtime.getRuntime();
    long max = r.maxMemory();
    if ( ( (long)blockSize * (long)threadCount * 2 ) <= max )
        {  // memory allocation
        multiBuffer = multiBufferInitHelper( blockSize );
        multiBufferTail = multiBufferInitHelper( tailSize );
        }
    else
        {  // insufficient memory reporting
        lastError = new StatusEntry
            ( false, "Memory limit error, change block or threads" );
        multiBuffer = null;
        multiBufferTail = null;
        }
    }

/*
Helper for class constructor    
*/
private ByteBuffer[] multiBufferInitHelper( int bufferSize )
    {
    if ( bufferSize != 0 )
        {
        int subCount = bufferSize / SUB_BUFFER_SIZE;
        int subTail  = bufferSize % SUB_BUFFER_SIZE;
        int n = ( subTail == 0 ) ? subCount : subCount + 1;
        ByteBuffer[] buffers = new ByteBuffer[n];
        int i;
        int offset = 0;
        for( i=0; i<subCount; i++ )
            {
            buffers[i] = ByteBuffer.allocateDirect( SUB_BUFFER_SIZE );
            buffers[i].put( dataBlock, offset, SUB_BUFFER_SIZE );
            offset += SUB_BUFFER_SIZE;
            }
        if ( subTail != 0 )
            {
            buffers[i] = ByteBuffer.allocateDirect( subTail );
            buffers[i].put( dataBlock, offset, subTail );
            }
        return buffers;
        }
    else
        {
        return null;
        }
    }

/*
Run performance scenario    
*/
@Override public void run()
    {
    if ( ! errorCheck() )
        {  // this used for exit by memory overflow pre-detection
        setSync( 0, lastError, 0, "N/A" );
        return;
        }

    iotWrite = new IOtaskScatterGatherWrite( this );
    iotCopy  = new IOtaskScatterGatherCopy( this );
    iotRead  = new IOtaskScatterGatherRead( this );

    if ( ( readWriteMode != READ_ONLY ) &&
         ( ! interrupt ) && ( ! isInterrupted() ) && errorCheck() )
        {
        if ( preDelay( writeDelay, WRITE_DELAY_NAME ) )
            {
            threadHelper( iotWrite );
            }
        }
    
    if ( ( readWriteMode != READ_ONLY ) && 
         ( ! interrupt ) && ( ! isInterrupted() ) && errorCheck() )
        {
        if ( preDelay( copyDelay, COPY_DELAY_NAME ) )
            {
            threadHelper( iotCopy );
            }
        }
    
    if ( ( readWriteMode != WRITE_ONLY ) && 
         ( ! interrupt ) && ( ! isInterrupted() ) && errorCheck() )
        {
        if ( preDelay( readDelay, READ_DELAY_NAME ) )
            {
            threadHelper( iotRead );
            }
        }

    if ( readWriteMode == READ_WRITE )
        {
        setSync( 0, lastError, DELETE_ID, DELETE_NAME );
        /*
        Phase = Delete, note about files not deleted in WRITE_ONLY mode.
        Note delete operation cycles for all files is not interruptable.
        */
        delete( pathsSrc, scatterReaders );
        delete( pathsDst, gatherWriters );
        }
    }

/*
Helper for delete files
*/
void delete( Path[] path, Channel[] channel )
    {
    if ( ( path == null )||( channel == null )||
         ( path.length == 0 )||( channel.length != path.length ) )
        {
        lastError = new StatusEntry( false, "Delete context bad" );
        }
    else
        {
        for( int i=0; i<path.length; i++ )
        try
            {
            if ( path[i]    != null )  Files.delete( path[i] );
            if ( channel[i] != null )  channel[i].close();
            }
        catch ( IOException e )
            {
            lastError =
                new StatusEntry( false, "Delete error: " + e.getMessage() );
            }
        }
    }
}
