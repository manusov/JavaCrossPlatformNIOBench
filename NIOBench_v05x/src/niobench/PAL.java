/*

NIOBench, file I/O Benchmark utility. (C)2018 IC Book Labs
Platform Abstraction Layer, communications with native OS.

*/

package niobench;

import java.io.*;
import java.net.URL;

public class PAL 
{
static int nativeType = -1;
final static int BLOCK_SIZE = 16384;
public native int checkPAL();
public native int entryPAL( long[] a, long[] b, long c, long d );

// Method for get native platform detection results
// NativeType: 0=Win32, 1=Win64, 2=Linux32, 3=Linux64, ... , -1=Unknown
public static int getNativeType() 
    {
    return nativeType; 
    }

// Method for load user mode library
public int loadUserModeLibrary()
    {
    String[] libNames      = 
        { "WIN32JNI" , "WIN64JNI" , "libLINUX32JNI" , "libLINUX64JNI" };
    String[] libExtensions = 
        { ".dll"     , ".dll"     , ".so"           , ".so"           };
    int status = 0;
    int count = 0;
    int i = 0;
    // detect native OS
    int n = libNames.length;
    int m = OSDetector.detectNative();
    nativeType = m;
    // cycle for select library load mode = f(OS) and load library
    for (i=0; i<n; i++)
        {
        if ( i != m ) { continue; }
        try {        
            status = 0;
            URL resource = NIOBench.class.getResource
                ( "/niobench/resources/" + libNames[i] + libExtensions[i] );
            File library;
            // input stream is library file image in the JAR resources
            // create input stream by JAR resource
            try ( InputStream input = resource.openStream() ) 
                {
                // library is library disk file in the temporary directory
                // create temporary file by name and extension
                library = File.createTempFile( libNames[i], libExtensions[i] );
                // output stream for write to library file on disk
                // create output stream by library file
                try ( FileOutputStream output = new FileOutputStream(library) ) 
                    {
                    byte[] buffer = new byte[BLOCK_SIZE];
                    count = 0;
                    for (int j=input.read(buffer); j!=-1; j=input.read(buffer))
                        {
                        output.write( buffer, 0, j );
                        count++;
                        }
                    }
                }
            if ( count>0   ) { System.load(library.getAbsolutePath()); }
            if ( status==0 ) { break; }
            }
        catch (Throwable e)
            {
            status = -1;
            count = 0;
            }
        }
    return status;
    }
}
