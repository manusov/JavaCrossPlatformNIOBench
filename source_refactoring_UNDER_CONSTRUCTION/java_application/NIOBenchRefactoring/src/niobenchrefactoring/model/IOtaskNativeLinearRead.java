/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Read phase at Native OS API IO scenario.
Version for linear access.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenario.READ_ID;
import static niobenchrefactoring.model.IOscenario.TOTAL_READ_ID;
import static niobenchrefactoring.model.IOscenarioNative.RW_GROUP_5;
import static niobenchrefactoring.resources.IOPB.transmitStringToIPB;
import static niobenchrefactoring.resources.PAL.FILE_ATTRIBUTE_BLANK;
import static niobenchrefactoring.resources.PAL.FILE_ATTRIBUTE_READ_SYNC;
import static niobenchrefactoring.resources.PAL.FILE_ATTRIBUTE_WRITE_SYNC;
import static niobenchrefactoring.resources.PAL.IPB_BLOCK_SIZE;
import static niobenchrefactoring.resources.PAL.IPB_ITERATIONS;
import static niobenchrefactoring.resources.PAL.IPB_REQUEST_ID;
import static niobenchrefactoring.resources.PAL.IPB_REQUEST_SIZE;
import static niobenchrefactoring.resources.PAL.IPB_SRC_ATTRIBUTES;
import static niobenchrefactoring.resources.PAL.IPB_SRC_PATH;
import static niobenchrefactoring.resources.PAL.MEASURE_READ_FILE;
import static niobenchrefactoring.resources.PAL.OPB_LAST_ERROR;
import static niobenchrefactoring.resources.PAL.OPB_LAST_OPERATION;
import static niobenchrefactoring.resources.PAL.OPB_TIMER_DELTA;

public class IOtaskNativeLinearRead extends IOtask
{
private final static String IOTASK_NAME = "Read/Linear/Native";
private final static int NATIVE_READ_DEFAULT_REPEATS = 5;
private final int repeats;

/*
Constructor stores IO scenario object
*/
IOtaskNativeLinearRead( IOscenarioNative ios )
    {
    super( ios );
    if ( ios.readWriteMode == RW_GROUP_5 )
        {
        repeats = NATIVE_READ_DEFAULT_REPEATS;
        }
    else
        {
        repeats = 1;
        }
    }

/*
Run IO task = read files group by native method
*/
@Override public void run()
    {
    IOscenarioNative iosn = (IOscenarioNative) ios;
    // All files total measured read cycle start
    iosn.statistics.startInterval( TOTAL_READ_ID, System.nanoTime() );
    //
    for( int i=0; i<iosn.fileCount; i++ )
        {
        if ( isInterrupted() || interrupt ) break;
        //
        iosn.ipb[IPB_REQUEST_ID]   = MEASURE_READ_FILE;
        iosn.ipb[IPB_REQUEST_SIZE] = iosn.fileSize;
        iosn.ipb[IPB_BLOCK_SIZE]   = iosn.blockSize;
        long attributes = FILE_ATTRIBUTE_BLANK;
        if ( iosn.readSync )
            {
            attributes += FILE_ATTRIBUTE_READ_SYNC +
                          FILE_ATTRIBUTE_WRITE_SYNC;
            }
        iosn.ipb[IPB_SRC_ATTRIBUTES] = attributes;
        iosn.ipb[IPB_ITERATIONS] = repeats;
        transmitStringToIPB( iosn.namesSrc[i], iosn.ipb, IPB_SRC_PATH );
        // start measurement time
        iosn.statistics.startInterval( READ_ID, 0 );
        // call Read File function, note unified DTA located just after OPB
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
                    new StatusEntry( false, "OS timer underflow at read"  );
            break;
            }
        // send results if timer valid
        iosn.statistics.sendMBPS( READ_ID, iosn.fileSize, delta );
        // Single file measured read end
        iosn.setSync( i+1, iosn.lastError, READ_ID, IOTASK_NAME );
        // check file I/O errors
        long a = iosn.opb[OPB_LAST_ERROR];
        long b = iosn.opb[OPB_LAST_OPERATION];
        if ( ( status <= 0 )&&( b != 0 ) )
            {
            String s = String.format( "Read error: %d %d %d", status, a, b );
            iosn.lastError = new StatusEntry( false, s  );
            break;
            }
        }
        //
        iosn.statistics.sendMBPS
            ( TOTAL_READ_ID, iosn.totalSize * repeats, System.nanoTime() );
        // All files total measured read cycle end

    }
    
}
