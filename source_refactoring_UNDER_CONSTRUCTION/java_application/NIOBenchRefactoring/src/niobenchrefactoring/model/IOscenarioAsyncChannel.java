/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO scenario class for Java NIO Asynchronous Channel benchmark, include phases: 
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
import static niobenchrefactoring.model.IOscenario.READ_ONLY;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class IOscenarioAsyncChannel extends IOscenario
{
final AsynchronousFileChannel[] channelsSrc;
final AsynchronousFileChannel[] channelsDst;
final ByteBuffer totalBuffer;

public IOscenarioAsyncChannel()
    {
    super();
    channelsSrc = new AsynchronousFileChannel[fileCount];
    channelsDst = new AsynchronousFileChannel[fileCount];
    totalBuffer = ByteBuffer.allocateDirect( fileSize );
    for( int i=0; i<bufferCount; i++ )
        totalBuffer.put( dataBlock );
    if ( tailSize > 0 )
        totalBuffer.put( dataBlock, 0, tailSize );
    }

public IOscenarioAsyncChannel
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
    channelsSrc = new AsynchronousFileChannel[fileCount];
    channelsDst = new AsynchronousFileChannel[fileCount];
    totalBuffer = ByteBuffer.allocateDirect( fileSize );
    for( int i=0; i<bufferCount; i++ )
        totalBuffer.put( dataBlock );
    if ( tailSize > 0 )
        totalBuffer.put( dataBlock, 0, tailSize );
    }

    
@Override public void run()
    {
   
    IOtask iotWrite = new IOtaskAsyncChannelWrite( this );
    IOtask iotRead  = new IOtaskAsyncChannelRead( this );
    
    if ( readWriteMode != READ_ONLY )
        {
        delay( writeDelay );
        threadHelper( iotWrite );
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
        delete( pathsSrc, channelsSrc );
        delete( pathsDst, channelsDst );
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
Helper for delete files
*/
void delete( Path[] path, AsynchronousFileChannel[] channel )
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
