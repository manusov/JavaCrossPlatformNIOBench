/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Pack phase at archives IO scenario.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import static niobenchrefactoring.model.IOscenario.*;

class IOtaskArchivePack extends IOtaskChannelWriteMT
{
private final static String IOTASK_NAME = "Pack/Zip";
final IOscenarioArchives iosa;

/*
Constructor stores IO scenario object
*/
IOtaskArchivePack( IOscenarioChannel ios )
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
    tasks = new PackTask[iosa.fileCount];
    futureTasks = new FutureTask[iosa.fileCount];
    int j = 0;
    for( int i=0; i<iosa.fileCount; i++ )
        {
        tasks[i] = new PackTask( i, j );
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
            iosa.channelsSrc[i] = FileChannel.open( iosa.pathsSrc[i] );
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
    Pack files
    */
    iosa.statistics.startInterval( TOTAL_PACK_ID, System.nanoTime() );
    for( int i=0; i<iosa.fileCount; i++ )
        executor.execute( futureTasks[i] );
    StatusEntry statusEntry = executorShutdownAndWait( executor );
    iosa.statistics.sendMBPS
        ( TOTAL_PACK_ID, iosa.totalSize, System.nanoTime() );
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
private class PackTask implements Callable<StatusEntry>
    {
    private final int fileIndex;
    private final int threadIndex;
    private PackTask( int fileIndex, int threadIndex )
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
            String locName = "blk";
            iosa.statistics.startInterval
                ( fileIndex, PACK_ID, System.nanoTime() );
            
            long n = iosa.fileSize;
            int i = 0;
            while ( n > 0 )
                {
                // read file
                int j = 0;
                do  {
                    iosa.byteBuffer[threadIndex].rewind();
                    j += iosa.channelsSrc[fileIndex].
                        read( iosa.byteBuffer[threadIndex] );
                    } while ( j < iosa.blockSize );
                iosa.byteBuffer[threadIndex].rewind();
                byte[] array = 
                    new byte[ iosa.byteBuffer[threadIndex].remaining() ];
                iosa.byteBuffer[threadIndex].get( array );
                // write to archive
                ZipEntry entry = new ZipEntry
                    ( locName + "_" + fileIndex + "_" + i );
                iosa.zos[fileIndex].putNextEntry( entry );
                iosa.zos[fileIndex].write( array );
                iosa.zos[fileIndex].closeEntry();
                n -= array.length;
                i++;
                }
            iosa.zos[fileIndex].close();            
            
            // end measured interval
            iosa.statistics.sendMBPS
                ( fileIndex, PACK_ID, iosa.fileSize, System.nanoTime() );
            }
        catch( IOException e )
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
        iosa.setSync( statusEntry, PACK_ID, IOTASK_NAME );
        return statusEntry;
        }
    }
}
