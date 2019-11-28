/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Copy phase at NIO Memory Mapped Files IO scenario.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenario.COPY_ID;
import static niobenchrefactoring.model.IOscenario.TOTAL_COPY_ID;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class IOtaskMappedCopy extends IOtask
{
private final static String IOTASK_NAME = 
    "NIO memory-mapped file single thread MBPS, Copy";

/*
Constructor stores IO scenario object
*/
IOtaskMappedCopy( IOscenarioMapped ios )
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
    MappedByteBuffer bufferDst = null;
    byte[] block = new byte[iosm.blockSize];
    byte[] blockTail = new byte[iosm.tailSize];
    // All files total measured (include context support) copy cycle start
    iosm.statistics.startInterval( TOTAL_COPY_ID, System.nanoTime() );
    //
    for( int i=0; i<iosm.fileCount; i++ )
        {
        try ( RandomAccessFile rafSrc = 
              new RandomAccessFile( iosm.namesSrc[i], "r"  );
              RandomAccessFile rafDst = 
              new RandomAccessFile( iosm.namesDst[i], "rw" );
              FileChannel channelSrc = rafSrc.getChannel();
              FileChannel channelDst = rafDst.getChannel() )
            {
            bufferSrc = channelSrc.
                map( FileChannel.MapMode.READ_ONLY, 0, iosm.fileSize );
            bufferDst = channelDst.
                map( FileChannel.MapMode.READ_WRITE, 0, iosm.fileSize );
            // Get timer for single file without operation
            iosm.statistics.startInterval( COPY_ID, System.nanoTime() );
            //
            if ( iosm.fastCopy )
                {
                bufferDst.put( bufferSrc );
                }
            else
                {
                for( int j=0; j<iosm.bufferCount; j++ )
                    {
                    bufferSrc.get( block );
                    bufferDst.put( block );
                    }
                if ( iosm.tailSize > 0 ) 
                    {
                    bufferSrc.get( blockTail );
                    bufferDst.put( blockTail );
                    }
                }
            channelDst.force( true );
            bufferDst.force();
            // End of time measured interval for single file without context
            iosm.statistics.
                sendMBPS( COPY_ID, iosm.fileSize, System.nanoTime() );
            //
            iosm.setSync( i+1, iosm.lastError, COPY_ID, IOTASK_NAME );
            }
        catch( Exception e )            
            {   // note unmap required before delay
            iosm.unmapAndDelete( null, bufferSrc );
            iosm.unmapAndDelete( null, bufferDst );
            iosm.delete( iosm.namesSrc );
            iosm.lastError = new StatusEntry
                ( false, "Mapped copy error: " + e.getMessage() );
            }
        finally
            {  // this call without delete, path string = null
            StatusEntry entry = iosm.unmapAndDelete( null, bufferSrc );
            if ( ! entry.flag )
                iosm.lastError = entry;
            entry = iosm.unmapAndDelete( null, bufferDst );
            if ( ! entry.flag )
                iosm.lastError = entry;
            }
        }
    //
    iosm.statistics.
        sendMBPS( TOTAL_COPY_ID, iosm.totalSize, System.nanoTime() );
    // All files total measured (include context support) copy cycle end
    }
}
