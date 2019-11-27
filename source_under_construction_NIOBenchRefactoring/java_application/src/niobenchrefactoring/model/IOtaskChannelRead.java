package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenario.READ_ID;
import static niobenchrefactoring.model.IOscenario.TOTAL_READ_ID;
import java.io.IOException;

public class IOtaskChannelRead extends IOtask
{
private final static String IOTASK_NAME = 
    "NIO channel single thread MBPS, Read";

/*
Constructor stores IO scenario object
*/
IOtaskChannelRead( IOscenarioChannel ios )
    {
    super( ios );
    }

/*
Run IO task
*/
@Override public void run()
    {
    IOscenarioChannel iosc = (IOscenarioChannel)ios;
    try
        {
        // All files total measured read cycle start
        iosc.statistics.startInterval( TOTAL_READ_ID, System.nanoTime() );
        //
        for( int i=0; i<iosc.fileCount; i++ )
            {
            if ( isInterrupted() ) break;
            int k;
            // Single file measured read start
            iosc.statistics.startInterval( READ_ID, System.nanoTime() );
            //
            do  {
                iosc.byteBuffer[0].rewind();
                k = iosc.channelsSrc[i].read( iosc.byteBuffer[0] );
                } while ( k != -1);
            //
            iosc.statistics.sendMBPS
                ( READ_ID, iosc.fileSize, System.nanoTime() );
            // Single file measured read end
            iosc.setSync( i+1, iosc.lastError, READ_ID, IOTASK_NAME );
            }
        //
        iosc.statistics.sendMBPS
            ( TOTAL_READ_ID, iosc.totalSize, System.nanoTime() );
        // All files total measured copy cycle end
        }
    catch( IOException e )
        {
        iosc.delete( iosc.pathsSrc, iosc.channelsSrc );
        iosc.delete( iosc.pathsDst, iosc.channelsDst );
        iosc.lastError = 
            new StatusEntry( false, "Read error: " + e.getMessage() );
        }
    }
}
