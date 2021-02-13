/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Platform Abstraction Layer (PAL) class for
check, detect native platform and communication with native library.
*/

package niobenchrefactoring.resources;

import niobenchrefactoring.NIOBenchRefactoring;
import java.io.*;
import java.net.URL;

public class PAL
{
private final static String[] LIBRARY_NAMES = 
    { "WIN32JNI" , "WIN64JNI" , "libLINUX32JNI" , "libLINUX64JNI" };
private final static String[] LIBRARY_EXTENSIONS = 
    { ".dll"     , ".dll"     , ".so"           , ".so"           };
private final static int LIBRARY_COUNT = LIBRARY_NAMES.length;
/*
This is one block, not limit maximum library size
*/
private final static int BINARY_SIZE = 16384;  
private static int binaryType = -1;
private static boolean binaryValid = false;
private static File library;
/*
Public constants for native functions select and status encoding
ID constants for functions without IPB
*/
public final static int GET_LIBRARY_NAME = 0;
public final static int GET_LIBRARY_INFO = 1;
/*
ID constants for functions with IPB
*/
public final static int GET_RANDOM_DATA     = 0;
public final static int MEASURE_READ_FILE   = 1;
public final static int MEASURE_WRITE_FILE  = 2;
public final static int MEASURE_COPY_FILE   = 3;
public final static int MEASURE_DELETE_FILE = 4;
/*
Support native model with precision measurements and timer underflow tolerant
note single file can be selected as group with N=1.
*/
public final static int PRECISION_LINEAR = 5;  // write, copy, read N files
public final static int PRECISION_MIXED  = 6;  // mixed IO, random sequence
// offsets for IPB addressing, units = asm quad words = java long
public final static int IPB_REQUEST_ID     = 0;
public final static int IPB_REQUEST_SIZE   = 1;
public final static int IPB_BLOCK_SIZE     = 2;
public final static int IPB_SRC_ATTRIBUTES = 3;
public final static int IPB_DST_ATTRIBUTES = 4;
public final static int IPB_ITERATIONS     = 5;
public final static int IPB_FILE_COUNT     = 6;
public final static int IPB_SRC_PATH       = 1024/8;
public final static int IPB_DST_PATH       = 2048/8;
// paths strings locations for sequences of files with
// name generation by native code
public final static int IPB_SRC_PATH_PREFIX  = 1024/8;
public final static int IPB_SRC_PATH_POSTFIX = 1024/8 + 512/8;
public final static int IPB_DST_PATH_PREFIX  = 2048/8;
public final static int IPB_DST_PATH_POSTFIX = 2048/8 + 512/8;
// offsets for OPB addressing, units = asm quad words = java long
public final static int OPB_BUFFER_BASE    = 0;
public final static int OPB_BUFFER_SIZE    = 1;
public final static int OPB_SRC_HANDLE     = 2;
public final static int OPB_DST_HANDLE     = 3;
public final static int OPB_OPERATION_SIZE = 4;
public final static int OPB_TIMER_DELTA    = 5;
public final static int OPB_LAST_OPERATION = 6;
public final static int OPB_LAST_ERROR     = 7;
// support precision mode
public final static int OPB_HANDLES_SRC_BASE = 16;
public final static int OPB_HANDLES_DST_BASE = 17;
public final static int OPB_TIMER_READ       = 18;
public final static int OPB_TIMER_WRITE      = 19;
public final static int OPB_TIMER_COPY       = 20;
public final static int OPB_TOTAL_READ       = 21;
public final static int OPB_TOTAL_WRITE      = 22;
public final static int OPB_TOTAL_COPY       = 23;
public final static int OPB_DATA_ARRAY       = 4096/8;
// constant for size and alignment reservations at IPB and OPB + data buffer
public final static int FILE_API_IPB_SIZE    = 4096/8;
public final static int FILE_API_OPB_SIZE    = 4096/8;
public final static int OPB_ALIGNMENT_ADDEND = 4096/8;
// status constants for native IO operation phase ID
public final static int STEP_NONE       = 0;
public final static int STEP_OPEN_READ  = 1;
public final static int STEP_READ       = 2;
public final static int STEP_OPEN_WRITE = 3;
public final static int STEP_WRITE      = 4;
public final static int STEP_CLOSE      = 5;
public final static int STEP_DELETE     = 6;
// Microsoft Windows file attributes
public final static long FILE_ATTRIBUTE_BLANK      = 0;
public final static long FILE_ATTRIBUTE_READ_SYNC  = 1;
public final static long FILE_ATTRIBUTE_WRITE_SYNC = 2;
// special status constant for native width
public final static int JRE32_UNDER_OS64 = 33;
// Methods for get native platform detection results
// Binaries types: 0=Win32, 1=Win64, 2=Linux32, 3=Linux64, ... , -1=Unknown
public int getBinaryType()      { return binaryType;  }
public boolean getBinaryValid() { return binaryValid; }
// Target native methods
// entryBinary parameters is IPB, OPB, IPB size, OPB size (size in LONG-QWORDS)
// IPB = Input Parameters block, OPB = Output Parameters Block
public native int checkBinary();
public native int entryBinary( long[] a, long[] b, long c, long d );

/*
Load native library for current OS, "DLL" for Windows, "SO" for Linux,
4 variants: Windows 32, Windows 64, Linux 32, Linux 64.
return = status, 
if status >= 0, library loaded OK,
otherwise error: library not loaded.
*/
public int loadUserModeLibrary()
    {
    // Initializing variables
    int status = 0;
    int count;
    binaryType = OSDetector.detect();
    // Load library, cycle for find binary type match
    for ( int i=0; i<LIBRARY_COUNT; i++ )
        {
        if ( i != binaryType ) { status=-1; continue; }
        try {        
            status = 0;
            URL resource = NIOBenchRefactoring.class.getResource
                ( "/niobenchrefactoring/resources/" + 
                LIBRARY_NAMES[i] + LIBRARY_EXTENSIONS[i] );
            if ( resource == null ) throw new IOException();
            try ( InputStream input = resource.openStream() )
                {
                library = File.createTempFile
                        ( LIBRARY_NAMES[i], LIBRARY_EXTENSIONS[i] );
                try ( FileOutputStream output = 
                      new FileOutputStream( library ) ) 
                    {
                    byte[] buffer = new byte[BINARY_SIZE];
                    count = 0;
                    for ( int j = input.read( buffer ); j != -1; 
                          j = input.read( buffer ) )
                        {
                        output.write( buffer, 0, j );
                        count++;
                        }
                    }
                }
            if ( count > 0   ) { System.load( library.getAbsolutePath() ); }
            if ( status == 0 ) { break; }  // break if library match OK
            }
        catch ( IOException e )
            {
            status = -1;  // set status for error detect
            }
        }
    binaryValid = status >= 0;
    return status;
    }
}
