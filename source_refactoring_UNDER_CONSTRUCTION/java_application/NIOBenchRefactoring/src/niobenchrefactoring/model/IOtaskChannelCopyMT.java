/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Copy phase at NIO Channels and Buffers IO scenario.
Multi-thread version.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenario.COPY_ID;
import static niobenchrefactoring.model.IOscenario.TOTAL_COPY_ID;
import java.io.IOException;
import java.nio.channels.FileChannel;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.DSYNC;
import static java.nio.file.StandardOpenOption.SPARSE;
import static java.nio.file.StandardOpenOption.WRITE;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class IOtaskChannelCopyMT extends IOtaskChannelWriteMT
{
private final static String IOTASK_NAME = "Copy/MT/NIO channel";
    
/*
Constructor stores IO scenario object, create executor
*/
IOtaskChannelCopyMT( IOscenarioChannel ios )
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
    tasks = new CopyTask[iosc.fileCount];
    futureTasks = new FutureTask[iosc.fileCount];
    int j = 0;
    for( int i=0; i<iosc.fileCount; i++ )
        {
        tasks[i] = new CopyTask( i, j );
        futureTasks[i] = new FutureTask( tasks[i] );
        j++;
        if ( j >= iosc.threadCount )
            j = 0;
        }
    
    /*
    Create files, yet zero size, make this work outside of measured interval,
    cycle for required number of files
    */
    try
        {
        for( int i=0; i<iosc.fileCount; i++ )
            {
            iosc.channelsSrc[i] = FileChannel.open( iosc.pathsSrc[i] );
            if ( iosc.dataSparse )
                iosc.channelsDst[i] = FileChannel.open
                    ( iosc.pathsDst[i], CREATE, WRITE, SPARSE );
            else if ( iosc.writeSync )
                iosc.channelsDst[i] = FileChannel.open
                    ( iosc.pathsDst[i], CREATE, WRITE, DSYNC );
            else
                iosc.channelsDst[i] = FileChannel.open
                    ( iosc.pathsDst[i], CREATE, WRITE );
            }
        }
    catch( IOException e )
        {
        iosc.delete( iosc.pathsSrc, iosc.channelsSrc );
        iosc.delete( iosc.pathsDst, iosc.channelsDst );
        iosc.lastError = 
            new StatusEntry( false, "Copy error: " + e.getMessage() );
        }
    /*
    Copy files
    */
    iosc.statistics.startInterval( TOTAL_COPY_ID, System.nanoTime() );
    for( int i=0; i<iosc.fileCount; i++ )
        executor.execute( futureTasks[i] );
    StatusEntry statusEntry = executorShutdownAndWait( executor );
    iosc.statistics.sendMBPS
        ( TOTAL_COPY_ID, iosc.totalSize, System.nanoTime() );
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
File Copy task for parallel execution
*/
private class CopyTask implements Callable<StatusEntry>
    {
    private final int fileIndex;
    private final int threadIndex;
    private CopyTask( int fileIndex, int threadIndex )
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
            int k = 0;
            if ( iosc.fastCopy )
                {
                iosc.statistics.startInterval
                        ( threadIndex, COPY_ID, System.nanoTime() );
                while( k < iosc.fileSize )
                    {
                    k += iosc.channelsSrc[fileIndex].transferTo
                        ( 0, iosc.channelsSrc[fileIndex].size(), 
                          iosc.channelsDst[fileIndex] );
                    }
                if ( iosc.copySync )
                    iosc.channelsDst[fileIndex].force( true );
                iosc.statistics.sendMBPS
                    ( threadIndex, COPY_ID, iosc.fileSize, System.nanoTime() );
                }
            else
                {
                iosc.statistics.startInterval
                        ( threadIndex, COPY_ID, System.nanoTime() );
                while( k < iosc.fileSize )
                    {
                    int n = iosc.blockSize;
                    int m = iosc.fileSize - k;
                    if ( n > m ) n = m;
                    k += iosc.channelsSrc[fileIndex].
                        transferTo( 0, n, iosc.channelsDst[fileIndex] );
                    if ( iosc.copySync )
                        iosc.channelsDst[fileIndex].force( true );
                    }
                iosc.statistics.sendMBPS
                    ( threadIndex, COPY_ID, iosc.fileSize, System.nanoTime() );
                }
            }
        catch( IOException e )
            {
            iosc.delete( iosc.pathsSrc, iosc.channelsSrc );
            iosc.delete( iosc.pathsDst, iosc.channelsDst );
            statusFlag = false;
            statusString = "Copy error: " + e.getMessage();
            }
        StatusEntry statusEntry = new StatusEntry( statusFlag, statusString );
        if ( ! statusEntry.flag )
            iosc.lastError = statusEntry;
        iosc.setSync( statusEntry, COPY_ID, IOTASK_NAME );
        return statusEntry;
        }
    }
}
