/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Write phase at NIO Memory Mapped Files IO scenario.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenario.TOTAL_WRITE_ID;
import static niobenchrefactoring.model.IOscenario.WRITE_ID;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class IOtaskMappedWrite extends IOtask
{
private final static String IOTASK_NAME = 
    "NIO memory-mapped file single thread MBPS, Write";

/*
Constructor stores IO scenario object
*/
IOtaskMappedWrite( IOscenarioMapped ios )
    {
    super( ios );
    }

/*
Run IO task
*/
@Override public void run()
    {
    IOscenarioMapped iosm = (IOscenarioMapped)ios;
    MappedByteBuffer buffer = null;
    // All files total measured (include context support) write cycle start
    iosm.statistics.startInterval( TOTAL_WRITE_ID, System.nanoTime() );
    //
    for( int i=0; i<iosm.fileCount; i++ )
        {
        try ( RandomAccessFile raf = 
              new RandomAccessFile( iosm.namesSrc[i], "rw" );
              FileChannel channel = raf.getChannel(); )
            {
            buffer = channel.
                map( FileChannel.MapMode.READ_WRITE, 0, iosm.fileSize );
            // Get timer for single file without operation
            iosm.statistics.startInterval( WRITE_ID, System.nanoTime() );
            //
            for( int j=0; j<iosm.bufferCount; j++ )
                {
                iosm.byteBuffer[0].rewind();
                buffer.put( iosm.byteBuffer[0] );
                }
            if ( iosm.tailSize > 0 ) 
                {
                iosm.byteBufferTail[0].rewind();
                buffer.put( iosm.byteBufferTail[0] );
                }
            channel.force( true );
            buffer.force();
            // End of time measured interval for single file without context
            iosm.statistics.
                sendMBPS( WRITE_ID, iosm.fileSize, System.nanoTime() );
            //
            iosm.setSync( i+1, iosm.lastError, WRITE_ID, IOTASK_NAME );
            }
        catch( Exception e )            
            {   // note unmap required before delay
            iosm.unmapAndDelete( null, buffer );
            iosm.delete( iosm.namesSrc );
            iosm.lastError = new StatusEntry
                ( false, "Mapped write error: " + e.getMessage() );
            }
        finally
            {  // this call without delete, path string = null
            StatusEntry entry = iosm.unmapAndDelete( null, buffer );
            if ( ! entry.flag )
                iosm.lastError = entry;
            }
        }
    //
    iosm.statistics.
        sendMBPS( TOTAL_WRITE_ID, iosm.totalSize, System.nanoTime() );
    // All files total measured (include context support) write cycle end
    }
}
