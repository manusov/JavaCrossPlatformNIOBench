/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Platform Abstraction Layer (PAL) class for
check, detect native platform and communication with native library.
*/

package niobenchrefactoring.resources;

import niobenchrefactoring.NIOBenchRefactoring;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class PAL
{
private final static String[] LIBRARY_NAMES = 
    { "WIN32JNI" , "WIN64JNI" , "libLINUX32JNI" , "libLINUX64JNI" };
private final static String[] LIBRARY_EXTENSIONS = 
    { ".dll"     , ".dll"     , ".so"           , ".so"           };
private final static int LIBRARY_COUNT = LIBRARY_NAMES.length;

// This is one block, not limit maximum library size
private final static int BINARY_SIZE = 16384;  
private static int binaryType = -1;
private static boolean binaryValid = false;
private static File library;

// Public constants for native functions select and status encoding
// ID constants for functions without IPB
public final static int GET_LIBRARY_NAME = 0;
public final static int GET_LIBRARY_INFO = 1;
// ID constants for functions with IPB
public final static int GET_RANDOM_DATA     = 0;
public final static int MEASURE_READ_FILE   = 1;
public final static int MEASURE_WRITE_FILE  = 2;
public final static int MEASURE_COPY_FILE   = 3;
public final static int MEASURE_MIXED_IO    = 4;
public final static int MEASURE_DELETE_FILE = 5;
// offsets for IPB addressing, units = asm quad words = java long
public final static int IPB_REQUEST_ID     = 0;
public final static int IPB_REQUEST_SIZE   = 1;
public final static int IPB_BLOCK_SIZE     = 2;
public final static int IPB_SRC_ATTRIBUTES = 3;
public final static int IPB_DST_ATTRIBUTES = 4;
public final static int IPB_ITERATIONS     = 5;
public final static int IPB_SRC_PATH       = 1024/8;
public final static int IPB_DST_PATH       = 2048/8;
// offsets for OPB addressing, units = asm quad words = java long
public final static int OPB_BUFFER_BASE    = 0;
public final static int OPB_BUFFER_SIZE    = 1;
public final static int OPB_SRC_HANDLE     = 2;
public final static int OPB_DST_HANDLE     = 3;
public final static int OPB_OPERATION_SIZE = 4;
public final static int OPB_TIMER_START    = 5;
public final static int OPB_TIMER_STOP     = 6;
public final static int OPB_LAST_OPERATION = 7;
public final static int OPB_LAST_ERROR     = 8;
public final static int OPB_DATA_ARRAY     = 4096/8;
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
public final static long WINDOWS_FILE_ATTRIBUTE_NORMAL        = 0x80L;
public final static long WINDOWS_FILE_ATTRIBUTE_NO_BUFFERING  = 0x20000000L;
public final static long WINDOWS_FILE_ATTRIBUTE_WRITE_THROUGH = 0x80000000L;
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
            if ( status == 0 ) { break; }
            }
        catch ( IOException e )
            {
            status = -1;
            }
        }
    binaryValid = status >= 0;
    return status;
    }
}
