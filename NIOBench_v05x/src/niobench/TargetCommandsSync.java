/*

NIOBench, file I/O Benchmark utility. (C)2018 IC Book Labs
NIO Benchmarks target operation, child class for sync. write and copy mode.

*/

package niobench;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import static java.nio.file.StandardOpenOption.*;

public class TargetCommandsSync extends TargetCommands
{
// Thread execution
@Override public void run()
    {
    // Initializing file IO channels
    errorCode = 0;        
    srcPaths = new Path[fileCount];
    dstPaths = new Path[fileCount];
    in = new FileChannel[fileCount];
    out = new FileChannel[fileCount];
    fs = FileSystems.getDefault();
    // direct buffer used, for minimize memory operations delays
    mBuf = ByteBuffer.allocateDirect(BLOCK_SIZE);
    mBuf.put(data, 0, BLOCK_SIZE);

    // Create and write source files
    // STEP 1 = WRITE
    try {
        for ( int i=0; i<fileCount; i++ )
            {
            if (interrupt) break;
            srcPaths[i] = fs.getPath( filePath + SRC_NAME + i + EXTENSION );
            Files.createFile( srcPaths[i] );
            in[i] = FileChannel.open( srcPaths[i] , APPEND , DSYNC );
            // start timings
            ns1 = System.nanoTime();
            for(int j=0; j<fileSize; j++)
                {
                mBuf.rewind();
                in[i].write(mBuf);          // 1=number of buffers, not bytes
                }
            ns2 = System.nanoTime();
            nanosecondsWrite[i] = ns2 - ns1;
            // end timings
            in[i].close();
            counter++;
            }
        } catch ( Exception e )
            {
            deleteSrc();
            errorCode=1;
            flag=false;
            }

    // Copy source files to destination files
    // STEP 2 = COPY
    try {
        for( int i=0; i<fileCount; i++ )
            {
            if (interrupt) break;
            // start timings
            ns1 = System.nanoTime();
            in[i] = FileChannel.open( srcPaths[i] );  // Re-open, now closed
            dstPaths[i] = fs.getPath( filePath + DST_NAME + i + EXTENSION );
            out[i] = FileChannel.open( dstPaths[i] , CREATE , WRITE , DSYNC );
            in[i].transferTo( 0, in[i].size(), out[i] );
            out[i].force( true );
            ns2 = System.nanoTime();
            nanosecondsCopy[i] = ns2 - ns1;
            // end timings
            counter++;
            }
        } catch ( Exception e )
            { 
            deleteSrc();
            deleteDst();
            errorCode=2;
            flag=false;
            }

    // Read source files
    // STEP 3 = READ    
    int count;
    try {
        for( int i=0; i<fileCount; i++ )
            {
            if (interrupt) break;
            // start timings
            ns1 = System.nanoTime();
            do {
               mBuf.rewind();
               count = in[i].read(mBuf);  
               } while (count != -1);
            ns2 = System.nanoTime();
            nanosecondsRead[i] = ns2 - ns1;
            // end timings
            counter++;
            }
        } catch ( Exception e1 )
            { 
            deleteSrc();
            deleteDst();
            errorCode=3;
            flag=false;
            }

    // Delete required number of source files
    deleteSrc();

    // Delete required number of destination files
    deleteDst();
    
    // End thread execution
    flag = false;
    }

// Helpers methods
// Note methods corrected, 
// for delete next file even if delete current file failed.
// helper method: delete source files
private void deleteSrc()
    {
    for( int i=0; i<fileCount; i++ )
        {
        try {
            Files.delete( srcPaths[i] );
            in[i].close();
            } catch ( Exception e ) { }
        }

    }
// helper method: delete destination files
private void deleteDst()
    {
    for( int i=0; i<fileCount; i++ )
        {
        try {
            Files.delete( dstPaths[i] );
            out[i].close();
            } catch ( Exception e ) { }
        }
    }

}
