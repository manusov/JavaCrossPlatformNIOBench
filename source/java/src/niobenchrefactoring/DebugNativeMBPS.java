/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
This class used for debug native linear write/copy/read single file scenario,
means legacy mode with space usage minimization.
This class can be removed from ready product
or used for console mode and/or debug mode support.
*/

package niobenchrefactoring;

import static niobenchrefactoring.resources.IOPB.transmitStringToIPB;
import static niobenchrefactoring.resources.PAL.*;

class DebugNativeMBPS extends DebugNative
{
private final String SRC_PREFIX    = "C:\\TEMP\\src";    // Windows variant
// private final String SRC_PREFIX    = "src";           // Universal variant
private final String SRC_POSTFIX   = ".bin";
private final String DST_PREFIX    = "C:\\TEMP\\dst";    // Windows variant
// private final String DST_PREFIX    = "dst";           // Universal variant
private final String DST_POSTFIX   = ".bin";

private final int FILE_SIZE        = 10 * 1024 * 1024;   // 512 * 1024;
private final int BLOCK_SIZE       = 1 * 1024 * 1024;    // 4 * 1024;
private final int FILE_COUNT       = 10;
private final int ITERATIONS_COUNT = 5;

// Total size example = 512 * 1024 * 20 * 5 = 52,428,800

/*
Write/Copy/Read fixed-size file, call native function PRECISION_LINEAR.
Supports debug messages and Write/Copy/Read speeds calculation.
Depends on global variables (binaryValid, pal)
initialized by parent class constructor.
*/
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
