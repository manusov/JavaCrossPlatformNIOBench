/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Copy phase at Native OS API IO scenario.
Version for randomized address access.
Io tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenarioNative.RW_GROUP_5;
import static niobenchrefactoring.resources.IOPB.transmitStringToIPB;
import static niobenchrefactoring.model.IOscenario.*;
import static niobenchrefactoring.resources.PAL.*;

class IOtaskNativeLinearCopy extends IOtask
{
private final static String IOTASK_NAME = "Copy/Linear/Native";
private final static int NATIVE_COPY_DEFAULT_REPEATS = 5;
private final int repeats;

/*
Constructor stores IO scenario object
*/
IOtaskNativeLinearCopy( IOscenarioNative ios )
    {
    super( ios );
    if ( ios.readWriteMode == RW_GROUP_5 )
        {
        repeats = NATIVE_COPY_DEFAULT_REPEATS;
        }
    else
        {
        repeats = 1;
        }
    }

/*
Run IO task = copy files group by native method
*/
@Override public void run()
    {
    IOscenarioNative iosn = (IOscenarioNative) ios;
    // All files total measured copy cycle start
    iosn.statistics.startInterval( TOTAL_COPY_ID, System.nanoTime() );
    //
    for( int i=0; i<iosn.fileCount; i++ )
        {
        if ( isInterrupted() || interrupt ) break;
        //
        iosn.ipb[IPB_REQUEST_ID]   = MEASURE_COPY_FILE;
        iosn.ipb[IPB_REQUEST_SIZE] = iosn.fileSize;
        iosn.ipb[IPB_BLOCK_SIZE]   = iosn.blockSize;
        long attributes = FILE_ATTRIBUTE_BLANK;
        if ( iosn.copySync )
            {
            attributes += FILE_ATTRIBUTE_READ_SYNC +
                          FILE_ATTRIBUTE_WRITE_SYNC;
            }
        iosn.ipb[IPB_SRC_ATTRIBUTES] = attributes;
        iosn.ipb[IPB_DST_ATTRIBUTES] = attributes;
        iosn.ipb[IPB_ITERATIONS] = repeats;
        transmitStringToIPB( iosn.namesSrc[i], iosn.ipb, IPB_SRC_PATH );
        transmitStringToIPB( iosn.namesDst[i], iosn.ipb, IPB_DST_PATH );
        // start measurement time
        iosn.statistics.startInterval( COPY_ID, 0 );
        // call Copy File function, note unified DTA located just after OPB
        int status = iosn.pal.entryBinary
            ( iosn.ipb, iosn.opb, iosn.ipb.length, iosn.opb.length );
        // Single file measured copy report about start and stop
        // TODO. USE opb[OPB_OPERATION_SIZE] returned by native code
        double nanoseconds = iosn.opb[OPB_TIMER_DELTA];
        long delta = (long)( nanoseconds / (double)repeats );
        // detect time measurement errors
        if ( delta <= 0 )
            {
            iosn.lastError = 
                    new StatusEntry( false, "OS timer underflow at copy"  );
            break;
            }
        // send results if timer valid        
        iosn.statistics.sendMBPS( COPY_ID, iosn.fileSize, delta );
        // Single file measured copy end
        iosn.setSync( i+1, iosn.lastError, COPY_ID, IOTASK_NAME );
        // check file I/O errors
        long a = iosn.opb[OPB_LAST_ERROR];
        long b = iosn.opb[OPB_LAST_OPERATION];
        if ( ( status <= 0 )&&( b != 0 ) )
            {
            String s = String.format( "Copy error: %d %d %d", status, a, b );
            iosn.lastError = new StatusEntry( false, s  );
            break;
            }
        }
        //
        iosn.statistics.sendMBPS
            ( TOTAL_COPY_ID, iosn.totalSize * repeats, System.nanoTime() );
        // All files total measured copy cycle end
    }
}
