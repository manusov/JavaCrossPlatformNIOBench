/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO scenario class for Java NIO Channels and Buffers benchmark, include phases: 
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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;

public class IOscenarioChannel extends IOscenario
{
/*
Vector fields, by number of files, required for sequental execution
*/
final FileChannel[] channelsSrc;
final FileChannel[] channelsDst;
/*
Vector fields, by number of threads, required for parallel execution
*/
final ByteBuffer[] byteBuffer;
final ByteBuffer[] byteBufferTail;

/*
Constructor with parameters
*/
public IOscenarioChannel
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
    channelsSrc = new FileChannel[fileCount];
    channelsDst = new FileChannel[fileCount];
    byteBuffer = new ByteBuffer[threadCount];
    byteBufferTail = new ByteBuffer[threadCount];
    buffersInitHelper();
    }

/*
Helper for class constructor    
*/    
private void buffersInitHelper()
    {
    /*
    Pre-check memory size for prevent out-of-memory exception    
    */
    Runtime r = Runtime.getRuntime();
    long max = r.maxMemory();
    if ( ( (long)blockSize * (long)threadCount * 2 ) <= max )
        {  // memory allocation
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
    else
        {  // insufficient memory reporting
        lastError = new StatusEntry
            ( false, "Memory limit error, change block or threads" );
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
        
    if ( threadCount > 1 )
        {  // initialize tasks for multi-thread branch
        iotWrite = new IOtaskChannelWriteMT( this );
        iotCopy  = new IOtaskChannelCopyMT( this );
        iotRead  = new IOtaskChannelReadMT( this );
        }
    else
        {  // initialize tasks for single-thread branch
        iotWrite = new IOtaskChannelWrite( this );
        iotCopy  = new IOtaskChannelCopy( this );
        iotRead  = new IOtaskChannelRead( this );
        }
    
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
        delete( pathsSrc, channelsSrc );
        delete( pathsDst, channelsDst );
        }
    }

/*
Helper for delete files
*/
void delete( Path[] path, FileChannel[] channel )
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
