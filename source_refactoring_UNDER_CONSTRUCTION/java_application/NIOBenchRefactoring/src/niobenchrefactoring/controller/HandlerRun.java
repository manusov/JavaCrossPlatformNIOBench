/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Run" button: run selected benchmark scenario.
*/

package niobenchrefactoring.controller;

import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.util.Date;
import javax.swing.table.AbstractTableModel;
import static niobenchrefactoring.model.HelperDelay.delay;
import niobenchrefactoring.model.IOscenario;
import static niobenchrefactoring.model.IOscenario.COMPLETE_ID;
import static niobenchrefactoring.model.IOscenario.COPY_ID;
import static niobenchrefactoring.model.IOscenario.DELAY_ID;
import static niobenchrefactoring.model.IOscenario.DELETE_ID;
import static niobenchrefactoring.model.IOscenario.ERROR_ID;
import static niobenchrefactoring.model.IOscenario.READ_ID;
import static niobenchrefactoring.model.IOscenario.READ_ONLY;
import static niobenchrefactoring.model.IOscenario.STARTING_ID;
import static niobenchrefactoring.model.IOscenario.TOTAL_COPY_ID;
import static niobenchrefactoring.model.IOscenario.TOTAL_READ_ID;
import static niobenchrefactoring.model.IOscenario.TOTAL_WRITE_ID;
import static niobenchrefactoring.model.IOscenario.WRITE_ID;
import static niobenchrefactoring.model.IOscenario.WRITE_ONLY;
import niobenchrefactoring.model.StateAsync;
import niobenchrefactoring.model.StateSync;
import niobenchrefactoring.model.StatusEntry;
import niobenchrefactoring.model.TableChannel;
import niobenchrefactoring.view.Application;
import niobenchrefactoring.view.ApplicationPanel;
import opendraw.FunctionController;
import opendraw.FunctionModelInterface;
import opendraw.OpenDraw;
import openlog.OpenLog;
import opentable.OpenTable;
import opentable.ReportTableModel;
import opentable.StatisticsTableModel;

public class HandlerRun extends Handler
{
private boolean runStop = false;
private boolean interrupt = false;
private boolean error = false;
private Thread threadRun;
private final static String MESSAGE_INTERRUPTED = "Test interrupted";
private String unitsString;
private final static int ASYNC_PAUSE = 10;

private final double ACTIVE_PERCENTAGE = 99.0;
private double currentPercentage;
private double addendPercentage;
private int previousID;

private OpenLog   childLog;
private OpenTable childTable;
private OpenDraw  childDraw;
private StatisticsTableModel childTableModel;
private FunctionModelInterface childDrawModel;
    
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
        childLog   = application.getChildLog();
        childTable = application.getChildTable();
        if ( childTable != null )
            {
            childTableModel = childTable.getTableModel();
            }
        childDraw  = application.getChildDraw();
        if ( childDraw != null )
            {
            FunctionController ctrl = childDraw.getController();
            if ( ctrl != null )
                {
                childDrawModel = ctrl.getModel();
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
        unitsString = "MBPS";
        application.disableGuiBeforeRun();
        panel.disableGuiBeforeRun();
        table.clear();
        /*
        Openable log:   send benchm. start message with time, get milliseconds
        Openable table: blank according with number of files
        Openable draw:  start model, setup X axis count
        */
        Date date = new Date();
        long t1 = date.getTime();
        String msg = String.format
            ( "--- Benchmark runs at %s ---\r\n", date.toString() );
        logHelper( msg );
        // childDrawModel.startModel();
        childDrawModel.blankModel();
        int fileCount = panel.optionFileCount();
        if ( fileCount > 0 )
            {
            childTable.blankTable( fileCount );
            childDrawModel.rescaleXmax( fileCount );
            }
        logHelper( null );
        tableHelper( null );
        drawHelper( null );
        
        /*
        Initializing variables
        */
        currentPercentage = 0.0;
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
        previousID = -1;
        
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
                // support openable statistics table
                tableHelper( sync );
                // support openable drawings table
                BigDecimal[] bd = new BigDecimal[] 
                    { new BigDecimal( sync.count - 1 ), null, null, null };
                
                if ( sync.phaseID != previousID )
                    {
                    previousID = sync.phaseID;
                    childDrawModel.resetCount();
                    }
                
                switch ( sync.phaseID )
                    {
                    case READ_ID:
                        bd[1] = new BigDecimal( sync.current );
                        drawHelper( bd );
                        break;
                    case WRITE_ID:
                        bd[2] = new BigDecimal( sync.current );
                        drawHelper( bd );
                        break;
                    case COPY_ID:
                        bd[3] = new BigDecimal( sync.current );
                        drawHelper( bd );
                        break;
                    }
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
                        }
                    }
                }
            if ( ! ios.isAlive() )
                postCount--;
            delay( ASYNC_PAUSE );
            }
        /*
        Test main cycle end,
        */
        application.updateProgress( 100 );
        if ( ! error )
            {
            application.updateOperationString( "Test complete.", COMPLETE_ID );
            }
        /*
        Send benchmark done message with time to text log, get milliseconds
        */
        date = new Date();
        long t2 = date.getTime();
        double seconds = ( t2 - t1 ) / 1000.0;
        msg = String.format
            ( "--- Benchmark done at %s ---\r\n" + 
              "Duration include service time is %.3f seconds\r\n",  
              date.toString(), seconds );
        logHelper( msg );
        /*
        Get integral time measurement results and write to text log.
        Mark medians in the statistics table.
        */
        StateAsync[] async = ios.getAsync();
        if ( async != null )
            {
            double totalRead = Double.NaN;
            double totalWrite = Double.NaN;
            double totalCopy = Double.NaN;
            for( int i=0; i<async.length; i++ )
                {
                if ( async[i] != null )
                    {
                    switch( i )
                        {
                        case TOTAL_READ_ID:
                            totalRead = async[i].current;
                            break;
                        case TOTAL_WRITE_ID:
                            totalWrite = async[i].current;
                            break;
                        case TOTAL_COPY_ID:
                            totalCopy = async[i].current;
                            break;
                        }
                    }
                }
            // test results to log
            msg = String.format
                ( "Total scenario %s include files cycle overhead:\r\n" +
                  "Read = %.3f , Write = %.3f , Copy = %.3f\r\n", 
                  unitsString, totalRead, totalWrite, totalCopy );
            logHelper( msg );
            
            // IO scenario options to log
            msg = panel.reportIOscenario();
            logHelper( msg );
            
            // update medians in the openable table
            tableMedianHelper( async );
            
            // sequental results table to log
            msg = "--- Measurements ---\r\n";
            StringBuilder sb = new StringBuilder( msg );
            ReportTableModel rtm = new ReportTableModel( childTableModel );
            tableToStringHelper( rtm, sb );
            logHelper( sb.toString() );
            
            // summary table to log
            msg = "--- Summary ---\r\n";
            sb = new StringBuilder( msg );
            tableToStringHelper( table, sb );
            logHelper( sb.toString() );
            }
        // childDrawModel.stopModel();
        /*
        Done, restore GUI state
        */
        panel.enableGuiAfterRun();
        application.enableGuiAfterRun();
        runStop = false;
        }
    }

/*
Convert table model to text data, return as StringBuilder
*/
private void tableToStringHelper( AbstractTableModel table, StringBuilder sb )
    {
    if ( table != null )
        {
        int columns = table.getColumnCount();
        int rows = table.getRowCount();
        int colSize;
        int[] maxColSizes = new int[columns];
        int maxColSize = 13;
        // Get column names lengths
        for ( int i=0; i<columns; i++ )
            {
            maxColSizes[i] = ( table.getColumnName(i).trim() ).length(); 
            }
        // Get column maximum lengths
        for ( int j=0; j<rows; j++ )
            {
            for ( int i=0; i<columns; i++ )
                {
                colSize = ( ( ( String )
                              ( table.getValueAt( j, i ) ) ).trim() ).length();
                if ( colSize > maxColSizes[i] )
                    {
                    maxColSizes[i] = colSize; 
                    }
                }
            }
        for ( int i=0; i<maxColSizes.length; i++ ) 
            {
            maxColSize += maxColSizes[i];
            }
        // Write table up
        for ( int i=0; i<columns; i++ )
            {
            sb.append( " " );
            sb.append( ( table.getColumnName( i ) ).trim() );
            sb.append( " " );
            colSize = maxColSizes[i] - 
                    ( table.getColumnName( i ).trim() ).length() + 1;
            for ( int k=0; k<colSize; k++ )
                {
                sb.append( " " );
                }
            }
        // Write horizontal line
        sb.append("\r\n" );
        for ( int i=0; i<maxColSize; i++ )
            {
            sb.append( "-" );
            }
        sb.append("\r\n" );
    // Write table content
    for (int j=0; j<rows; j++)         // this cycle for rows
        {
        for (int i=0; i<columns; i++)  // this cycle for columns
            {
            sb.append( " " );
            sb.append( ((String)( table.getValueAt( j, i ) )).trim() );
            sb.append( " " );
            colSize = maxColSizes[i] - 
                ( ( ( String )table.getValueAt( j, i )).trim() ).length() + 1;
            for ( int k=0; k<colSize; k++ )
                {
                sb.append( " " );
                }
            }
            sb.append( "\r\n" );
        }
        // Write horizontal line
        for ( int i=0; i<maxColSize; i++ )
            {
            sb.append( "-" );
            }
        sb.append( "\r\n\r\n" );
        }
    }


private void logHelper( String s )
    {
    if ( childLog != null )
        {
        if ( s != null )
            {
            childLog.write( s );
            }
        childLog.repaint();
        childLog.revalidate();
        }
    }

private void tableHelper( StateSync sync )
    {
    if ( ( childTable != null )&&( childTableModel != null ) )
        {
        if ( sync != null )
            {
            childTableModel.notifySync( sync );
            }
        childTable.repaint();
        childTable.revalidate();
        }
    }

private void tableMedianHelper( StateAsync[] async )
    {
    if ( ( childTable != null )&&( childTableModel != null ) )
        {
        if ( async != null )
            {
            childTableModel.notifyMedians( async );
            }
        }
    tableHelper( null );
    }

private void drawHelper( BigDecimal[] bd )
    {
    if ( ( childDraw != null )&&( childDrawModel != null ) )
        {
        if ( bd != null )
            {
            childDrawModel.updateValue( bd );
            childDrawModel.rescaleYmax();
            }
        childDraw.repaint();
        childDraw.revalidate();
        }
    }

}