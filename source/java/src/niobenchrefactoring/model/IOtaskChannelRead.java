/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Read phase at NIO Channels and Buffers IO scenario.
Single-thread version.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import static niobenchrefactoring.model.IOscenario.*;

class IOtaskChannelRead extends IOtask
{
private final static String IOTASK_NAME = "Read/ST/NIO channel";

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
private int i = 0;
@Override public void run()
    {
    IOscenarioChannel iosc = (IOscenarioChannel)ios;
    try
        {
        // All files total measured read cycle start
        iosc.statistics.startInterval( TOTAL_READ_ID, System.nanoTime() );
        //
        for( i=0; i<iosc.fileCount; i++ )
            {
            if ( isInterrupted() || interrupt ) break;
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
    
    catch( ClosedByInterruptException e1 )
        {
        try
            {
            if ( iosc.channelsSrc[i] != null ) iosc.channelsSrc[i].close();
            interrupt = true;
            }
        catch ( IOException e2 )
            {
            }
        }
    
    catch( IOException e )
        {
        iosc.delete( iosc.pathsSrc, iosc.channelsSrc );
        iosc.delete( iosc.pathsDst, iosc.channelsDst );
        iosc.lastError = 
            new StatusEntry( false, "Read: " + e.getMessage() );
        iosc.setSync( 0, iosc.lastError, READ_ID, IOTASK_NAME );
        }
    }
}
