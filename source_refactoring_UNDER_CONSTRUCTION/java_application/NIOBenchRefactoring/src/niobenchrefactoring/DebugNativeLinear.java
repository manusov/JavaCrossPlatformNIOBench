package niobenchrefactoring;

import static niobenchrefactoring.resources.IOPB.transmitStringToIPB;
import static niobenchrefactoring.resources.PAL.FILE_API_IPB_SIZE;
import static niobenchrefactoring.resources.PAL.FILE_API_OPB_SIZE;
import static niobenchrefactoring.resources.PAL.FILE_ATTRIBUTE_BLANK;
import static niobenchrefactoring.resources.PAL.FILE_ATTRIBUTE_READ_SYNC;
import static niobenchrefactoring.resources.PAL.FILE_ATTRIBUTE_WRITE_SYNC;
import static niobenchrefactoring.resources.PAL.IPB_BLOCK_SIZE;
import static niobenchrefactoring.resources.PAL.IPB_DST_PATH_POSTFIX;
import static niobenchrefactoring.resources.PAL.IPB_DST_PATH_PREFIX;
import static niobenchrefactoring.resources.PAL.IPB_FILE_COUNT;
import static niobenchrefactoring.resources.PAL.IPB_ITERATIONS;
import static niobenchrefactoring.resources.PAL.IPB_REQUEST_ID;
import static niobenchrefactoring.resources.PAL.IPB_REQUEST_SIZE;
import static niobenchrefactoring.resources.PAL.IPB_SRC_ATTRIBUTES;
import static niobenchrefactoring.resources.PAL.IPB_SRC_PATH_POSTFIX;
import static niobenchrefactoring.resources.PAL.IPB_SRC_PATH_PREFIX;
import static niobenchrefactoring.resources.PAL.OPB_ALIGNMENT_ADDEND;
import static niobenchrefactoring.resources.PAL.OPB_BUFFER_BASE;
import static niobenchrefactoring.resources.PAL.OPB_HANDLES_DST_BASE;
import static niobenchrefactoring.resources.PAL.OPB_HANDLES_SRC_BASE;
import static niobenchrefactoring.resources.PAL.OPB_TIMER_COPY;
import static niobenchrefactoring.resources.PAL.OPB_TIMER_READ;
import static niobenchrefactoring.resources.PAL.OPB_TIMER_WRITE;
import static niobenchrefactoring.resources.PAL.OPB_TOTAL_COPY;
import static niobenchrefactoring.resources.PAL.OPB_TOTAL_READ;
import static niobenchrefactoring.resources.PAL.OPB_TOTAL_WRITE;
import static niobenchrefactoring.resources.PAL.PRECISION_LINEAR;

public class DebugNativeLinear extends DebugNativeIO
{
private final String SRC_PREFIX    = "C:\\TEMP\\src";
private final String SRC_POSTFIX   = ".bin";
private final String DST_PREFIX    = "C:\\TEMP\\dst";
private final String DST_POSTFIX   = ".bin";

private final int FILE_SIZE        = 100 * 1024 * 1024;   // 512 * 1024;
private final int BLOCK_SIZE       = 10 * 1024 * 1024;    // 4 * 1024;
private final int FILE_COUNT       = 10;
private final int ITERATIONS_COUNT = 5;

// Total size example = 512 * 1024 * 20 * 5 = 52,428,800

void testLinear()
    {
    if ( ! binaryValid )
        {
        System.out.println( "Native linear sequence test skipped." );
        }
    else
        {
        System.out.println( "Native linear sequence test." );
        int ipbSize = FILE_API_IPB_SIZE;
        int opbSize = FILE_API_OPB_SIZE + BLOCK_SIZE / 8 + FILE_COUNT * 2 +
                      OPB_ALIGNMENT_ADDEND;
        long[] ipb = new long[ipbSize];
        long[] opb = new long[opbSize];
        ipb[IPB_REQUEST_ID]     = PRECISION_LINEAR;
        ipb[IPB_REQUEST_SIZE]   = FILE_SIZE;
        ipb[IPB_BLOCK_SIZE]     = BLOCK_SIZE;
        ipb[IPB_FILE_COUNT]     = FILE_COUNT;
        ipb[IPB_ITERATIONS]     = ITERATIONS_COUNT;
        ipb[IPB_SRC_ATTRIBUTES] = FILE_ATTRIBUTE_BLANK +
                                  FILE_ATTRIBUTE_READ_SYNC +
                                  FILE_ATTRIBUTE_WRITE_SYNC;
        
        transmitStringToIPB( SRC_PREFIX,  ipb, IPB_SRC_PATH_PREFIX );
        transmitStringToIPB( SRC_POSTFIX, ipb, IPB_SRC_PATH_POSTFIX );
        transmitStringToIPB( DST_PREFIX,  ipb, IPB_DST_PATH_PREFIX );
        transmitStringToIPB( DST_POSTFIX, ipb, IPB_DST_PATH_POSTFIX );

        long a = opb[OPB_BUFFER_BASE];
        long b = opb[OPB_HANDLES_SRC_BASE];
        long c = opb[OPB_HANDLES_DST_BASE];
        
        System.out.println
            ( "Cleared bases = " + a + ", " + b + ", " + c );
        
        int status = pal.entryBinary( ipb, opb, ipbSize, opbSize );
        
        a = opb[OPB_BUFFER_BASE];
        b = opb[OPB_HANDLES_SRC_BASE];
        c = opb[OPB_HANDLES_DST_BASE];
        System.out.println
            ( "Status and initialized bases = " +
              status + ", " + a + ", " + b + ", " + c );
        
        long tor = opb[OPB_TOTAL_READ];
        long tow = opb[OPB_TOTAL_WRITE];
        long toc = opb[OPB_TOTAL_COPY];
        System.out.println
            ( "R/W/C total size (bytes) = " + tor + ", " + tow + ", " + toc );
        
        long tir = opb[OPB_TIMER_READ];
        long tiw = opb[OPB_TIMER_WRITE];
        long tic = opb[OPB_TIMER_COPY];
        System.out.println
            ( "R/W/C delta timer (ns) = " + tir + ", " + tiw + ", " + tic );

        double readMegabytes  = tor / 1000000.0;
        double writeMegabytes = tow / 1000000.0;
        double copyMegabytes  = toc / 1000000.0;
        String s = String.format
            ( "R/W/C megabytes = %.2f, %.2f, %.2f", 
              readMegabytes, writeMegabytes, copyMegabytes );
        System.out.println( s );

        double readSeconds  = tir / 1000000000.0;
        double writeSeconds = tiw / 1000000000.0;
        double copySeconds  = tic / 1000000000.0;
        s = String.format
            ( "R/W/C seconds = %.2f, %.2f, %.2f", 
              readSeconds, writeSeconds, copySeconds );
        System.out.println( s );
        
        double readMBPS  = readMegabytes / readSeconds;
        double writeMBPS = writeMegabytes / writeSeconds;
        double copyMBPS  = copyMegabytes / copySeconds;
        s = String.format
            ( "R/W/C mbps = %.2f, %.2f, %.2f", 
              readMBPS, writeMBPS, copyMBPS );
        System.out.println( s );
        }
    }
}
