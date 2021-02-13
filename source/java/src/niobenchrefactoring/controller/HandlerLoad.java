/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Load" button: 
load report with system information and previous benchmarks/tests results,
can be used for drawings graphical files generation.
*/

package niobenchrefactoring.controller;

import java.awt.event.ActionEvent;
import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import niobenchrefactoring.model.TableChannel;
import niobenchrefactoring.view.*;
import opendraw.*;
import openlog.OpenLog;
import opentable.*;

public class HandlerLoad extends Handler
{
private final static JFileChooser CHOOSER = new JFileChooser();    
private final static String DEFAULT_EXT  = "txt";
private final static String DEFAULT_FILE = "report" + "." + DEFAULT_EXT;
private final static int DOWN_LINES = 5;

public HandlerLoad( Application application )
    {
    super( application );
    }

@Override public void actionPerformed( ActionEvent e )
    {
    // initializing file operations context
    CHOOSER.setDialogTitle( "LOAD REPORT - select directory" );
    FileNameExtensionFilter filter = 
        new FileNameExtensionFilter ( "Text files" , DEFAULT_EXT );
    CHOOSER.setFileFilter( filter );
    CHOOSER.setFileSelectionMode( JFileChooser.FILES_ONLY );
    CHOOSER.setSelectedFile( new File( DEFAULT_FILE ) );
    int select = CHOOSER.showOpenDialog( application );
    // load file, build list of strings
    boolean loaded = false;
    CopyOnWriteArrayList<String> reportStrings = null;
    if( select == JFileChooser.APPROVE_OPTION )
        {
        String filePath = CHOOSER.getSelectedFile().getPath();
        reportStrings = new CopyOnWriteArrayList<>();
                File file = new File( filePath );
        try ( FileReader fileReader = 
                new FileReader( file ); 
              BufferedReader bufferedReader = 
                new BufferedReader( fileReader ); )
            {
            reportStrings.clear();
            String line;
            while ( ( line = bufferedReader.readLine() ) != null )
                reportStrings.add( line );
            loaded = true;
            }
        catch ( Exception e1 )
            {
            loaded = false;
            }
        }
    // interpreting list of strings, if loaded successfully
    if ( ( loaded ) && ( reportStrings != null ) )
        {
        // objects for parameters extract
        int summaryCount = 0;
        String[] summaryKeys = null;
        String[][] summaryValues = null;
        int downCount = 0;
        StatisticsTableModel tableModel = null;
        String[] downKeys = null;
        String[][] downValues = null;
        ArrayList<Measure> measureValues = new ArrayList<>();
        // report type detection by test names strings
        String panelPrefix = "--- ";
        String[] panelsNames = application.getPanelsNames();
        int n = reportStrings.size();
        int m = panelsNames.length;
        boolean b = true;
        for( int i=0; ( i<n && b ); i++ )
            {
            String s = ( reportStrings.get( i ) ).trim();
            if ( s.startsWith( panelPrefix ) )
                {
                s = s.replace( panelPrefix, "" );
                for( int j=0; j<m; j++ )
                    {
                    if ( s.startsWith( panelsNames[j] ) )
                        {  // name string detected, make tab selection
                        application.selectTabByReportId( j );
                        b = false;
                        break;
                        }
                    }
                }
            }
        // report type detection done, initializing GUI global objects
        TableChannel summaryTable = null;
        ApplicationPanel panel = application.getSelectedPanel();
        if ( panel != null )
            {
            summaryTable = panel.getTableModel();
            if ( summaryTable != null )
                {
                int rows = summaryTable.getRowCount();
                int columns = summaryTable.getColumnCount();
                summaryKeys = new String[rows];
                summaryValues = new String[rows][columns - 1];
                for( int i=0; i<rows; i++ )
                    {
                    summaryKeys[i] = summaryTable.getValueAt( i, 0 );
                    }
                }
            }
        // child window = text log
        OpenLog log = application.getChildLog();
        // child window = table
        OpenTable table = application.getChildTable();
        if ( table != null )
            {
            tableModel = table.getTableModel();
            if ( ( tableModel != null ) && 
                 ( tableModel.getRowCount() >= DOWN_LINES ) )
                {
                int rows = tableModel.getRowCount();
                int columns = tableModel.getColumnCount();
                downKeys = new String[DOWN_LINES];
                downValues = new String[DOWN_LINES][columns - 1];
                int j = rows - DOWN_LINES;
                for( int i=0; i<DOWN_LINES; i++ )
                    {
                    downKeys[i] = tableModel.getValueAt( j, 0 );
                    j++;
                    }
                }
            }
        // child window = draw
        OpenDraw draw = application.getChildDraw();
        // start cycle for interpreting strings of text report
        StringBuilder sb = new StringBuilder
            ( "Log data now loaded from text report file.\r\n" );
        for( int i=0; i<n; i++ )
            {
            // raw text store
            String s = reportStrings.get( i );
            sb.append( "\r\n" );
            sb.append( s );
            // summary table store
            if ( ( summaryKeys != null ) &&
                 ( summaryCount < summaryKeys.length ) )
                {
                String[] values = 
                        summaryHelper( s, summaryKeys[summaryCount], 4 );
                if ( ( values != null ) && 
                     ( values.length == summaryValues[summaryCount].length ) )
                    {
                    summaryValues[summaryCount] = values;
                    summaryCount++;
                    }
                }
            // results table down summary rows store
            if ( ( downKeys != null ) && 
                 ( downCount < downKeys.length ) )
                {
                String[] values = 
                        summaryHelper( s, downKeys[downCount], 3 );
                if ( ( values != null ) && 
                     ( values.length == downValues[downCount].length ) )
                    {
                    downValues[downCount] = values;
                    downCount++;
                    }
                
                }
            // results table store, this also used as drawings values store
            Measure measure = measureHelper( s );
            if ( measure != null )
                {
                measureValues.add( measure );
                }
            }
        // update main window summary table by load results
        if ( ( summaryTable != null ) && ( summaryValues != null ) )
            {
            summaryTable.clear();
            for( int i=0; i<summaryValues.length; i++ )
                {
                for( int j=0; j<summaryValues[i].length; j++ )
                    {
                    summaryTable.setValueAt( summaryValues[i][j], i, j+1 );
                    }
                }
            }
        // update openable text log by load results
        if ( log != null )
            {
            log.overWrite( sb.toString() );
            log.repaint();
            log.revalidate();
            }
        // update openable results table by load results
        if ( ( table != null ) && ( tableModel != null ) )
            {
            int measureCount = measureValues.size();
            table.blankTable( measureCount );
            for ( int i=0; i<measureCount; i++ )
                {
                Measure measure = measureValues.get( i );
                String[] s = measure.texts;
                if ( s.length == tableModel.getColumnCount() )
                    {
                    for( int j=0; j<s.length; j++ )
                        {
                        tableModel.setValueAt( measure.texts[j], i, j );
                        if ( ( j > 0 ) && ( measure.medians[j-1] ) )
                            {
                            tableModel.markMedian( i, j );
                            }
                        }
                    }
                }
            int tableCount = tableModel.getRowCount() - DOWN_LINES;
            for( int i=0; i<DOWN_LINES; i++ )
                {
                if ( downValues[i].length == tableModel.getColumnCount() - 1 )
                    {
                    for ( int j=0; j<downValues[i].length; j++ ) 
                        {
                        tableModel.setValueAt
                                ( downValues[i][j], tableCount, j+1 );
                        }
                    tableCount++;
                    }
                }
            table.repaint();
            table.revalidate();
            }
        // update openable performance drawings by load results
        FunctionController fc = draw.getController();
        if ( fc != null )
            {
            FunctionModelInterface fim = fc.getModel();
            if ( fim != null )
                {
                int measureCount = measureValues.size();
                fim.blankModel();
                fim.rescaleXmax( measureCount );
                for( int i=0; i<measureCount; i++ )
                    {
                    Measure measure = measureValues.get( i );
                    BigDecimal[] bd = measure.numbers;
                    bd[0] = bd[0].subtract( BigDecimal.ONE );
                    fim.updateValue( bd, true );
                    fim.rescaleYmax();
                    }
                draw.repaint();
                draw.revalidate();
                }
            }
        
        // message box about report loaded successfully
        JOptionPane.showMessageDialog
            ( application, "Report loaded successfully", "LOAD REPORT",
            JOptionPane.WARNING_MESSAGE ); 
        }
    else if ( select == JFileChooser.APPROVE_OPTION )
        {  // error message
        JOptionPane.showMessageDialog
            ( application, "Load report failed", "ERROR",
            JOptionPane.ERROR_MESSAGE ); 
        }
    }

/*
Helpers
*/

// pattern for detect summary table strings
private final static Pattern SUMMARY_PATTERN = 
        Pattern.compile( "\\W*\\d+(,|\\.)*\\d+\\W*" );

private String[] summaryHelper( String s, String key, int patternLength )
    {
    String[] result = null;
    s = s.trim();
    key = key.trim();
    if ( s.startsWith( key ) )
        {
        s = s.substring( key.length() );
        Matcher findMatcher = SUMMARY_PATTERN.matcher( s );
        ArrayList<String> list = new ArrayList<>();
        while ( findMatcher.find() )
            {
            list.add( findMatcher.group().trim() );
            }
        result = list.toArray( new String[list.size()] );
        }
    // check and return result strings array
    if ( ( result != null )&&( result.length == patternLength ) )
        return result;
    if ( ( result != null )&&( result.length == 2 )&&( patternLength == 3 ) ) 
        {  // special support for read, write present, copy absent (async. ch.)
        String[] s1 = result[1].split( "\\s+" );
        if ( ( s1 != null )&&( s1.length == 2 )&&( s1[1].equals( "-" ) ) )
            return new String[]{ result[0], s1[0], s1[1] };
        else
            return null;
        }
    else
        return null;
    }

/*
This class for extracted measure value, 
note for extracted summary value used String[]    
*/
private class Measure
    {
    final String[]     texts;
    final BigDecimal[] numbers;
    final boolean[]    medians;
    Measure( int n, int m )
        {
        texts   = new String[n];
        numbers = new BigDecimal[n];
        medians = new boolean[m];
        }
    }

// pattern for split string to substrings by spaces
private final static String SPLIT_PATTERN = "\\s+";

// pattern for detect string number, integer value
private final static Pattern INDEX_PATTERN = 
        Pattern.compile( "\\W*\\d+\\W*" );

// pattern for detect measurement results, floating point value
private final static Pattern VALUE_PATTERN = 
        Pattern.compile( "\\W*\\d+(,|\\.)*\\d+\\W*" );

// pattern for detect median marks at measurement results
private final static Pattern MEDIAN_PATTERN = 
        Pattern.compile( "\\W*M\\W*" );

private Measure measureHelper( String s )
    {
    Measure result = null;
    int countNumbers = 0;
    String[] subs = ( s.trim() ).split( SPLIT_PATTERN );
    if ( ( subs != null )&&( subs.length >= 4 )&&( subs.length <= 7 ) )
        {
        Matcher indexMatcher = INDEX_PATTERN.matcher( subs[0] );
        if ( indexMatcher.matches() )
            {
            result = new Measure( 4, 3 );
            result.texts[0] = subs[0];
            result.numbers[0] = new BigDecimal( subs[0] );
            for ( int i=1; i<subs.length; i++ )
                {
                Matcher valueMatcher = VALUE_PATTERN.matcher( subs[i] );
                if ( valueMatcher.matches() )
                    {
                    countNumbers++;
                    result.texts[countNumbers] = subs[i];
                    String s1 = subs[i].replace( ',', '.' );
                    result.numbers[countNumbers] = new BigDecimal( s1 );
                    }
                else
                    {
                    Matcher medianMatcher = MEDIAN_PATTERN.matcher( subs[i] );
                    if ( medianMatcher.matches() )
                        {
                        result.medians[countNumbers - 1] = true;
                        }
                    }
                }
            }
        }
    return result;
    }
}
