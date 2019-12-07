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
import niobenchrefactoring.model.IOscenario;
import static niobenchrefactoring.model.IOscenario.COMPLETE_ID;
import static niobenchrefactoring.model.IOscenario.DELAY_ID;
import static niobenchrefactoring.model.IOscenario.DELETE_ID;
import static niobenchrefactoring.model.IOscenario.ERROR_ID;
import static niobenchrefactoring.model.IOscenario.READ_ONLY;
import static niobenchrefactoring.model.IOscenario.STARTING_ID;
import static niobenchrefactoring.model.IOscenario.WRITE_ONLY;
import niobenchrefactoring.model.StateAsync;
import niobenchrefactoring.model.StateSync;
import niobenchrefactoring.model.StatusEntry;
import niobenchrefactoring.model.TableChannel;
import niobenchrefactoring.view.Application;
import niobenchrefactoring.view.ApplicationPanel;

public class HandlerRun extends Handler
{
private boolean runStop = false;
private boolean interrupt = false;
private boolean error = false;
private Thread threadRun;
private final static String MESSAGE_INTERRUPTED = "Test interrupted";
private final static int ASYNC_PAUSE = 10;

private final double ACTIVE_PERCENTAGE = 99.0;
private double currentPercentage;
private double addendPercentage;
    
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

/*
Asynchronous thread for test execution
*/

private class ThreadRun extends Thread
    {
    private final ApplicationPanel panel;
    private final TableChannel table;
    ThreadRun( ApplicationPanel panel )
        {
        this.panel = panel;
        table = panel.getTableModel();
        }
    
    @Override public void run()
        {
        /*
        Prepare GUI state for test, clear previous results    
        */
        application.disableGuiBeforeRun();
        panel.disableGuiBeforeRun();
        table.clear();
        
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
        
        /* DEBUG START
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
        DEBUG END */

        /*
        Initializing variables
        */
        currentPercentage = 0.0;
        int fileCount = panel.optionFileCount();
        int rwMode = panel.optionRwMode();
        int deleteAddend = 1;
        int phaseCount = 3;             // write + copy + read = 3 phases
        if ( rwMode == READ_ONLY )
            {
            phaseCount = 1;             // read only = 1 phase
            deleteAddend = 0;
            }
        if ( rwMode == WRITE_ONLY )
            {
            phaseCount = 2;             // write + copy = 2 phases
            deleteAddend = 0;
            }
        addendPercentage = 
            ACTIVE_PERCENTAGE / ( fileCount * phaseCount + deleteAddend );
        interrupt = false;
        error = false;
        
        /*
        Run test
        */
        application.updateProgress( 0 );
        application.updateOperationString( "Starting test...", STARTING_ID );
        IOscenario ios = panel.buildIOscenario();
        ios.start();
        int postCount = 3;
        
        /*
        Test main cycle start
        */
        while( postCount > 0 )
            {
            StateSync sync = ios.getSync();
            while ( sync != null )
                {
                application.updateProgress( (int)currentPercentage );
                String opname;
                // support errors visualization
                StatusEntry entry = sync.statusEntry;
                if ( entry != null )
                    {
                    if ( ! entry.flag )
                        {
                        error = true;
                        application.
                            updateOperationString( entry.string, ERROR_ID );
                        }
                    }
                // break if errors detected
                if ( error )
                    break;
                // support test flow visualisation if no errors
                switch ( sync.phaseID ) 
                    {
                    case DELAY_ID:
                        opname = sync.phaseName + ", second # " + sync.count;
                        break;
                    case DELETE_ID:
                        opname = sync.phaseName;
                        currentPercentage += addendPercentage;
                        break;
                    default:
                        opname = sync.phaseName + ", file # " + sync.count;
                        currentPercentage += addendPercentage;
                        break;
                    }
                application.updateOperationString( opname, sync.phaseID );
                // get next sync result, update table if last pass
                sync = ios.getSync();
                if ( sync == null )  // check last iteration of this sub-cycle
                    {
                    // get async result, more detail, 
                    // per all operations: read, write, copy + total R,W,C
                    StateAsync[] async = ios.getAsync();
                    if ( async != null )
                        {
                        // update brief statistics table at window up
                        table.measurementNotify( async );
                        // update openable text log
                        // TODO. contain requirements for each sync ? Change.
                
                        // update openable full statistics table
                        // TODO. contain requirements for each sync ? Change.
                        // current to this, median to brief.
                
                
                        // update openable drawings Y=F(X)
                        // TODO. contain requirements for each sync ? Change.
                            
                        }
                    }
                }
            if ( ! ios.isAlive() )
                postCount--;
            delay( ASYNC_PAUSE );
            }
        /*
        Test main cycle end
        */
        
        application.updateProgress( 100 );
        if ( ! error )
            {
            application.updateOperationString( "Test complete.", COMPLETE_ID );
            }
        
        /*
        Done, restore GUI state
        */
        panel.enableGuiAfterRun();
        application.enableGuiAfterRun();
        runStop = false;
        }
    }
}
