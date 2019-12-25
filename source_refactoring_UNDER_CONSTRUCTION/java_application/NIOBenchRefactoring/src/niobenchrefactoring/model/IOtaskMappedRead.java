/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Read phase at NIO Memory Mapped Files IO scenario.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenario.READ_ID;
import static niobenchrefactoring.model.IOscenario.TOTAL_READ_ID;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class IOtaskMappedRead extends IOtask
{
private final static String IOTASK_NAME = "Read/NIO memory mapped";

/*
Constructor stores IO scenario object
*/
IOtaskMappedRead( IOscenarioMapped ios )
    {
    super( ios );
    }

/*
Run IO task
*/
@Override public void run()
    {
    IOscenarioMapped iosm = (IOscenarioMapped)ios;
    MappedByteBuffer bufferSrc = null;
    byte[] block = new byte[iosm.blockSize];
    byte[] blockTail = new byte[iosm.tailSize];
    // All files total measured (include context support) read cycle start
    iosm.statistics.startInterval( TOTAL_READ_ID, System.nanoTime() );
    //
    for( int i=0; i<iosm.fileCount; i++ )
        {
        try ( RandomAccessFile rafSrc = 
              new RandomAccessFile( iosm.namesSrc[i], "r"  );
              FileChannel channelSrc = rafSrc.getChannel() )
            {
            bufferSrc = channelSrc.
                map( FileChannel.MapMode.READ_ONLY, 0, iosm.fileSize );
            // Get timer for single file without operation
            iosm.statistics.startInterval( READ_ID, System.nanoTime() );
            //
            for( int j=0; j<iosm.bufferCount; j++ )
                bufferSrc.get( block );
            if ( iosm.tailSize > 0 ) 
                bufferSrc.get( blockTail );
            
            // End of time measured interval for single file without context
            iosm.statistics.
                sendMBPS( READ_ID, iosm.fileSize, System.nanoTime() );
            //
            iosm.setSync( i+1, iosm.lastError, READ_ID, IOTASK_NAME );
            }
        catch( Exception e )            
            {   // note unmap required before delay
            iosm.unmapAndDelete( null, bufferSrc );
            iosm.delete( iosm.namesSrc );
            iosm.lastError = new StatusEntry( false, e.getMessage() );
            }
        finally
            {  // this call without delete, path string = null
            StatusEntry entry = iosm.unmapAndDelete( null, bufferSrc );
            if ( ! entry.flag )
                iosm.lastError = entry;
            }
        }
    //
    iosm.statistics.
        sendMBPS( TOTAL_READ_ID, iosm.totalSize, System.nanoTime() );
    // All files total measured (include context support) write cycle end
    }
}
