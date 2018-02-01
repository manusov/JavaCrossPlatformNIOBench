//---------- NIOBench, file I/O Benchmark utility. (C)2018 IC Book Labs --------
// Detect OS type and platform native architecture

package niobench;

import java.util.Properties;

public class OSDetector 
{
private final static String[] osPatterns   = { "win" , "linux" };
private final static String[] cpuPatterns  = { "x86" , "amd64" , "i386" };
private final static String[] propPatterns = { "os.name" , "os.arch" };

public static int detectNative()
    {
    //--- initializing variables ---
    int nativeType = -1;
    int xO = -1;
    int xC = -1;
    int nP = propPatterns.length;
    int nO = osPatterns.length;
    int nC = cpuPatterns.length;
    Properties p = System.getProperties();
    String[] arguments = new String[nP];
    String s1, s2;
    int n1, n2;
    //--- get system info, built array of strings ---
    for ( int i=0; i<nP; i++ )
        {
        s1 = p.getProperty( propPatterns[i] );
        s1 = s1.trim();
        s1 = s1.toUpperCase();
        arguments[i] = s1;
        }
    //--- detect OS type ---
    for ( int i=0; i<nO; i++ )
        {
        s1 = arguments[0];
        n1 = s1.length();
        s2 = osPatterns[i];
        s2 = s2.trim();
        s2 = s2.toUpperCase();
        n2 = s2.length();
        if ( n1 >= n2 )
            {
            s1 = s1.substring( 0, n2 );
            if ( s1.equals(s2) ) { xO = i; }
            }
        }
    //--- detect CPU architecture ---
    for ( int i=0; i<nC; i++ )
        {
        s1 = arguments[1];
        n1 = s1.length();
        s2 = cpuPatterns[i];
        s2 = s2.trim();
        s2 = s2.toUpperCase();
        n2 = s2.length();
        if ( n1 >= n2 )
            {
            s1 = s1.substring( 0, n2 );
            if ( s1.equals(s2) ) { xC = i; }
            }
        }
    //--- analysing and return ---
    if ( ( xO==0 ) && ( xC==0 ) ) { nativeType = 0; }   // Windows 32
    if ( ( xO==0 ) && ( xC==1 ) ) { nativeType = 1; }   // Windows 64
    if ( ( xO==1 ) && ( xC==0 ) ) { nativeType = 2; }   // Linux 32
    if ( ( xO==1 ) && ( xC==2 ) ) { nativeType = 2; }   // Linux 32
    if ( ( xO==1 ) && ( xC==1 ) ) { nativeType = 3; }   // Linux 64
    return nativeType;
    }

    
}
