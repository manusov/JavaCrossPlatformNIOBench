/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Run" button: run selected benchmark scenario.
*/

package niobenchrefactoring.controller;

import java.awt.event.ActionEvent;
import static niobenchrefactoring.model.HelperDelay.delay;
import niobenchrefactoring.model.StatusEntry;
import niobenchrefactoring.view.Application;
import niobenchrefactoring.view.ApplicationPanel;

public class HandlerRun extends Handler
{
private boolean runStop = false;
private boolean interrupted = false;
private Thread threadRun;
private final static String MESSAGE_INTERRUPTED = "Test interrupted";
    
public HandlerRun( Application application )
    {
    super( application );
    }
    
@Override public void actionPerformed( ActionEvent e )
    {
    if ( application != null )
        {
        ApplicationPanel panel = application.getSelectedPanel();
        if ( panel != null )
            {
            if ( ! runStop )
                {  // test RUNNING by this button press
                runStop = true;
                threadRun = new ThreadRun( panel );
                threadRun.start();
                }
            else
                {  // test STOPPED by this button press
                threadRun.interrupt();
                }
            }
        }
    }

private class ThreadRun extends Thread
    {
    private final ApplicationPanel panel;
    ThreadRun( ApplicationPanel panel )
        {
        this.panel = panel;
        }
    
    @Override public void run()
        {
        application.disableGuiBeforeRun();
        panel.disableGuiBeforeRun();
        
        
        /* DEBUG START
        System.out.println( "DBG1" );
        while( ! isInterrupted() )
            {
            StatusEntry entry = delay( 1000 );
            if ( ! entry.flag )
                break;
            System.out.printf( "*" );
            }
        System.out.println( "\r\nDBG2" );
        DEBUG END */
        
        // DEBUG START
        interrupted = false;
        for( int i=0; i<=100; i++ )
            {
            application.updateProgress( i );
            StatusEntry entry = delay( 100 );
            interrupted = ( isInterrupted() | ( ! entry.flag ) );
            if ( interrupted )
                break;
            }
        if( interrupted )
            {
            application.updateProgress( 0 );
            application.updateProgress( MESSAGE_INTERRUPTED );
            }
        // DEBUG END
        
        /*        
        public IOscenario( String pathSrc, String prefixSrc, String postfixSrc,
                   String pathDst, String prefixDst, String postfixDst,
                   int fileCount, int fileSize, int blockSize, int threadCount,
                   boolean readSync, boolean writeSync, boolean copySync,
                   boolean dataSparse, boolean fastCopy, 
                   int readWriteMode, int addressMode, int dataMode,
                   int readDelay, int writeDelay, int copyDelay,
                   byte[] dataBlock )
        */

        

        panel.enableGuiAfterRun();
        application.enableGuiAfterRun();
        runStop = false;
        }
    }
}
