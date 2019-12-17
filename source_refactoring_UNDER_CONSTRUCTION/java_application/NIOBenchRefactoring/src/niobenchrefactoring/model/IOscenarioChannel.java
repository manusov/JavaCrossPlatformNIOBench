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

import static niobenchrefactoring.model.HelperDelay.delay;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

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
Default constructor
*/
public IOscenarioChannel()
    {
    super();
    channelsSrc = new FileChannel[fileCount];
    channelsDst = new FileChannel[fileCount];
    byteBuffer = new ByteBuffer[threadCount];
    byteBufferTail = new ByteBuffer[threadCount];
    buffersInitHelper();
    }

/*
Constructor with parameters
*/
public IOscenarioChannel
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
    IOtask iotWrite;    
    IOtask iotCopy;
    IOtask iotRead;
    
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
    
    if ( ( readWriteMode != READ_ONLY ) && errorCheck() )
        {
        preDelay( writeDelay, WRITE_DELAY_NAME );
        threadHelper( iotWrite );
        }
    
    if ( ( readWriteMode != READ_ONLY ) && errorCheck() )
        {
        preDelay( copyDelay, COPY_DELAY_NAME );
        threadHelper( iotCopy );
        }
    
    if ( ( readWriteMode != WRITE_ONLY ) && errorCheck() )
        {
        preDelay( readDelay, READ_DELAY_NAME );
        threadHelper( iotRead );
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
Helper for interrupt when error
*/
boolean errorCheck()
    {
    if ( lastError == null )
        return true;
    else
        {
        return lastError.flag;
        }
    }


/*
Helper for run thread and wait it termination
*/
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

/*
Helper for time delay with seconds visualization
*/
void preDelay( int milliseconds, String name )
    {
    if ( milliseconds > 0 )
        {
        int seconds = milliseconds / 1000;
        milliseconds = milliseconds % 1000;
        int i = 0;
        if ( seconds > 0 )
            {  // delay integer count of seconds
            for( i=0; i<seconds; i++ )
                {
                setSync( i, lastError, DELAY_ID, name );
                delay( 1000 );
                }
            }
        if ( milliseconds > 0 )
            {  // delay tail
            setSync( i, lastError, DELAY_ID, name );
            delay( milliseconds );
            }
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