/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Write phase at Java Scatter-Gather file IO scenario.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import static java.nio.file.StandardOpenOption.*;
import static niobenchrefactoring.model.IOscenario.*;

class IOtaskScatterGatherWrite extends IOtask
{
private final static String IOTASK_NAME = "Write/ST/NIO scatter-gather";

/*
Constructor stores IO scenario object
*/
IOtaskScatterGatherWrite( IOscenarioScatterGather ios )
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
        // All files total measured write cycle start
        iosg.statistics.startInterval( TOTAL_WRITE_ID, System.nanoTime() );
        //
        for( int i=0; i<iosg.fileCount; i++ )
            {
            if ( isInterrupted() ) break;
            Files.createFile( iosg.pathsSrc[i] );
            if ( iosg.writeSparse )
                iosg.gatherWriters[i] = FileChannel.open
                    ( iosg.pathsSrc[i], APPEND, DSYNC, SPARSE );
            else if ( iosg.writeSync )
                iosg.gatherWriters[i] = FileChannel.open
                    ( iosg.pathsSrc[i], APPEND, DSYNC );
            else
                iosg.gatherWriters[i] = FileChannel.open
                    ( iosg.pathsSrc[i], APPEND );
            int j = iosg.fileSize;
            // Single file measured write start
            iosg.statistics.startInterval( WRITE_ID, System.nanoTime() );
            // write sequence of blocks
            while ( j >= iosg.blockSize )
                {
                // rewind all buffers of multi-buffer
                for ( ByteBuffer b : iosg.multiBuffer )
                    b.rewind();
                int k = 0;
                while( k < iosg.blockSize )
                    k += iosg.gatherWriters[i].write( iosg.multiBuffer );
                j -= iosg.blockSize;
                }
            // write tail
            if ( ( j > 0 )&&( iosg.multiBufferTail != null ) )
                {
                for ( ByteBuffer b : iosg.multiBufferTail )
                    b.rewind();
                int k = 0;
                while ( k < j )
                    k += iosg.gatherWriters[i].write( iosg.multiBufferTail );
                }
            //
            iosg.statistics.sendMBPS
                ( WRITE_ID, iosg.fileSize, System.nanoTime() );
            // Single file measured write end
            iosg.setSync( i+1, iosg.lastError, WRITE_ID, IOTASK_NAME );
            iosg.gatherWriters[i].close();
            }
        //
        iosg.statistics.
            sendMBPS( TOTAL_WRITE_ID, iosg.totalSize, System.nanoTime() );
        // All files total measured write cycle end
        }
    catch( IOException e )
        {
        iosg.delete( iosg.pathsSrc, iosg.gatherWriters );
        iosg.lastError =
            new StatusEntry( false, "Write error: " + e.getMessage() );
        }
    }
}
