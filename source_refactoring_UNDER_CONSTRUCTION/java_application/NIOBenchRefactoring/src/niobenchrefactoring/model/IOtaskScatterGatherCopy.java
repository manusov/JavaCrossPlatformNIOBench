/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Copy phase at Java Scatter-Gather file IO scenario.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenario.COPY_ID;
import static niobenchrefactoring.model.IOscenario.TOTAL_COPY_ID;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.DSYNC;
import static java.nio.file.StandardOpenOption.SPARSE;
import static java.nio.file.StandardOpenOption.WRITE;

public class IOtaskScatterGatherCopy extends IOtask
{
private final static String IOTASK_NAME = 
    "NIO scatter-gather MBPS, Copy";

/*
Constructor stores IO scenario object
*/
IOtaskScatterGatherCopy( IOscenarioScatterGather ios )
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
        // All files total measured copy cycle start
        iosg.statistics.startInterval( TOTAL_COPY_ID, System.nanoTime() );
        //
        for( int i=0; i<iosg.fileCount; i++ )
            {
            if ( isInterrupted() ) break;
            iosg.scatterReaders[i] = FileChannel.open( iosg.pathsSrc[i] );
            if ( iosg.dataSparse )
                iosg.gatherWriters[i] = FileChannel.open
                    ( iosg.pathsDst[i], CREATE, WRITE, DSYNC, SPARSE );
            else if ( iosg.copySync )
                iosg.gatherWriters[i] = FileChannel.open
                    ( iosg.pathsDst[i], CREATE, WRITE, DSYNC );
            else
                iosg.gatherWriters[i] = FileChannel.open
                    ( iosg.pathsDst[i], CREATE, WRITE );
            // Single file measured copy start
            iosg.statistics.startInterval( COPY_ID, System.nanoTime() );
            //
            // rewind all buffers of multi-buffer and read to multi-buffer
            for ( ByteBuffer b : iosg.multiBuffer ) 
                b.rewind();
            iosg.scatterReaders[i].read( iosg.multiBuffer );
            // rewind all buffers of multi-buffer and write from multi-buf.
            for ( ByteBuffer b : iosg.multiBuffer ) 
                b.rewind();
            iosg.gatherWriters[i].write( iosg.multiBuffer );
            //
            iosg.statistics.sendMBPS
                ( COPY_ID, iosg.fileSize, System.nanoTime() );
            // Single file measured copy end
            iosg.setSync( i+1, iosg.lastError, COPY_ID, IOTASK_NAME );
            }
        //
        iosg.statistics.sendMBPS
            ( TOTAL_COPY_ID, iosg.totalSize, System.nanoTime() );
        // All files total measured copy cycle end
        }
    catch( IOException e )
        {
        iosg.delete( iosg.pathsSrc, iosg.gatherWriters );
        iosg.delete( iosg.pathsDst, iosg.scatterReaders );
        iosg.lastError = 
            new StatusEntry( false, "Copy error: " + e.getMessage() );
        }
    }
}
