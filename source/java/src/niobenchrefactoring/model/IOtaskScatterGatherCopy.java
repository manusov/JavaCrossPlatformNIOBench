/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Copy phase at Java Scatter-Gather file IO scenario.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import static java.nio.file.StandardOpenOption.*;
import static niobenchrefactoring.model.IOscenario.*;

class IOtaskScatterGatherCopy extends IOtask
{
private final static String IOTASK_NAME = "Copy/ST/NIO scatter-gather";

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
            if ( iosg.copySparse )
                iosg.gatherWriters[i] = FileChannel.open
                    ( iosg.pathsDst[i], CREATE, WRITE, DSYNC, SPARSE );
            else if ( iosg.copySync )
                iosg.gatherWriters[i] = FileChannel.open
                    ( iosg.pathsDst[i], CREATE, WRITE, DSYNC );
            else
                iosg.gatherWriters[i] = FileChannel.open
                    ( iosg.pathsDst[i], CREATE, WRITE );
            int j = iosg.fileSize;
            // Single file measured copy start
            iosg.statistics.startInterval( COPY_ID, System.nanoTime() );

            // copy sequence of blocks
            while ( j >= iosg.blockSize )
                {
                // rewind all buffers of multi-buffer
                for ( ByteBuffer b : iosg.multiBuffer )
                    b.rewind();
                // read block
                int k = 0;
                while( k < iosg.blockSize )
                    k += iosg.scatterReaders[i].read( iosg.multiBuffer );
                // rewind all buffers of multi-buffer
                for ( ByteBuffer b : iosg.multiBuffer )
                    b.rewind();
                // write block
                k = 0;
                while( k < iosg.blockSize )
                    k += iosg.gatherWriters[i].write( iosg.multiBuffer );
                j -= iosg.blockSize;
                }
            // copy write tail
            if ( ( j > 0 )&&( iosg.multiBufferTail != null ) )
                {
                // rewind all buffers of multi-buffer tail    
                for ( ByteBuffer b : iosg.multiBufferTail )
                    b.rewind();
                // read block tail
                int k = 0;
                while ( k < j )
                    k += iosg.scatterReaders[i].read( iosg.multiBufferTail );
                // rewind all buffers of multi-buffer tail    
                for ( ByteBuffer b : iosg.multiBufferTail )
                    b.rewind();
                // write block tail
                k = 0;
                while ( k < j )
                    k += iosg.gatherWriters[i].write( iosg.multiBufferTail );
                }
            
            iosg.statistics.sendMBPS
                ( COPY_ID, iosg.fileSize, System.nanoTime() );
            // Single file measured copy end
            iosg.setSync( i+1, iosg.lastError, COPY_ID, IOTASK_NAME );
            
            iosg.scatterReaders[i].close();
            iosg.gatherWriters[i].close();
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
