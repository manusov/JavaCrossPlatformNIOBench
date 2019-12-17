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

// Public constant for native functions select and status encoding
public final static int GET_LIBRARY_NAME = 0;
public final static int GET_LIBRARY_INFO = 1;
public final static int GET_RANDOM_DATA = 0;
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
