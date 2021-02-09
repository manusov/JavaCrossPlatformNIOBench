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
import static niobenchrefactoring.model.IOscenario.*;
import static niobenchrefactoring.model.IOscenarioNative.*;
import niobenchrefactoring.model.*;
import niobenchrefactoring.view.*;
import opendraw.*;
import openlog.OpenLog;
import opentable.*;

public class HandlerRun extends Handler
{
private boolean runStop = false;
private boolean interrupt = false;
private boolean error = false;
private Thread threadRun;
private String unitsString;
private final static int ASYNC_PAUSE = 10;  // milliseconds

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
                {  // test RUNNING by this button press when test no running
                interrupt = false;
                runStop = true;
                threadRun = new ThreadRun( panel );
                threadRun.start();
                }
            else
                {  // test STOPPED by this button press when test running
                interrupt = true;
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
        childDrawModel.blankModel();
        int fileCount = panel.optionFileCount();
        if ( fileCount > 0 )
            {
            childTable.blankTable( fileCount );
            childDrawModel.rescaleXmax( fileCount );
            }
        logHelper( null );
        tableHelper( (StateSync) null );
        drawHelper( null, false );
        /*
        Initializing variables
        */
        currentPercentage = 0.0;
        int rwMode = panel.optionRwMode();
        int deleteAddend = 1;
        int phaseCount = 3;             // write + copy + read = 3 phases
        if ( panel instanceof PanelAsyncChannel )
            {
            phaseCount = 2;             // without copy for Async. Channel
            }
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
        boolean tripleDraw = ( rwMode == RW_SINGLE_5 ) || 
                             ( rwMode == RW_SINGLE_1 );
        addendPercentage = 
            ACTIVE_PERCENTAGE / ( fileCount * phaseCount + deleteAddend );
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
                // clear count if current line (read, write or copy) done
                if ( sync.phaseID != previousID )
                    {
                    previousID = sync.phaseID;
                    if ( ! tripleDraw )
                        {
                        childDrawModel.resetCount();
                        }
                    }
                // select read, write or copy drawings update
                switch ( sync.phaseID )
                    {
                    case READ_ID:
                        bd[1] = new BigDecimal( sync.current );
                        drawHelper( bd, true  );
                        break;
                    case WRITE_ID:
                        bd[2] = new BigDecimal( sync.current );
                        drawHelper( bd, ! tripleDraw );
                        break;
                    case COPY_ID:
                        bd[3] = new BigDecimal( sync.current );
                        drawHelper( bd, ! tripleDraw );
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
            // check termination, normal and interrupted
            if ( ! ios.isAlive() )
                postCount--;
            // time pause for prevent over-utilization system at wait cycle
            StatusEntry delayStatus = delay( ASYNC_PAUSE );
            // check interrupted by user,
            // note delay routine can "eat" interrupt status, 
            // required check status flag
            if ( isInterrupted() || ( ! delayStatus.flag ) )
                {
                ios.interrupt();
                }
            }
        /*
        Test main cycle end, check errors and interrupt conditions,
        no visual messages if error, current error message used
        */
        application.updateProgress( 100 );
        if ( ( ! error ) && ( ! interrupt ) )
            {  // if no errors and not interrupted
            application.updateOperationString
                ( "Test complete.", COMPLETE_ID );
            }
        else if ( ! error )
            {  // if no errors and interrupted
            application.updateOperationString
                ( "Test interrupted.", COMPLETE_ID );
            }
        /*
        Send benchmark done message with time to text log, get milliseconds
        */
        date = new Date();
        long t2 = date.getTime();
        double seconds = ( t2 - t1 ) / 1000.0;
        msg = String.format
            ( "--- Benchmark done at %s ---\r\n", date.toString() );
        String msgDuration;
        if ( ( seconds > 0 )&&( seconds < Integer.MAX_VALUE ) )
            {
            int d = (int)seconds;
            msgDuration = String.format
                ( "Duration include service time is" + 
                  " %02d:%02d:%02d (%d seconds)", 
                  d / 3600, d / 60 % 60, d % 60, d );
            }
            else
            {
            msgDuration = "Duration time measurement FAILED";
            }
        logHelper( msg + msgDuration + "\r\n" );
        /*
        Get integral time measurement results and write to text log.
        Mark medians in the statistics table.
        */
        StateAsync[] async = ios.getAsync();
        if ( async != null )
            {
            // update table
            tableHelper( async );
            // initializing parameters for log
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
            ReportTableModel rtm = new ReportTableModel
                ( childTableModel, application.getPanelType() );
            tableToStringHelper( rtm, sb );
            logHelper( sb.toString() );
            // summary table to log
            msg = "--- Summary ---\r\n";
            sb = new StringBuilder( msg );
            tableToStringHelper( table, sb );
            logHelper( sb.toString() );
            }
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

private void tableHelper( StateAsync[] async )
    {
    if ( ( childTable != null )&&( childTableModel != null ) )
        {
        if ( async != null )
            {
            childTableModel.notifyAsync( async );
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
    tableHelper( (StateSync) null );
    }

private void drawHelper( BigDecimal[] bd, boolean increment )
    {
    if ( ( childDraw != null )&&( childDrawModel != null ) )
        {
        if ( bd != null )
            {
            childDrawModel.updateValue( bd, increment );
            childDrawModel.rescaleYmax();
            }
        childDraw.repaint();
        childDraw.revalidate();
        }
    }
}
