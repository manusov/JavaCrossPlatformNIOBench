/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Read phase at Java Scatter-Gather file IO scenario.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import static niobenchrefactoring.model.IOscenario.*;

public class IOtaskScatterGatherRead extends IOtask
{
private final static String IOTASK_NAME = "Read/ST/NIO scatter-gather";

/*
Constructor stores IO scenario object
*/
IOtaskScatterGatherRead( IOscenarioScatterGather ios )
    {
    super( ios );
    }

/*
Run IO task
*/
@Override public void run()
    {
    IOscenarioScatterGather iosg = (IOscenarioScatterGather)ios;
    try
        {
        // All files total measured read cycle start
        iosg.statistics.startInterval( TOTAL_READ_ID, System.nanoTime() );
        //
        for( int i=0; i<iosg.fileCount; i++ )
            {
            if ( isInterrupted() ) break;
            iosg.scatterReaders[i] = FileChannel.open( iosg.pathsSrc[i] );
            int j = iosg.fileSize;
            // Single file measured read start
            iosg.statistics.startInterval( READ_ID, System.nanoTime() );
            // read sequence of blocks
            while ( j >= iosg.blockSize )
                {
                // rewind all buffers of multi-buffer
                for ( ByteBuffer b : iosg.multiBuffer )
                    b.rewind();
                int k = 0;
                while( k < iosg.blockSize )
                    {
                    k += iosg.scatterReaders[i].read( iosg.multiBuffer );
                    }
                j -= iosg.blockSize;
                }
            // read tail
            if ( ( j > 0 )&&( iosg.multiBufferTail != null ) )
                {
                for ( ByteBuffer b : iosg.multiBufferTail )
                    b.rewind();
                int k = 0;
                while ( k < j )
                    {
                    k += iosg.scatterReaders[i].read( iosg.multiBufferTail );
                    }
                }
            //
            iosg.statistics.sendMBPS
                ( READ_ID, iosg.fileSize, System.nanoTime() );
            // Single file measured read end
            iosg.setSync( i+1, iosg.lastError, READ_ID, IOTASK_NAME );
            }
        //
        iosg.statistics.sendMBPS
            ( TOTAL_READ_ID, iosg.totalSize, System.nanoTime() );
        // All files total measured read cycle end
        }
    catch( IOException e )
        {
        iosg.delete( iosg.pathsSrc, iosg.gatherWriters );
        iosg.delete( iosg.pathsDst, iosg.scatterReaders );
        iosg.lastError = 
            new StatusEntry( false, "Read error: " + e.getMessage() );
        }
    }
}
