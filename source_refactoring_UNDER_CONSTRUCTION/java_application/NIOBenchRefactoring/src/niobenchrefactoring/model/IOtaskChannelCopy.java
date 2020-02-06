/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Copy phase at NIO Channels and Buffers IO scenario.
Single-thread version.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import java.io.IOException;
import java.nio.channels.*;
import static java.nio.file.StandardOpenOption.*;
import static niobenchrefactoring.model.IOscenario.*;

public class IOtaskChannelCopy extends IOtask
{
private final static String IOTASK_NAME = "Copy/ST/NIO channel";

/*
Constructor stores IO scenario object
*/
IOtaskChannelCopy( IOscenarioChannel ios )
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
        // All files total measured copy cycle start
        iosc.statistics.startInterval( TOTAL_COPY_ID, System.nanoTime() );
        //
        for( i=0; i<iosc.fileCount; i++ )
            {
            if ( isInterrupted() || interrupt ) break;
            if ( iosc.fastCopy )
                {
                iosc.channelsSrc[i] = FileChannel.open( iosc.pathsSrc[i] );
                if ( iosc.dataSparse )
                    iosc.channelsDst[i] = FileChannel.open
                        ( iosc.pathsDst[i], CREATE, WRITE, DSYNC, SPARSE );
                else if ( iosc.copySync )
                    iosc.channelsDst[i] = FileChannel.open
                        ( iosc.pathsDst[i], CREATE, WRITE, DSYNC );
                else
                    iosc.channelsDst[i] = FileChannel.open
                        ( iosc.pathsDst[i], CREATE, WRITE );
                int k = 0;
                // Single file measured copy start
                iosc.statistics.startInterval( COPY_ID, System.nanoTime() );
                //
                while( k < iosc.fileSize )
                    {
                    k += iosc.channelsSrc[i].transferTo
                        ( 0, iosc.channelsSrc[i].size(), iosc.channelsDst[i] );
                    }
                if ( iosc.copySync )
                    iosc.channelsDst[i].force( true );
                //
                iosc.statistics.sendMBPS
                    ( COPY_ID, iosc.fileSize, System.nanoTime() );
                // Single file measured copy end
                iosc.setSync( i+1, iosc.lastError, COPY_ID, IOTASK_NAME );
                }
            else
                {
                iosc.channelsSrc[i] = FileChannel.open( iosc.pathsSrc[i] );
                if ( iosc.dataSparse )
                    iosc.channelsDst[i] = FileChannel.open
                        ( iosc.pathsDst[i], CREATE, WRITE, DSYNC, SPARSE );
                else if ( iosc.copySync )
                    iosc.channelsDst[i] = FileChannel.open
                        ( iosc.pathsDst[i], CREATE, WRITE, DSYNC );
                else
                    iosc.channelsDst[i] = FileChannel.open
                        ( iosc.pathsDst[i], CREATE, WRITE );
                int k = 0;
                // Single file measured copy start
                iosc.statistics.startInterval( COPY_ID, System.nanoTime() );
                //
                while( k < iosc.fileSize )
                    {
                    int n = iosc.blockSize;
                    int m = iosc.fileSize - k;
                    if ( n > m ) n = m;
                    k += iosc.channelsSrc[i].transferTo
                        ( 0, n, iosc.channelsDst[i] );
                    if ( iosc.copySync )
                        iosc.channelsDst[i].force( true );
                    }
                //
                iosc.statistics.sendMBPS
                    ( COPY_ID, iosc.fileSize, System.nanoTime() );
                // Single file measured copy end
                iosc.setSync( i+1, iosc.lastError, COPY_ID, IOTASK_NAME );
                }
            }
        //
        iosc.statistics.sendMBPS
            ( TOTAL_COPY_ID, iosc.totalSize, System.nanoTime() );
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
            new StatusEntry( false, "Copy: " + e.getMessage() );
        iosc.setSync( 0, iosc.lastError, COPY_ID, IOTASK_NAME );
        }
    }
}
