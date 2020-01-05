/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Copy phase at Native OS API IO scenario.
Version for randomized address access.
Io tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenario.COPY_ID;
import static niobenchrefactoring.model.IOscenario.TOTAL_COPY_ID;
import static niobenchrefactoring.resources.IOPB.transmitStringToIPB;
import static niobenchrefactoring.resources.PAL.IPB_BLOCK_SIZE;
import static niobenchrefactoring.resources.PAL.IPB_DST_ATTRIBUTES;
import static niobenchrefactoring.resources.PAL.IPB_DST_PATH;
import static niobenchrefactoring.resources.PAL.IPB_ITERATIONS;
import static niobenchrefactoring.resources.PAL.IPB_REQUEST_ID;
import static niobenchrefactoring.resources.PAL.IPB_REQUEST_SIZE;
import static niobenchrefactoring.resources.PAL.IPB_SRC_ATTRIBUTES;
import static niobenchrefactoring.resources.PAL.IPB_SRC_PATH;
import static niobenchrefactoring.resources.PAL.MEASURE_COPY_FILE;
import static niobenchrefactoring.resources.PAL.OPB_LAST_ERROR;
import static niobenchrefactoring.resources.PAL.OPB_LAST_OPERATION;
import static niobenchrefactoring.resources.PAL.OPB_TIMER_START;
import static niobenchrefactoring.resources.PAL.OPB_TIMER_STOP;
import static niobenchrefactoring.resources.PAL.WINDOWS_FILE_ATTRIBUTE_NORMAL;
import static niobenchrefactoring.resources.PAL.WINDOWS_FILE_ATTRIBUTE_NO_BUFFERING;
import static niobenchrefactoring.resources.PAL.WINDOWS_FILE_ATTRIBUTE_WRITE_THROUGH;

public class IOtaskNativeLinearCopy extends IOtask
{
private final static String IOTASK_NAME = "Copy/Linear/Native";

/*
Constructor stores IO scenario object
*/
IOtaskNativeLinearCopy( IOscenarioNative ios )
    {
    super( ios );
    }

/*
Run IO task
*/
@Override public void run()
    {
    IOscenarioNative iosn = (IOscenarioNative) ios;
    // All files total measured copy cycle start
    iosn.statistics.startInterval( TOTAL_COPY_ID, System.nanoTime() );
    //
    for( int i=0; i<iosn.fileCount; i++ )
        {
        iosn.ipb[IPB_REQUEST_ID]   = MEASURE_COPY_FILE;
        iosn.ipb[IPB_REQUEST_SIZE] = iosn.fileSize;
        iosn.ipb[IPB_BLOCK_SIZE]   = iosn.blockSize;
        long attributes = WINDOWS_FILE_ATTRIBUTE_NORMAL;
        if ( iosn.copySync )
            {
            attributes += WINDOWS_FILE_ATTRIBUTE_NO_BUFFERING +
                          WINDOWS_FILE_ATTRIBUTE_WRITE_THROUGH;
            }
        iosn.ipb[IPB_SRC_ATTRIBUTES] = attributes;
        iosn.ipb[IPB_DST_ATTRIBUTES] = attributes;
        iosn.ipb[IPB_ITERATIONS] = 5;                                           // !
        transmitStringToIPB( iosn.namesSrc[i], iosn.ipb, IPB_SRC_PATH );
        transmitStringToIPB( iosn.namesDst[i], iosn.ipb, IPB_DST_PATH );
        // call Read File function
        int status = iosn.pal.entryBinary
            ( iosn.ipb, iosn.opb, iosn.ipb.length, iosn.opb.length );
        // Single file measured copy report about start and stop
        iosn.statistics.startInterval
            ( COPY_ID, iosn.opb[OPB_TIMER_START] * (100/5) );                  // !
        iosn.statistics.sendMBPS
            ( COPY_ID, iosn.fileSize, iosn.opb[OPB_TIMER_STOP] * (100/5) );    // !
        // Single file measured copy end
        iosn.setSync( i+1, iosn.lastError, COPY_ID, IOTASK_NAME );
        // check errors
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
        iosn.statistics.
            sendMBPS( TOTAL_COPY_ID, iosn.totalSize, System.nanoTime() );
        // All files total measured copy cycle end
    }
}
