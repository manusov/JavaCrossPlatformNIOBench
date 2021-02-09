/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Read phase at NIO Asynchronous Channels IO scenario.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import static java.nio.file.StandardOpenOption.READ;
import java.util.concurrent.CountDownLatch;
import static niobenchrefactoring.model.IOscenario.*;

public class IOtaskAsyncChannelRead extends IOtask
{
private final static String IOTASK_NAME = "Read NIO async channel";

IOtaskAsyncChannelRead( IOscenarioAsyncChannel ios )
    {
    super( ios );
    }

/*
Fields used by completer class
*/
IOscenarioAsyncChannel iosac;
CountDownLatch latch;

/*
Run IO task
*/
@Override public void run()
    {
    iosac = (IOscenarioAsyncChannel)ios;
    latch = new CountDownLatch( iosac.fileCount );
    Object attachment = null;
    try
        {
        iosac.totalBuffer.rewind();
        ByteBuffer[] duplicates = new ByteBuffer[iosac.fileCount];
        for( int i=0; i<iosac.fileCount; i++ )
            {
            iosac.channelsSrc[i] = 
                AsynchronousFileChannel.open( iosac.pathsSrc[i], READ );
            duplicates[i] = iosac.totalBuffer.duplicate();
            }
        iosac.statistics.startInterval( TOTAL_READ_ID, System.nanoTime() );
        for( int i=0; i<iosac.fileCount; i++ )
            {
            iosac.channelsSrc[i].read
                ( duplicates[i], 0, attachment, new AsyncCompleter( i ) );
            }
        try
            {
            latch.await();
            }
        catch ( InterruptedException e )
            {
            iosac.lastError = new StatusEntry
                ( false, "countdown read wait interrupted" + e.getMessage() );
            }
        for( int i=0; i<iosac.fileCount; i++ )
            {
            iosac.channelsSrc[i].close();
            }
        iosac.statistics.sendMBPS
            ( TOTAL_READ_ID, iosac.totalSize, System.nanoTime() );
        }
    catch( IOException e )
        {
        iosac.delete( iosac.pathsSrc, iosac.channelsSrc );
        iosac.lastError = 
            new StatusEntry( false, "Read error: " + e.getMessage() );
        }
    }

/*
Async completer for callback
*/
private class AsyncCompleter implements CompletionHandler< Integer, Object >
    {
    private final int id;
    AsyncCompleter( int i )
        {
        id = i;
        // Single file measured read start at completer class constructor
        iosac.statistics.startInterval
            ( id, READ_ID, System.nanoTime() );
        }
    @Override public void completed( Integer result, Object attachment )
        {
        iosac.statistics.sendMBPS
            ( id, READ_ID, iosac.fileSize, System.nanoTime() );
        // Single file measured read end when complete signaled
        iosac.setSync( iosac.lastError, READ_ID, IOTASK_NAME );
        // signal operation termination from this thread
        latch.countDown();
        }
    @Override public void failed( Throwable e, Object attachment )
        {
        iosac.lastError = new StatusEntry
            ( false, "Failed read completion" + e.getMessage() );
        }
    }
}
