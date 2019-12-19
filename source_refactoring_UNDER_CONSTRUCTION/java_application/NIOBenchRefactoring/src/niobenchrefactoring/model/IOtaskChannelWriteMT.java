/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Write phase at NIO Channels and Buffers IO scenario.
Multi-thread version.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenario.TOTAL_WRITE_ID;
import static niobenchrefactoring.model.IOscenario.WRITE_ID;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.DSYNC;
import static java.nio.file.StandardOpenOption.SPARSE;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class IOtaskChannelWriteMT extends IOtaskChannelWrite
{
private final static String IOTASK_NAME = "Write/MT/NIO channel";

final IOscenarioChannel iosc;
final ExecutorService executor;
Callable<StatusEntry>[] tasks;
FutureTask[] futureTasks;

/*
Constructor stores IO scenario object, create executor
*/
IOtaskChannelWriteMT( IOscenarioChannel ios )
    {
    super( ios );
    iosc = (IOscenarioChannel)ios;
    executor = Executors.newFixedThreadPool( iosc.threadCount );
    }

/*
Run IO task
*/
@Override public void run()
    {
    /*
    Create task list    
    */
    tasks = new WriteTask[iosc.fileCount];
    futureTasks = new FutureTask[iosc.fileCount];
    int j = 0;
    for( int i=0; i<iosc.fileCount; i++ )
        {
        tasks[i] = new WriteTask( i, j );
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
            Files.createFile( iosc.pathsSrc[i] );
            if ( iosc.dataSparse )
                iosc.channelsSrc[i] = FileChannel.open
                    ( iosc.pathsSrc[i], APPEND, DSYNC, SPARSE );
            else if ( iosc.writeSync )
                iosc.channelsSrc[i] = FileChannel.open
                    ( iosc.pathsSrc[i], APPEND, DSYNC );
            else
                iosc.channelsSrc[i] = FileChannel.open
                    ( iosc.pathsSrc[i], APPEND );
            }
        }
    catch( IOException e )
        {
        iosc.delete( iosc.pathsSrc, iosc.channelsSrc );
        iosc.lastError = 
            new StatusEntry( false, "Write error: " + e.getMessage() );
        }
    /*
    Write files
    */
    iosc.statistics.startInterval( TOTAL_WRITE_ID, System.nanoTime() );
    for( int i=0; i<iosc.fileCount; i++ )
        executor.execute( futureTasks[i] );
    StatusEntry statusEntry = executorShutdownAndWait( executor );
    iosc.statistics.sendMBPS
        ( TOTAL_WRITE_ID, iosc.totalSize, System.nanoTime() );
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
File Write task for parallel execution
*/
private class WriteTask implements Callable<StatusEntry>
    {
    private final int fileIndex;
    private final int threadIndex;
    private WriteTask( int fileIndex, int threadIndex )
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
            int j = iosc.fileSize;
            iosc.statistics.startInterval
                    ( threadIndex, WRITE_ID, System.nanoTime() );
            while( j >= iosc.blockSize )
                {
                iosc.byteBuffer[threadIndex].rewind();
                int k = 0;
                while( k < iosc.blockSize )
                    {
                    k += iosc.channelsSrc[fileIndex].
                            write( iosc.byteBuffer[threadIndex] );
                    }
                j -= iosc.blockSize;
                }
            if ( j > 0 )
                {
                iosc.byteBufferTail[threadIndex].rewind();
                int k = 0;
                while ( k < j )
                    {
                    k += iosc.channelsSrc[fileIndex].
                            write( iosc.byteBufferTail[threadIndex] );
                    }
                }
            if ( iosc.writeSync )
                iosc.channelsSrc[fileIndex].force( true );
            iosc.statistics.sendMBPS
                ( threadIndex, WRITE_ID, iosc.fileSize, System.nanoTime() );
            iosc.channelsSrc[fileIndex].close();
            }
        catch( IOException e )
            {
            iosc.delete( iosc.pathsSrc, iosc.channelsSrc );
            statusFlag = false;
            statusString = "Write error: " + e.getMessage();
            }
        StatusEntry statusEntry = new StatusEntry( statusFlag, statusString );
        if ( ! statusEntry.flag )
            iosc.lastError = statusEntry;
        iosc.setSync( statusEntry, WRITE_ID, IOTASK_NAME );
        return statusEntry;
        }
    }

/*
Helper for parallel executor all running tasks execution wait
*/
StatusEntry executorShutdownAndWait( ExecutorService executor )
    {
    boolean statusFlag = true;
    String statusString = "OK";
    try
        {
        executor.shutdown();
        boolean b = executor.awaitTermination( 10, TimeUnit.MINUTES );
        if ( !b )
            {
            statusFlag = false;
            statusString = "Executor wait timeout";
            }
        }
    catch ( InterruptedException e )
        {
        statusFlag = false;
        statusString = "Executor wait: " + e.getMessage();
        }
    return new StatusEntry( statusFlag, statusString );
    }

/*
Helper for parallel executor shutdown, method can be used by child classes
*/
StatusEntry executorShutdown( ExecutorService executor )
    {
    boolean statusFlag = true;
    String statusString = "OK";
    if (( ! executor.isTerminated() )||( ! executor.isShutdown() ))
        {
        try
            {
            executor.shutdown();
            executor.awaitTermination( 5, TimeUnit.SECONDS );
            }
        catch (InterruptedException e )
            {
            statusFlag = false;
            statusString = "Executor shutdown: " + e.getMessage();
            }
        finally
            {
            if (( ! executor.isTerminated() )||( ! executor.isShutdown() ))
                {
                statusFlag = false;
                statusString = "Non-finished tasks termination forced";
                executor.shutdownNow();
                if (( ! executor.isTerminated() )||( ! executor.isShutdown() ))
                    {
                    statusString = "Cannot shutdown tasks";
                    }
                }
            }
        }
    return new StatusEntry( statusFlag, statusString );
    }

}
