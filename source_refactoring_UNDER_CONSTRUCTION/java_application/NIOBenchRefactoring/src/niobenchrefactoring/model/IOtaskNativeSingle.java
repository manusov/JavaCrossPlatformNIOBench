/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Write/Copy/Read as single phase at 
Legacy Native OS API IO scenario.
Version for linear access.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenario.COPY_ID;
import static niobenchrefactoring.model.IOscenario.READ_ID;
import static niobenchrefactoring.model.IOscenario.TOTAL_COPY_ID;
import static niobenchrefactoring.model.IOscenario.TOTAL_READ_ID;
import static niobenchrefactoring.model.IOscenario.TOTAL_WRITE_ID;
import static niobenchrefactoring.model.IOscenario.WRITE_ID;
import static niobenchrefactoring.model.IOscenarioNative.RW_SINGLE_5;
import static niobenchrefactoring.resources.IOPB.transmitStringToIPB;
import static niobenchrefactoring.resources.PAL.FILE_ATTRIBUTE_BLANK;
import static niobenchrefactoring.resources.PAL.FILE_ATTRIBUTE_READ_SYNC;
import static niobenchrefactoring.resources.PAL.FILE_ATTRIBUTE_WRITE_SYNC;
import static niobenchrefactoring.resources.PAL.IPB_BLOCK_SIZE;
import static niobenchrefactoring.resources.PAL.IPB_DST_ATTRIBUTES;
import static niobenchrefactoring.resources.PAL.IPB_DST_PATH_POSTFIX;
import static niobenchrefactoring.resources.PAL.IPB_DST_PATH_PREFIX;
import static niobenchrefactoring.resources.PAL.IPB_FILE_COUNT;
import static niobenchrefactoring.resources.PAL.IPB_ITERATIONS;
import static niobenchrefactoring.resources.PAL.IPB_REQUEST_ID;
import static niobenchrefactoring.resources.PAL.IPB_REQUEST_SIZE;
import static niobenchrefactoring.resources.PAL.IPB_SRC_ATTRIBUTES;
import static niobenchrefactoring.resources.PAL.IPB_SRC_PATH_POSTFIX;
import static niobenchrefactoring.resources.PAL.IPB_SRC_PATH_PREFIX;
import static niobenchrefactoring.resources.PAL.OPB_LAST_ERROR;
import static niobenchrefactoring.resources.PAL.OPB_LAST_OPERATION;
import static niobenchrefactoring.resources.PAL.OPB_TIMER_COPY;
import static niobenchrefactoring.resources.PAL.OPB_TIMER_READ;
import static niobenchrefactoring.resources.PAL.OPB_TIMER_WRITE;
import static niobenchrefactoring.resources.PAL.PRECISION_LINEAR;

public class IOtaskNativeSingle extends IOtask
{
private final static String IOTASK_NAME = "Single/Native";
private final static int NATIVE_SINGLE_DEFAULT_REPEATS = 5;
private final int repeats;

/*
Constructor stores IO scenario object
*/
IOtaskNativeSingle( IOscenarioNative ios )
    {
    super( ios );
    if ( ios.readWriteMode == RW_SINGLE_5 )
        {
        repeats = NATIVE_SINGLE_DEFAULT_REPEATS;
        }
    else
        {
        repeats = 1;
        }
    }

/*
Run IO task = write/copy/read files group by single call of native method
*/
@Override public void run()
    {
    IOscenarioNative iosn = (IOscenarioNative) ios;
    long time = System.nanoTime();
    // mark start integral time
    iosn.statistics.startInterval( TOTAL_WRITE_ID , time );
    iosn.statistics.startInterval( TOTAL_COPY_ID  , time );
    iosn.statistics.startInterval( TOTAL_READ_ID  , time );
    
    // initializing parameters same for all iterations
    iosn.ipb[IPB_REQUEST_ID]   = PRECISION_LINEAR;
    iosn.ipb[IPB_REQUEST_SIZE] = iosn.fileSize;
    iosn.ipb[IPB_BLOCK_SIZE]   = iosn.blockSize;
    iosn.ipb[IPB_FILE_COUNT]   = 1;  // external iterations, single file
    iosn.ipb[IPB_ITERATIONS]   = repeats;
    long srcAttributes = FILE_ATTRIBUTE_BLANK;
    long dstAttributes = FILE_ATTRIBUTE_BLANK;
    if ( ( iosn.writeSync )||( iosn.readSync ) )
        {
        srcAttributes += FILE_ATTRIBUTE_READ_SYNC +
                         FILE_ATTRIBUTE_WRITE_SYNC;
        }
    if ( iosn.copySync )
        {
        dstAttributes += FILE_ATTRIBUTE_READ_SYNC +
                         FILE_ATTRIBUTE_WRITE_SYNC;
        }
    iosn.ipb[IPB_SRC_ATTRIBUTES] = srcAttributes;
    iosn.ipb[IPB_DST_ATTRIBUTES] = dstAttributes;
        
    for( int i=0; i<iosn.fileCount; i++ )
        {
        if ( isInterrupted() || interrupt ) break;
        // initializing per-iteration names
        String[] srcPP = nameHelper ( iosn.namesSrc[i] );
        String[] dstPP = nameHelper ( iosn.namesDst[i] );
        transmitStringToIPB( srcPP[0], iosn.ipb, IPB_SRC_PATH_PREFIX  );
        transmitStringToIPB( srcPP[1], iosn.ipb, IPB_SRC_PATH_POSTFIX );
        transmitStringToIPB( dstPP[0], iosn.ipb, IPB_DST_PATH_PREFIX  );
        transmitStringToIPB( dstPP[1], iosn.ipb, IPB_DST_PATH_POSTFIX );
        // mark start time
        iosn.statistics.startInterval( WRITE_ID, 0 );
        iosn.statistics.startInterval( COPY_ID, 0 );
        iosn.statistics.startInterval( READ_ID, 0 );
        
        // call native function: Write/Copy/Read as single call
        int status = iosn.pal.entryBinary
            ( iosn.ipb, iosn.opb, iosn.ipb.length, iosn.opb.length );
        
        // get timings from native call results, calculate MBPS
        double tiw = iosn.opb[OPB_TIMER_WRITE];
        double tic = iosn.opb[OPB_TIMER_COPY];
        double tir = iosn.opb[OPB_TIMER_READ];
        long deltaTiw = (long)( tiw / (double)repeats );
        long deltaTic = (long)( tic / (double)repeats );
        long deltaTir = (long)( tir / (double)repeats );
        // check timer underflow
        if ( deltaTiw <= 0 )
            {
            iosn.lastError = 
                    new StatusEntry( false, "OS timer underflow at write"  );
            break;
            }
        if ( deltaTic <= 0 )
            {
            iosn.lastError = 
                    new StatusEntry( false, "OS timer underflow at copy"  );
            break;
            }
        if ( deltaTir <= 0 )
            {
            iosn.lastError = 
                    new StatusEntry( false, "OS timer underflow at read"  );
            break;
            }
        // mark end time, store write, copy, read timings 
        iosn.statistics.sendMBPS( WRITE_ID, iosn.fileSize, deltaTiw );
        iosn.statistics.sendMBPS( COPY_ID,  iosn.fileSize, deltaTic );
        iosn.statistics.sendMBPS( READ_ID,  iosn.fileSize, deltaTir );
        
        // mark test progress
        iosn.setSync( i+1, iosn.lastError, WRITE_ID, IOTASK_NAME );
        iosn.setSync( i+1, iosn.lastError, COPY_ID,  IOTASK_NAME );
        iosn.setSync( i+1, iosn.lastError, READ_ID,  IOTASK_NAME );
        // check status-signaled errors
        long a = iosn.opb[OPB_LAST_ERROR];
        long b = iosn.opb[OPB_LAST_OPERATION];
        if ( ( status <= 0 )&&( b != 0 ) )
            {
            String s = String.format( "IO error: %d %d %d", status, a, b );
            iosn.lastError = new StatusEntry( false, s  );
            break;
            }
        }

    // for this scenario, integral speed same for write, copy, read
    long size = iosn.totalSize * repeats;
    time = System.nanoTime();
    // mark end integral time
    iosn.statistics.sendMBPS( TOTAL_WRITE_ID , size , time );
    iosn.statistics.sendMBPS( TOTAL_COPY_ID  , size , time );
    iosn.statistics.sendMBPS( TOTAL_READ_ID  , size , time );
    }

/*
Helpers
*/

private String[] nameHelper( String s1 )
    {
    String prefix  = s1;
    String postfix = "";
    int k = s1.lastIndexOf( '.' );
    if ( k > 0 )
        {
        prefix  = s1.substring( 0, k );
        postfix = s1.substring( k );
        }
    return new String[] { prefix, postfix };
    }

}
