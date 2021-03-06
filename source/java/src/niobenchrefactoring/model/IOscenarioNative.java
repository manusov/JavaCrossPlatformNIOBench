/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO scenario class for OS API native file I/O benchmark, include phases: 
write files, copy files, read files.
*/

package niobenchrefactoring.model;

import niobenchrefactoring.resources.IOPB;
import static niobenchrefactoring.resources.IOPB.transmitStringToIPB;
import niobenchrefactoring.resources.PAL;
import static niobenchrefactoring.resources.PAL.*;

public class IOscenarioNative extends IOscenario
{
final PAL pal;
final long[] ipb;
final long[] opb;
/*
Special values for "R/W" option at Native test panel.
*/
public final static int RW_GROUP_5  = 10;
public final static int RW_GROUP_1  = 11;
public final static int RW_SINGLE_5 = 12;
public final static int RW_SINGLE_1 = 13;

/*
Constructor with parameters
*/
public IOscenarioNative
    ( String pathSrc, String prefixSrc, String postfixSrc,
      String pathDst, String prefixDst, String postfixDst,
      int fileCount, int fileSize, int blockSize, int threadCount,
      boolean readSync, boolean writeSync, boolean copySync, 
      boolean writeSyncSparse, boolean copySyncSparse,
      boolean fastCopy,
      int readWriteMode, int addressMode, int dataMode,
      int readDelay, int writeDelay, int copyDelay,
      byte[] dataBlock,
      PAL pal )
    {
    super( pathSrc, prefixSrc, postfixSrc, pathDst, prefixDst, postfixDst,
           fileCount, fileSize, blockSize, threadCount,
           readSync, writeSync, copySync, writeSyncSparse, copySyncSparse,
           fastCopy,
           readWriteMode, addressMode, dataMode,
           readDelay, writeDelay, copyDelay, dataBlock );
    this.pal = pal;
    /*
    Pre-check memory size for prevent out-of-memory exception    
    */
    Runtime r = Runtime.getRuntime();
    long max = r.maxMemory();
    if ( ( (long)blockSize * (long)threadCount * 2 ) <= max )
        {
        /*
        This IPB, OPB use unified DTA location for all tasks: Write, Copy, Read,
        */
        ipb = new long[ FILE_API_IPB_SIZE ];
        opb = new long[ FILE_API_OPB_SIZE + 
                        OPB_ALIGNMENT_ADDEND + blockSize / 8 ];
        /*
        initializing data (OPB + DTA),
        use same unified DTA location method: DTA base = OPB base + OPB size
        DTA located just after OPB for write, read, copy,
        otherwise, if use IPB for Write data and OPB for Read data,
        required both big IPB (Write) and big OPB (read) = this alternative
        is non optimal for memory size usage and OS paging performance.
        */
        int offset = FILE_API_OPB_SIZE;
        int length = OPB_ALIGNMENT_ADDEND;
        int srcLength = dataBlock.length;   // size of existed array, bytes
        int dstLength = length * 8;         // required for alignment, bytes
        if ( srcLength < dstLength )
            {  // support block size insufficient for page alignment
            byte[] data = new byte[dstLength];
            int duplicates = dstLength / srcLength;
            int i = 0;
            for( int j=0; j<duplicates; j++ )
                {
                System.arraycopy( dataBlock, 0, data, i, srcLength );
                i += srcLength;
                }
            IOPB.transmitBytesToDTA( opb, data, offset, length );
            }
        else
            {  // support block size sufficient for page alignment
            IOPB.transmitBytesToDTA( opb, dataBlock, offset, length );
            }
        // alignment part done, fill main part of DTA
        offset = FILE_API_OPB_SIZE + OPB_ALIGNMENT_ADDEND;
        length = blockSize / 8;
        IOPB.transmitBytesToDTA( opb, dataBlock, offset, length );
        }
    else
        {  // insufficient memory reporting
        lastError = new StatusEntry
            ( false, "Memory limit error, change block or threads" );
        ipb = null;
        opb = null;
        }
    }

/*
Run performance scenario    
*/
@Override public void run()
    {
    if ( ! errorCheck() )
        {  // this used for exit by memory overflow pre-detection
        setSync( 0, lastError, 0, "N/A" );
        return;
        }
    
    if ( ( readWriteMode == RW_SINGLE_5 ) || ( readWriteMode == RW_SINGLE_1 ) )
        {  // single file mode
        iotWrite = new IOtaskNativeSingle( this );
        threadHelper( iotWrite );
        if ( ! lastError.flag )
            {
            setSync( 0, lastError, 0, "N/A" );
            }
        }
    
    else if ( ( readWriteMode == RW_GROUP_5 ) || 
              ( readWriteMode == RW_GROUP_1 ) )
        {  // file group mode, initializing test scenario
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

        // file group mode, execute test scenario
        if ( ( readWriteMode != READ_ONLY ) &&
             ( ! interrupt ) && ( ! isInterrupted() ) && errorCheck() )
            {
            if ( preDelay( writeDelay, WRITE_DELAY_NAME ) )
                {
                threadHelper( iotWrite );
                }
            }
    
        if ( ( readWriteMode != READ_ONLY ) && 
             ( ! interrupt ) && ( ! isInterrupted() ) && errorCheck() )
            {
            if ( preDelay( copyDelay, COPY_DELAY_NAME ) )
                {
                threadHelper( iotCopy );
                }
            }
    
        if ( ( readWriteMode != WRITE_ONLY ) && 
             ( ! interrupt ) && ( ! isInterrupted() ) && errorCheck() )
            {
            if ( preDelay( readDelay, READ_DELAY_NAME ) )
                {
                threadHelper( iotRead );
                }
            }

        if ( ( readWriteMode != READ_ONLY ) || ( readWriteMode != WRITE_ONLY ) )
            {
            setSync( 0, lastError, DELETE_ID, DELETE_NAME );
            /*
            Phase = Delete, note about files not deleted in WRITE_ONLY mode.
            Note delete operation cycles for all files is not interruptable.
            */
            deleteNative( namesSrc );
            deleteNative( namesDst );
            }
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
