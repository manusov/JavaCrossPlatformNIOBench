package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenario.READ_ID;
import static niobenchrefactoring.model.IOscenario.TOTAL_READ_ID;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class IOtaskScatterGatherRead extends IOtask
{
private final static String IOTASK_NAME = 
    "NIO scatter-gather MBPS, Read";

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
            // Single file measured read start
            iosg.statistics.startInterval( READ_ID, System.nanoTime() );
            //
            // rewind all buffers of multi-buffer and read to multi-buffer
            for ( ByteBuffer b : iosg.multiBuffer ) 
                b.rewind();
            iosg.scatterReaders[i].read( iosg.multiBuffer );
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
