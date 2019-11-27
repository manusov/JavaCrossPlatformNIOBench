package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenario.TOTAL_WRITE_ID;
import static niobenchrefactoring.model.IOscenario.WRITE_ID;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.DSYNC;
import static java.nio.file.StandardOpenOption.SPARSE;

public class IOtaskScatterGatherWrite extends IOtask
{
private final static String IOTASK_NAME = 
    "NIO scatter-gather MBPS, Write";

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
            if ( iosg.dataSparse )
                iosg.gatherWriters[i] = FileChannel.open
                    ( iosg.pathsSrc[i], APPEND, DSYNC, SPARSE );
            else if ( iosg.writeSync )
                iosg.gatherWriters[i] = FileChannel.open
                    ( iosg.pathsSrc[i], APPEND, DSYNC );
            else
                iosg.gatherWriters[i] = FileChannel.open
                    ( iosg.pathsSrc[i], APPEND );
            // rewind all buffers of multi-buffer
            for ( ByteBuffer b : iosg.multiBuffer ) 
                b.rewind();
            // Single file measured write start
            iosg.statistics.startInterval( WRITE_ID, System.nanoTime() );
            //
            iosg.gatherWriters[i].write( iosg.multiBuffer );
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
