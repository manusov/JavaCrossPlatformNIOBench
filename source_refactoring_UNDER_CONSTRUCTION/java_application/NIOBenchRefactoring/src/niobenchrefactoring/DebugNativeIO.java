package niobenchrefactoring;

import static niobenchrefactoring.resources.IOPB.transmitStringToIPB;
import niobenchrefactoring.resources.PAL;
import static niobenchrefactoring.resources.PAL.*;

public class DebugNativeIO
{
private final String SRC_FILE_NAME = "C:\\TEMP\\a1.bin";
private final String DST_FILE_NAME = "C:\\TEMP\\a2.bin";
private final PAL pal;
private boolean binaryValid = false;

DebugNativeIO()
    {
    pal = new PAL();
    int loadStatus = pal.loadUserModeLibrary();
    binaryValid = false;
    int binaryType = -1;
    int palWidth = -1;
    if ( loadStatus==0 )
        {
        try 
            {
            binaryValid = pal.getBinaryValid();
            binaryType = pal.getBinaryType();
            palWidth = pal.checkBinary();
            }
        catch ( Exception e )
            {
            palWidth = -2; 
            }
        catch ( UnsatisfiedLinkError e ) 
            { 
            palWidth = -3; 
            }
        }
    String s = String.format( "PAL init status: %b, %d, %d, %d.", 
                              binaryValid, loadStatus, binaryType, palWidth );
    System.out.println( s );
    }
    
void testRead()
    {
    if ( ! binaryValid )
        {
        System.out.println( "Native read test skipped." );
        }
    else
        {
        System.out.println( "Native read test." );
        // send input parameters for Read File function
        int fileSize  = 100 * 1024 * 1024;
        int blockSize = 1024 * 1024;
        String fileName = SRC_FILE_NAME;
        int ipbSize = FILE_API_IPB_SIZE;
        int opbSize = FILE_API_OPB_SIZE + OPB_ALIGNMENT_ADDEND + blockSize / 8;
        long[] ipb = new long[ipbSize];
        long[] opb = new long[opbSize];
        ipb[IPB_REQUEST_ID]     = MEASURE_READ_FILE;
        ipb[IPB_REQUEST_SIZE]   = fileSize;
        ipb[IPB_BLOCK_SIZE]     = blockSize;
        ipb[IPB_SRC_ATTRIBUTES] = WINDOWS_FILE_ATTRIBUTE_NORMAL +
                                  WINDOWS_FILE_ATTRIBUTE_NO_BUFFERING +
                                  WINDOWS_FILE_ATTRIBUTE_WRITE_THROUGH;
        ipb[IPB_ITERATIONS] = 5;
        transmitStringToIPB( fileName, ipb, IPB_SRC_PATH );
        // call Read File function
        int status = pal.entryBinary( ipb, opb, ipbSize, opbSize );
        // Receive and interpreting output parameters after Read File function
        String s;
        long a = opb[OPB_LAST_ERROR];
        if ( ( a == 0 )&&( status > 0 ) )
            {
            double megabytes = opb[OPB_OPERATION_SIZE];
            megabytes /= ( 1024*1024 );
            double seconds = opb[OPB_TIMER_STOP] - opb[OPB_TIMER_START];
            seconds /= 1E7;
            double mbps = megabytes / seconds;
            s = String.format
                ( "READ:  megabytes = %.1f, seconds = %.1f, mbps = %.1f ", 
                  megabytes, seconds, mbps );
            }
        else
            {
            s = "Error = " + a;
            }
        System.out.println( s );
        }
    }

void testWrite()
    {
    if ( ! binaryValid )
        {
        System.out.println( "Native write test skipped." );
        }
    else
        {
        System.out.println( "Native write test." );
        // send input parameters for Write File function
        int fileSize  = 100 * 1024 * 1024;
        int blockSize = 1024 * 1024;
        String fileName = SRC_FILE_NAME;
        int ipbSize = FILE_API_IPB_SIZE;
        int opbSize = FILE_API_OPB_SIZE + OPB_ALIGNMENT_ADDEND + blockSize / 8;
        long[] ipb = new long[ipbSize];
        long[] opb = new long[opbSize];
        ipb[IPB_REQUEST_ID]     = MEASURE_WRITE_FILE;
        ipb[IPB_REQUEST_SIZE]   = fileSize;
        ipb[IPB_BLOCK_SIZE]     = blockSize;
        ipb[IPB_SRC_ATTRIBUTES] = WINDOWS_FILE_ATTRIBUTE_NORMAL +
                                  WINDOWS_FILE_ATTRIBUTE_NO_BUFFERING +
                                  WINDOWS_FILE_ATTRIBUTE_WRITE_THROUGH;
        ipb[IPB_ITERATIONS] = 5;
        transmitStringToIPB( fileName, ipb, IPB_SRC_PATH );
        // call Write File function
        int status = pal.entryBinary( ipb, opb, ipbSize, opbSize );
        // Receive and interpreting output parameters after Write File function
        String s;
        long a = opb[OPB_LAST_ERROR];
        if ( ( a == 0 )&&( status > 0 ) )
            {
            double megabytes = opb[OPB_OPERATION_SIZE];
            megabytes /= ( 1024*1024 );
            double seconds = opb[OPB_TIMER_STOP] - opb[OPB_TIMER_START];
            seconds /= 1E7;
            double mbps = megabytes / seconds;
            s = String.format
                ( "WRITE: megabytes = %.1f, seconds = %.1f, mbps = %.1f ", 
                  megabytes, seconds, mbps );
            }
        else
            {
            s = "Error = " + a;
            }
        System.out.println( s );
        }
    }

void testCopy()
    {
    if ( ! binaryValid )
        {
        System.out.println( "Native copy test skipped." );
        }
    else
        {
        System.out.println( "Native copy test." );
        // send input parameters for Write File function
        int fileSize  = 100 * 1024 * 1024;
        int blockSize = 1024 * 1024;
        String srcFileName = SRC_FILE_NAME;
        String dstFileName = DST_FILE_NAME;
        int ipbSize = FILE_API_IPB_SIZE;
        int opbSize = FILE_API_OPB_SIZE + OPB_ALIGNMENT_ADDEND + blockSize / 8;
        long[] ipb = new long[ipbSize];
        long[] opb = new long[opbSize];
        ipb[IPB_REQUEST_ID]     = MEASURE_COPY_FILE;
        ipb[IPB_REQUEST_SIZE]   = fileSize;
        ipb[IPB_BLOCK_SIZE]     = blockSize;
        long attributes = WINDOWS_FILE_ATTRIBUTE_NORMAL +
                          WINDOWS_FILE_ATTRIBUTE_NO_BUFFERING +
                          WINDOWS_FILE_ATTRIBUTE_WRITE_THROUGH;
        ipb[IPB_SRC_ATTRIBUTES] = attributes;
        ipb[IPB_DST_ATTRIBUTES] = attributes;
        ipb[IPB_ITERATIONS] = 5;
        transmitStringToIPB( srcFileName, ipb, IPB_SRC_PATH );
        transmitStringToIPB( dstFileName, ipb, IPB_DST_PATH );
        // call Write File function
        int status = pal.entryBinary( ipb, opb, ipbSize, opbSize );
        // Receive and interpreting output parameters after Write File function
        String s;
        long a = opb[OPB_LAST_ERROR];
        if ( ( a == 0 )&&( status > 0 ) )
            {
            double megabytes = opb[OPB_OPERATION_SIZE];
            megabytes /= ( 1024*1024 );
            double seconds = opb[OPB_TIMER_STOP] - opb[OPB_TIMER_START];
            seconds /= 1E7;
            double mbps = megabytes / seconds;
            s = String.format
                ( "COPY: megabytes = %.1f, seconds = %.1f, mbps = %.1f ", 
                  megabytes, seconds, mbps );
            }
        else
            {
            s = "Error = " + a;
            }
        System.out.println( s );
        }

    }

void testMixedIO()
    {
    
    }

void testDelete()
    {
    testDelete( SRC_FILE_NAME );
    testDelete( DST_FILE_NAME );
    }

void testDelete( String fileName )
    {
    int ipbSize = FILE_API_IPB_SIZE;
    int opbSize = FILE_API_OPB_SIZE;
    long[] ipb = new long[ipbSize];
    long[] opb = new long[opbSize];
    ipb[IPB_REQUEST_ID] = MEASURE_DELETE_FILE;
    transmitStringToIPB( fileName, ipb, IPB_SRC_PATH );
    int status = pal.entryBinary( ipb, opb, ipbSize, opbSize );
    long a = opb[OPB_LAST_ERROR];
    String s;
    if ( ( status > 0 )&&(  a == 0 ) )
        {
        s = "delete ok";
        }
    else
        {
        s = "delete failed, error = " + a;
        }
    System.out.println( s );
    
    }

}
