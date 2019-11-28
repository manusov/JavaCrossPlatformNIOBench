/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Application main class.
This class must run CONTROLLER at MODEL/VIEW/CONTROLLER (MVC) functionality.
*/


/*

UNDER CONSTRUCTION. This main module yet sequence of tests

Near roadmap.
---------------
1)+  "About" function and paths.
2)+  Connect templates for native libraries, get native name
     ( win 32/64, linux 32/64 ).
3)+  Native method for get random array
     ( win 32/64, linux 32/64 ).
4)+  Inspect existed classes.

5)   Connect charts (drawings) child window. First verify at constant patterns.

6)   Connect tables child window. First verify at constant patterns.
7)   Design tabbed panel.
8)   Design channels test panel.
9)   Design IOPS-oriented address randomization, must be valid for
     write/copy/read. Correct tasks names. First verify at channels.
10)  Data randomization for all IO scenarios. Verify as MBPS, IOPS.
     First at channels.
11)  Archives benchmark.
12)  Native MBPS support.
13)  Native IOPS support.
14)  Other scenarios applications.
10)  Tools handlers: exit, save report, about.
11)  ...


---
1) TODO. 
   MULTI-THREAD CAN BE SUPPORT FOR ALL SCENARIO BY UNIFIED WAY (?),
   SUPPORT MULTI-THREAD FOR EACH READ-WRITE METHOD IS TOO COMPLEX.
   PLUS, CAN COMBINE DIFFERENT IO METHODS IF SUPPORTED BY ALL SCENARIO.
   CAN IMPLEMENT SERVER LOAD MODEL.
2) TODO. 
   CHECK ISINTERRUPTED FOR ALL IOTASKS.
3) NOTE.
   Classic MBPS and IOPS measurement first actual for NATIVE mode.

*/


package niobenchrefactoring;

import niobenchrefactoring.controller.HandlerAbout;
import niobenchrefactoring.model.HelperDelay;
import niobenchrefactoring.model.IOscenario;
import niobenchrefactoring.model.IOscenarioAsyncChannel;
import niobenchrefactoring.model.IOscenarioChannel;
import niobenchrefactoring.model.IOscenarioMapped;
import niobenchrefactoring.model.IOscenarioScatterGather;
import niobenchrefactoring.model.StateAsync;
import niobenchrefactoring.model.StateSync;
import niobenchrefactoring.resources.About;
import niobenchrefactoring.resources.PAL;
import javax.swing.JDialog;
import javax.swing.JFrame;
import static niobenchrefactoring.resources.IOPB.extractStringFromOPB;

public class NIOBenchRefactoring 
{

public static void main(String[] args) 
    {
/*
    int a = (int)0x80000000L; // 0x7FFFFFFF;
    int b = 2;                // 0x7FFFFFFF;
    long c = a * b;
    long d = (long)( a & 0xFFFFFFFFL ) * (long)( b & 0xFFFFFFFFL );
*/

/*        
    IOscenario ios = new IOscenarioChannel();
    ios.start();
*/

/*    
    IOscenario ios = new IOscenario();
    // ios.start();  // THIS TEST WITHOUT START

    double[] TEST_DATA = { 20.381, 25.10, 2.87, 3.14, 1.86 };
        System.out.println( "\r\n[START TEST STATISTICS]" );
    String line = 
        "-----------------------------------------------------------------";
    System.out.println( line );
    System.out.println( "current  min      max      average  median   status" );
    System.out.println( line );
    StatisticsModel sm = ios.getStatistics();
    for( int i=0; i<TEST_DATA.length; i++ )
        {
        boolean status = sm.send( 0, TEST_DATA[i] );
        StateAsync entry = sm.receive( 0 );
        String s = String.format
            ( "%-9.3f%-9.3f%-9.3f%-9.3f%-9.3f%b", 
              entry.current, entry.min, entry.max, entry.average, entry.median,
              status );
        System.out.println( s );
        }
    System.out.println( line );
    System.out.println( "[STOP TEST STATISTICS]" );
*/

/*        
    //
    // IOscenario ios = new IOscenarioChannel();
    // IOscenario ios = new IOscenarioAsyncChannel();
    // IOscenario ios = new IOscenarioMapped();
       IOscenario ios = new IOscenarioScatterGather();
    //   
    
    String unitsString = "MPPS";  // "MBPS"; "IOPS";
    
    System.out.println( "\r\n[ START IO SCENARIO. SYNCHRONOUS MONITOR ]" );
    String line =   
            "----------------------------------------------------------";
    String uptext = 
            " #    current  min      max      average  median   status ";

    ios.start();
    
    int previousID = -1;
    int postCount = 3;

    while( postCount > 0 )
        {
        HelperDelay.delay( 150 );
        StateSync e = ios.getSync();
        while ( e != null )
            {
            if ( e.phaseID != previousID )
                {
                previousID = e.phaseID;
                System.out.println( line );
                System.out.println( e.phaseName );
                System.out.println( uptext );
                System.out.println( line );
                }
            String s = String.format
                ( " %-5d%-9.2f%-9.2f%-9.2f%-9.2f%-9.2f%-5b",
                  e.count, e.current, e.min, e.max, e.average, e.median,
                  e.statusEntry.flag );
            System.out.println( s );
            e = ios.getSync();
            }
        if ( ! ios.isAlive() )
            {
            postCount--;
            }
        }
    
    System.out.println( line );
    totalStatisticsHelper( ios, unitsString );
    System.out.println( line );
    System.out.println( "[ STOP SYNCHRONOUS MONITOR ]" );
*/


/*        
    JFrame.setDefaultLookAndFeelDecorated( true );
    JDialog.setDefaultLookAndFeelDecorated( true );
    HandlerAbout about = new HandlerAbout();
    final JDialog dialog = about.createDialog
        ( null , About.getShortName() , About.getVendorName() );
    dialog.setLocationRelativeTo( null );
    dialog.setVisible( true );
*/

        
    PAL pal = new PAL();
    int loadStatus = pal.loadUserModeLibrary();
    int runStatus = -1;
    String runString = "no status string";
    if ( loadStatus == 0 )
        {
        try 
            {
            runStatus = pal.checkBinary(); 
            }
        catch ( Exception e )
            {
            runStatus = -2; 
            runString = e.toString();
            }
        catch ( UnsatisfiedLinkError e )
            {
            runStatus = -3; 
            runString = e.toString();
            }
        }
    String s1 = String.format( "Native library loader test:\r\n" + 
                               "load status = %d, run status = %d, [ %s ]",
                               loadStatus, runStatus, runString );
    System.out.println( s1 );

    if ( ( loadStatus == 0 )&&( runStatus > 0 ) )
        {
        int size = 4096/8;
        long[] data = new long[size];
        int nameStatus = pal.entryBinary( null, data, 0, size );
        String nameString = extractStringFromOPB( data );
        String s2 = String.format( "Get name status = %d, name = [ %s ]", 
                                   nameStatus, nameString );
        System.out.println( s2 );
        
        int detectStatus = pal.entryBinary( null, data, 1, size );
        int detectValue  = (int) data[0];
        String s3 = String.format( "RDRAND detect: status = %d, value = %d", 
                                   detectStatus, detectValue );
        System.out.println( s3 );
        
        long[] ipb = new long[size];
        long[] opb = new long[size];
        ipb[0] = 0;
        ipb[1] = 5;
        int numStatus = pal.entryBinary( ipb, opb, size, size );
        int numValue  = (int) opb[0];
        String s4 = String.format( "RDRAND data: status = %d, value = %d", 
                                   numStatus, numValue );
        System.out.println( s4 );
        
        int k = 0;
        for( int i=0; i<4; i++ )
            {
            for( int j=0; j<4; j++ )
                {
                System.out.printf( "%-21d  ", opb[k++] );
                }
            System.out.println();
            }
        }
    
    
    }
        
        
/*
Helper for summary statistics:
total MBPS per series of files Read, Write, Copy
*/
private static void totalStatisticsHelper( IOscenario ios, String unitsString )
    {
    StateAsync e;
    StateAsync[] entries = ios.getAsync();
    double totalRead = Double.NaN;
    double totalWrite = Double.NaN;
    double totalCopy = Double.NaN;
    if ( entries != null )
        {
        if ( ( entries.length > 3 )&&( ( e = entries[3] ) != null ) )
            totalRead = e.current;
        if ( ( entries.length > 4 )&&( ( e = entries[4] ) != null ) )
            totalWrite = e.current;
        if ( ( entries.length > 5 )&&( ( e = entries[5] ) != null ) )
            totalCopy = e.current;
        String s = String.format
            ( "Total %s: Read=%.3f , Write=%.3f , Copy=%.3f", 
               unitsString, totalRead, totalWrite, totalCopy );
        System.out.println( s );
        }
    }
}
