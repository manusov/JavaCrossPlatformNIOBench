/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Unpack phase at archives IO scenario.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import static java.nio.file.StandardOpenOption.*;
import java.util.concurrent.*;
import java.util.zip.ZipInputStream;
import static niobenchrefactoring.model.IOscenario.TOTAL_UNPACK_ID;
import static niobenchrefactoring.model.IOscenario.UNPACK_ID;

public class IOtaskArchiveUnpack extends IOtaskChannelWriteMT // IOtask
{
private final static String IOTASK_NAME = "Unpack/Zip";
final IOscenarioArchives iosa;

/*
Constructor stores IO scenario object
*/
IOtaskArchiveUnpack( IOscenarioChannel ios )
    {
    super( ios );
    iosa = (IOscenarioArchives)ios;
    }

/*
Run IO task
*/
@Override public void run()
    {
    /*
    Create task list    
    */
    tasks = new UnpackTask[iosa.fileCount];
    futureTasks = new FutureTask[iosa.fileCount];
    int j = 0;
    for( int i=0; i<iosa.fileCount; i++ )
        {
        tasks[i] = new UnpackTask( i, j );
        futureTasks[i] = new FutureTask( tasks[i] );
        j++;
        if ( j >= iosa.threadCount )
            j = 0;
        }
    /*
    Open channel
    */
    try
        {
        for( int i=0; i<iosa.fileCount; i++ )
            {
            iosa.channelsDst[i] = 
                    FileChannel.open( iosa.pathsDst[i], CREATE, APPEND );
            }
        }
    catch ( IOException e )
        {
        iosa.delete( iosa.pathsSrc, iosa.channelsSrc );
        iosa.delete( iosa.pathsDst, iosa.channelsDst );
        iosa.deleteZip( iosa.pathsZip, iosa.zis, iosa.zos );
        iosa.lastError = new StatusEntry( false, e.getMessage() );
        }
    /*
    Unpack files
    */
    iosa.statistics.startInterval( TOTAL_UNPACK_ID, System.nanoTime() );
    for( int i=0; i<iosa.fileCount; i++ )
        executor.execute( futureTasks[i] );
    StatusEntry statusEntry = executorShutdownAndWait( executor );
    iosa.statistics.sendMBPS
        ( TOTAL_UNPACK_ID, iosa.totalSize, System.nanoTime() );
    /*
    Store status
    */
    if ( ( ! statusEntry.flag )&&( iosa.lastError.flag ) )
        iosa.lastError = statusEntry;
    /*
    Executor shutdown
    */
    statusEntry = executorShutdown( executor );
    if ( ( ! statusEntry.flag )&&( iosa.lastError.flag ) )
        iosa.lastError = statusEntry;
    }

/*
File pack task for parallel execution
*/
private class UnpackTask implements Callable<StatusEntry>
    {
    private final int fileIndex;
    private final int threadIndex;
    private UnpackTask( int fileIndex, int threadIndex )
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
            // start measured interval
            iosa.statistics.startInterval
                ( fileIndex, UNPACK_ID, System.nanoTime() );
            //
            iosa.zis[fileIndex].close();
            iosa.zis[fileIndex] = new ZipInputStream
                ( new FileInputStream( iosa.namesZip[fileIndex] ) );
            while( ( iosa.zis[fileIndex].getNextEntry() ) != null )
                {
                byte[] array = new byte[iosa.blockSize];
                while ( iosa.zis[fileIndex].read( array ) > 0 ) {}
                ByteBuffer buffer = ByteBuffer.wrap( array );
                while ( iosa.channelsDst[fileIndex].write( buffer ) > 0 ) {}
                iosa.channelsDst[fileIndex].force( true );
                iosa.zis[fileIndex].closeEntry();
                }
            // iosa.zis[fileIndex].close();
            
            // end measured interval
            iosa.statistics.sendMBPS
                ( fileIndex, UNPACK_ID, iosa.fileSize, System.nanoTime() );
            //
            }
        catch ( IOException e )
            {
            iosa.delete( iosa.pathsSrc, iosa.channelsSrc );
            iosa.delete( iosa.pathsDst, iosa.channelsDst );
            iosa.deleteZip( iosa.pathsZip, iosa.zis, iosa.zos );
            statusFlag = false;
            statusString = e.getMessage();
            }
        StatusEntry statusEntry = new StatusEntry( statusFlag, statusString );
        if ( ! statusEntry.flag )
            iosa.lastError = statusEntry;
        iosa.setSync( statusEntry, UNPACK_ID, IOTASK_NAME );
        return statusEntry;
        }
    }
}
