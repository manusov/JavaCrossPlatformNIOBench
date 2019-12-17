/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Load" button: 
load report with system information and previous benchmarks/tests results,
can be used for drawings graphical files generation.
*/

package niobenchrefactoring.controller;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import niobenchrefactoring.model.TableChannel;
import niobenchrefactoring.view.Application;
import niobenchrefactoring.view.ApplicationPanel;
import opendraw.FunctionController;
import opendraw.FunctionModelInterface;
import opendraw.OpenDraw;
import openlog.OpenLog;
import opentable.OpenTable;
import opentable.StatisticsTableModel;

public class HandlerLoad extends Handler
{
private final static JFileChooser CHOOSER = new JFileChooser();    
private final static String DEFAULT_EXT  = "txt";
private final static String DEFAULT_FILE = "report" + "." + DEFAULT_EXT;

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
            
        // initializing GUI global objects
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
        OpenLog log = application.getChildLog();
        OpenTable table = application.getChildTable();
        if ( table != null )
            {
            tableModel = table.getTableModel();
            if ( ( tableModel != null ) && ( tableModel.getRowCount() >= 4 ) )
                {
                int rows = tableModel.getRowCount();
                int columns = tableModel.getColumnCount();
                downKeys = new String[4];
                downValues = new String[4][columns - 1];
                int j = rows - 4;
                for( int i=0; i<4; i++ )
                    {
                    downKeys[i] = tableModel.getValueAt( j, 0 );
                    j++;
                    }
                }
            }
        OpenDraw draw = application.getChildDraw();
        
        // start cycle for interpreting strings of text report
        StringBuilder sb = new StringBuilder
            ( "Log data now loaded from text report file.\r\n" );
        int n = reportStrings.size();
        for( int i=0; i<n; i++ )
            {
            // raw text store
            String s = ( reportStrings.get( i ) ).trim();
            sb.append( "\r\n" );
            sb.append( s );
            // summary table store
            if ( ( summaryKeys != null ) &&
                 ( summaryCount < summaryKeys.length ) )
                {
                String[] values = summaryHelper( s, summaryKeys[summaryCount] );
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
                String[] values = summaryHelper( s, downKeys[downCount] );
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
            int tableCount = tableModel.getRowCount() - 4;
            for( int i=0; i<4; i++ )
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
                    fim.updateValue( bd );
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

private final static Pattern SUMMARY_PATTERN = 
        Pattern.compile( "\\W*\\d+(,|\\.)*\\d+\\W*" );

private String[] summaryHelper( String s, String key )
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
    if ( ( result != null )&&( result.length == 3 ) )
        return result;
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

private final static String SPLIT_PATTERN = "\\s+";

private final static Pattern INDEX_PATTERN = 
        Pattern.compile( "\\W*\\d+\\W*" );

private final static Pattern VALUE_PATTERN = 
        Pattern.compile( "\\W*\\d+(,|\\.)*\\d+\\W*" );

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
                    result.numbers[countNumbers] = new BigDecimal( subs[i] );
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