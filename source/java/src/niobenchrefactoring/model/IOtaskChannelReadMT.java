/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Read phase at NIO Channels and Buffers IO scenario.
Multi-thread version.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.*;
import static niobenchrefactoring.model.IOscenario.*;

class IOtaskChannelReadMT extends IOtaskChannelWriteMT
{
private final static String IOTASK_NAME = "Read/MT/NIO channel";
    
/*
Constructor stores IO scenario object, create executor
*/
IOtaskChannelReadMT( IOscenarioChannel ios )
    {
    super( ios );
    }

/*
Run IO task
*/
@Override public void run()
    {
    /*
    Create task list    
    */
    tasks = new ReadTask[iosc.fileCount];
    futureTasks = new FutureTask[iosc.fileCount];
    int j = 0;
    for( int i=0; i<iosc.fileCount; i++ )
        {
        tasks[i] = new ReadTask( i, j );
        futureTasks[i] = new FutureTask( tasks[i] );
        j++;
        if ( j >= iosc.threadCount )
            j = 0;
        }
    /*
    Read files
    */
    iosc.statistics.startInterval( TOTAL_READ_ID, System.nanoTime() );
    for( int i=0; i<iosc.fileCount; i++ )
        executor.execute( futureTasks[i] );
    StatusEntry statusEntry = executorShutdownAndWait( executor );
    iosc.statistics.sendMBPS
        ( TOTAL_READ_ID, iosc.totalSize, System.nanoTime() );
    /*
    Store status
    */
    if ( ( ! statusEntry.flag )&&( iosc.lastError.flag ) )
        iosc.lastError = statusEntry;
    /*
    Executor shutdown
    */
    statusEntry = executorShutdown( executor );
    if ( ( ! statusEntry.flag )&&( iosc.lastError.flag ) )
        iosc.lastError = statusEntry;
    }

/*
File Read task for parallel execution
*/
private class ReadTask implements Callable<StatusEntry>
    {
    private final int fileIndex;
    private final int threadIndex;
    private ReadTask( int fileIndex, int threadIndex )
        {
        this.fileIndex = fileIndex;
        this.threadIndex = threadIndex;
        }
    @Override public StatusEntry call()
        {
        boolean statusFlag = true;
        String statusString = "OK";
        try
            {
            int k;
            iosc.statistics.startInterval
                // ( threadIndex, READ_ID, System.nanoTime() );
                   ( fileIndex, READ_ID, System.nanoTime() );
            do  {
                iosc.byteBuffer[threadIndex].rewind();
                k = iosc.channelsSrc[fileIndex].
                        read( iosc.byteBuffer[threadIndex] );
                } while ( k != -1);
            iosc.statistics.sendMBPS
                // ( threadIndex, READ_ID, iosc.fileSize, System.nanoTime() );
                   ( fileIndex, READ_ID, iosc.fileSize, System.nanoTime() );
            }
        catch( ClosedByInterruptException e1 )
            {
            try
                {
                if ( iosc.channelsSrc[fileIndex] != null )
                    iosc.channelsSrc[fileIndex].close();
                }
            catch ( IOException e2 )
                {
                }
            }

        catch( IOException e )
            {
            iosc.delete( iosc.pathsSrc, iosc.channelsSrc );
            iosc.delete( iosc.pathsDst, iosc.channelsDst );
            statusFlag = false;
            statusString = "Read error: " + e.getMessage();
            }
        StatusEntry statusEntry = new StatusEntry( statusFlag, statusString );
        if ( ! statusEntry.flag )
            iosc.lastError = statusEntry;
        iosc.setSync( statusEntry, READ_ID, IOTASK_NAME );
        return statusEntry;
        }
    }
}
