/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Write phase at Native OS API IO scenario.
Version for linear access.
IO tasks is basic components for build IO scenarios.
*/

// TODO. THIS IS PRE BUILD, 100 ns UNITS FOR WINDOWS, 
// REQUIRED CHANGE BECAUSE BETTER PRECISION POSSIBLE FOR LINUX.

// TODO. NUMBER OF MEASUREMENT ITERATIONS MUST BE OPTION.

// TODO. CHECK RETURNED SIZE INSTEAD RE-USE FIXED REPEATS COUNT.

package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenario.TOTAL_WRITE_ID;
import static niobenchrefactoring.model.IOscenario.WRITE_ID;
import static niobenchrefactoring.resources.IOPB.transmitStringToIPB;
import static niobenchrefactoring.resources.PAL.IPB_BLOCK_SIZE;
import static niobenchrefactoring.resources.PAL.IPB_ITERATIONS;
import static niobenchrefactoring.resources.PAL.IPB_REQUEST_ID;
import static niobenchrefactoring.resources.PAL.IPB_REQUEST_SIZE;
import static niobenchrefactoring.resources.PAL.IPB_SRC_ATTRIBUTES;
import static niobenchrefactoring.resources.PAL.IPB_SRC_PATH;
import static niobenchrefactoring.resources.PAL.MEASURE_WRITE_FILE;
import static niobenchrefactoring.resources.PAL.OPB_LAST_ERROR;
import static niobenchrefactoring.resources.PAL.OPB_LAST_OPERATION;
import static niobenchrefactoring.resources.PAL.OPB_TIMER_START;
import static niobenchrefactoring.resources.PAL.OPB_TIMER_STOP;
import static niobenchrefactoring.resources.PAL.WINDOWS_FILE_ATTRIBUTE_NORMAL;
import static niobenchrefactoring.resources.PAL.WINDOWS_FILE_ATTRIBUTE_NO_BUFFERING;
import static niobenchrefactoring.resources.PAL.WINDOWS_FILE_ATTRIBUTE_WRITE_THROUGH;

public class IOtaskNativeLinearWrite extends IOtask
{
private final static String IOTASK_NAME = "Write/Linear/Native";

/*
Constructor stores IO scenario object
*/
IOtaskNativeLinearWrite( IOscenarioNative ios )
    {
    super( ios );
    }

/*
Run IO task
*/
@Override public void run()
    {
    IOscenarioNative iosn = (IOscenarioNative) ios;
    // All files total measured write cycle start
    iosn.statistics.startInterval( TOTAL_WRITE_ID, System.nanoTime() );
    //
    for( int i=0; i<iosn.fileCount; i++ )
        {
        iosn.ipb[IPB_REQUEST_ID]   = MEASURE_WRITE_FILE;
        iosn.ipb[IPB_REQUEST_SIZE] = iosn.fileSize;
        iosn.ipb[IPB_BLOCK_SIZE]   = iosn.blockSize;
        long attributes = WINDOWS_FILE_ATTRIBUTE_NORMAL;
        if ( iosn.writeSync )
            {
            attributes += WINDOWS_FILE_ATTRIBUTE_NO_BUFFERING +
                          WINDOWS_FILE_ATTRIBUTE_WRITE_THROUGH;
            }
        iosn.ipb[IPB_SRC_ATTRIBUTES] = attributes;
        iosn.ipb[IPB_ITERATIONS] = 5;                                           // !
        transmitStringToIPB( iosn.namesSrc[i], iosn.ipb, IPB_SRC_PATH );
        // call Read File function
        int status = iosn.pal.entryBinary
            ( iosn.ipb, iosn.opb, iosn.ipb.length, iosn.opb.length );
        // Single file measured write report about start and stop
        iosn.statistics.startInterval
            ( WRITE_ID, iosn.opb[OPB_TIMER_START] * (100/5) );                  // !
        iosn.statistics.sendMBPS
            ( WRITE_ID, iosn.fileSize, iosn.opb[OPB_TIMER_STOP] * (100/5) );    // !
        // Single file measured write end
        iosn.setSync( i+1, iosn.lastError, WRITE_ID, IOTASK_NAME );
        // check errors
        long a = iosn.opb[OPB_LAST_ERROR];
        long b = iosn.opb[OPB_LAST_OPERATION];
        if ( ( status <= 0 )&&( b != 0 ) )
            {
            String s = String.format( "Write error: %d %d %d", status, a, b );
            iosn.lastError = new StatusEntry( false, s  );
            break;
            }
        }
        //
        iosn.statistics.
            sendMBPS( TOTAL_WRITE_ID, iosn.totalSize, System.nanoTime() );
        // All files total measured write cycle end
    }
}
