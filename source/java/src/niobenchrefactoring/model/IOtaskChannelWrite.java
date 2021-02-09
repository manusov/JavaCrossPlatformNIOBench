/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Write phase at NIO Channels and Buffers IO scenario.
Single-thread version.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import java.io.IOException;
import java.nio.channels.*;
import java.nio.file.Files;
import static java.nio.file.StandardOpenOption.*;
import static niobenchrefactoring.model.IOscenario.*;

class IOtaskChannelWrite extends IOtask
{
private final static String IOTASK_NAME = "Write/ST/NIO channel";

/*
Constructor stores IO scenario object
*/
IOtaskChannelWrite( IOscenarioChannel ios )
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
        // All files total measured write cycle start
        iosc.statistics.startInterval( TOTAL_WRITE_ID, System.nanoTime() );
        //
        for( i=0; i<iosc.fileCount; i++ )
            {
            if ( isInterrupted() || interrupt ) break;
            Files.createFile( iosc.pathsSrc[i] );
            if ( iosc.writeSparse )
                iosc.channelsSrc[i] = FileChannel.open
                    ( iosc.pathsSrc[i], APPEND, DSYNC, SPARSE );
            else if ( iosc.writeSync )
                iosc.channelsSrc[i] = FileChannel.open
                    ( iosc.pathsSrc[i], APPEND, DSYNC );
            else
                iosc.channelsSrc[i] = FileChannel.open
                    ( iosc.pathsSrc[i], APPEND );
            int j = iosc.fileSize;
            // Single file measured write start
            iosc.statistics.startInterval( WRITE_ID, System.nanoTime() );
            //
            while( j >= iosc.blockSize )
                {
                iosc.byteBuffer[0].rewind();
                int k = 0;
                while( k < iosc.blockSize )
                    {
                    k += iosc.channelsSrc[i].write( iosc.byteBuffer[0] );
                    }
                j -= iosc.blockSize;
                }
            if ( j > 0 )
                {
                iosc.byteBufferTail[0].rewind();
                int k = 0;
                while ( k < j )
                    {
                    k += iosc.channelsSrc[i].write( iosc.byteBufferTail[0] );
                    }
                }
            if ( iosc.writeSync )
                iosc.channelsSrc[i].force( true );
            //
            iosc.statistics.sendMBPS
                ( WRITE_ID, iosc.fileSize, System.nanoTime() );
            // Single file measured write end
            iosc.setSync( i+1, iosc.lastError, WRITE_ID, IOTASK_NAME );
            iosc.channelsSrc[i].close();
            }
        //
        iosc.statistics.
            sendMBPS( TOTAL_WRITE_ID, iosc.totalSize, System.nanoTime() );
        // All files total measured write cycle end
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
        iosc.lastError =
            new StatusEntry( false, "Write: " + e.getMessage() );
        iosc.setSync( 0, iosc.lastError, WRITE_ID, IOTASK_NAME );
        }
    }
}
