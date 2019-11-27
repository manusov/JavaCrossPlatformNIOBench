package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenario.READ_ID;
import static niobenchrefactoring.model.IOscenario.TOTAL_READ_ID;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class IOtaskChannelReadMT extends IOtaskChannelWriteMT
{
private final static String IOTASK_NAME = 
    "NIO channel multi thread MBPS, Read";
    
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
            iosc.statistics.startInterval( READ_ID, System.nanoTime() );
            do  {
                iosc.byteBuffer[threadIndex].rewind();
                k = iosc.channelsSrc[fileIndex].
                        read( iosc.byteBuffer[threadIndex] );
                } while ( k != -1);
            iosc.statistics.sendMBPS
                ( READ_ID, iosc.fileSize, System.nanoTime() );
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
        iosc.setSync( fileIndex+1, statusEntry, READ_ID, IOTASK_NAME );
        return statusEntry;
        }
    }
}
