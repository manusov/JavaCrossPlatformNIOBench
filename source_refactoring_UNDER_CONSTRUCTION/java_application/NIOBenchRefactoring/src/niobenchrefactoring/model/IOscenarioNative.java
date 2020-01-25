/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO scenario class for OS API native file I/O benchmark, include phases: 
write files, copy files, read files.
*/

package niobenchrefactoring.model;

import static niobenchrefactoring.resources.IOPB.transmitStringToIPB;
import niobenchrefactoring.resources.PAL;
import static niobenchrefactoring.resources.PAL.FILE_API_IPB_SIZE;
import static niobenchrefactoring.resources.PAL.FILE_API_OPB_SIZE;
import static niobenchrefactoring.resources.PAL.IPB_REQUEST_ID;
import static niobenchrefactoring.resources.PAL.IPB_SRC_PATH;
import static niobenchrefactoring.resources.PAL.MEASURE_DELETE_FILE;
import static niobenchrefactoring.resources.PAL.OPB_ALIGNMENT_ADDEND;
import static niobenchrefactoring.resources.PAL.OPB_LAST_ERROR;
import static niobenchrefactoring.resources.PAL.OPB_LAST_OPERATION;

public class IOscenarioNative extends IOscenario
{
final PAL pal;
final long[] ipb;
final long[] opb;

/*
Special values for "R/W" option at Native test panel.
*/
public final static int RW_GROUP_5 = 10;
public final static int RW_GROUP_1 = 11;

/*
Default constructor
*/
public IOscenarioNative( PAL pal )
    {
    super();
    this.pal = pal;
    ipb = new long[ FILE_API_IPB_SIZE ];
    opb = new long[ FILE_API_OPB_SIZE + OPB_ALIGNMENT_ADDEND + blockSize / 8 ];
    }

/*
Constructor with parameters
*/
public IOscenarioNative
    ( String pathSrc, String prefixSrc, String postfixSrc,
      String pathDst, String prefixDst, String postfixDst,
      int fileCount, int fileSize, int blockSize, int threadCount,
      boolean readSync, boolean writeSync, boolean copySync, 
      boolean dataSparse, boolean fastCopy,
      int readWriteMode, int addressMode, int dataMode,
      int readDelay, int writeDelay, int copyDelay,
      byte[] dataBlock,
      PAL pal )
    {
    super( pathSrc, prefixSrc, postfixSrc, pathDst, prefixDst, postfixDst,
           fileCount, fileSize, blockSize, threadCount,
           readSync, writeSync, copySync, dataSparse, fastCopy,
           readWriteMode, addressMode, dataMode,
           readDelay, writeDelay, copyDelay, dataBlock );
    this.pal = pal;
    ipb = new long[ FILE_API_IPB_SIZE ];
    opb = new long[ FILE_API_OPB_SIZE + OPB_ALIGNMENT_ADDEND + blockSize / 8 ];
    }

/*
Run performance scenario    
*/
@Override public void run()
    {
    if ( addressMode == ADDRESS_SEQUENTAL )
        {  // initialize tasks for sequental address branch
        iotWrite = new IOtaskNativeLinearWrite( this );
        iotCopy  = new IOtaskNativeLinearCopy( this );
        iotRead  = new IOtaskNativeLinearRead( this );
        }
    else
        {  // initialize tasks for randomized address branch
        iotWrite = new IOtaskNativeRandomWrite( this );
        iotCopy  = new IOtaskNativeRandomCopy( this );
        iotRead  = new IOtaskNativeRandomRead( this );
        }

/*    
    if ( ( readWriteMode != READ_ONLY ) && errorCheck() )
        {
        preDelay( writeDelay, WRITE_DELAY_NAME );
        threadHelper( iotWrite );
        }
    
    if ( ( readWriteMode != READ_ONLY ) && errorCheck() )
        {
        preDelay( copyDelay, COPY_DELAY_NAME );
        threadHelper( iotCopy );
        }
    
    if ( ( readWriteMode != WRITE_ONLY ) && errorCheck() )
        {
        preDelay( readDelay, READ_DELAY_NAME );
        threadHelper( iotRead );
        }
*/

    if ( ( readWriteMode != READ_ONLY ) &&
         ( ! interrupt ) && ( ! isInterrupted() ) && errorCheck() )
        {
        preDelay( writeDelay, WRITE_DELAY_NAME );
        threadHelper( iotWrite );
        }
    
    if ( ( readWriteMode != READ_ONLY ) && 
         ( ! interrupt ) && ( ! isInterrupted() ) && errorCheck() )
        {
        preDelay( copyDelay, COPY_DELAY_NAME );
        threadHelper( iotCopy );
        }
    
    if ( ( readWriteMode != WRITE_ONLY ) && 
         ( ! interrupt ) && ( ! isInterrupted() ) && errorCheck() )
        {
        preDelay( readDelay, READ_DELAY_NAME );
        threadHelper( iotRead );
        }

    if ( ( readWriteMode != READ_ONLY ) || ( readWriteMode != WRITE_ONLY ) )
        {
        setSync( 0, lastError, DELETE_ID, DELETE_NAME );
        /*
        Phase = Delete, note about files not deleted in WRITE_ONLY mode.
        Note delete operation cycles for all files is not interruptable.
        */
        // delete( pathsSrc, channelsSrc );
        // delete( pathsDst, channelsDst );
        
        deleteNative( namesSrc );
        deleteNative( namesDst );
        
        }
    }

void deleteNative( String[] names )
    {
    ipb[IPB_REQUEST_ID] = MEASURE_DELETE_FILE;
    for ( String name : names )
        {
        transmitStringToIPB( name, ipb, IPB_SRC_PATH );
        int status = pal.entryBinary( ipb, opb, ipb.length, opb.length );
        long a = opb[OPB_LAST_ERROR];
        long b = opb[OPB_LAST_OPERATION];
        if ( ( status <= 0 )&&( b != 0 ) )
            {
            String s = String.format( "Delete error: %d %d %d", status, a, b );
            lastError = new StatusEntry( false, s  );
            }
        }
    }
}
