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
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import niobenchrefactoring.model.TableChannel;
import niobenchrefactoring.view.Application;
import niobenchrefactoring.view.ApplicationPanel;
import opendraw.OpenDraw;
import openlog.OpenLog;
import opentable.OpenTable;

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
        // initializing GUI global objects
        TableChannel summaryTable = null;
        ApplicationPanel panel = application.getSelectedPanel();
        if ( panel != null )
            {
            summaryTable = panel.getTableModel();
            }
        OpenLog log = application.getChildLog();
        OpenTable table = application.getChildTable();
        OpenDraw draw = application.getChildDraw();
        
        // start cycle for interpreting strings of text report
        StringBuilder sb = new StringBuilder
            ( "Log data now loaded from text report file.\r\n" );
        int n = reportStrings.size();
        for( int i=0; i<n; i++ )
            {
            String s = ( reportStrings.get( i ) ).trim();
            sb.append( "\r\n" );
            sb.append( s );
            }
        
        if ( log != null )
            {
            log.overWrite( sb.toString() );
            log.repaint();
            log.revalidate();
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
    
}


/*

        // initializing GUI global objects
        TableChannel summaryTable = null;
        String[][] summaryStrings = null;
        boolean[] summaryMatches = null;
        int summaryRows = 0;
        int summaryColumns = 1;
        ApplicationPanel panel = application.getSelectedPanel();
        if ( panel != null )
            {
            summaryTable = panel.getTableModel();
            }
        OpenLog log = application.getChildLog();
        OpenTable table = application.getChildTable();
        OpenDraw draw = application.getChildDraw();
        
        // get compare patterns for summary table
        if ( summaryTable != null )
            {
            summaryRows = summaryTable.getRowCount();
            summaryColumns = summaryTable.getColumnCount();
            summaryStrings = new String[summaryRows][summaryColumns];
            summaryMatches = new boolean[summaryRows];
            for( int i=0; i<summaryRows; i++ )
                {
                for( int j=0; j<summaryColumns; j++ )
                    {
                    if ( j == 0 )
                        {
                        summaryStrings[i][j] = 
                                ( summaryTable.getValueAt( i, j ) ).trim();
                        }
                    else
                        {
                        summaryStrings[i][j] = "?";
                        }
                    }
                }
            }
        // start cycle for interpreting strings of text report
        StringBuilder sb = new StringBuilder
            ( "Log data now loaded from text report file.\r\n" );
        int n = reportStrings.size();
        for( int i=0; i<n; i++ )
            {
            String s = ( reportStrings.get( i ) ).trim();
            // provide data for summary table at main window by load result
            for( int j=0; j<summaryRows; j++ )
                {
                String s0 = ( summaryStrings[j][0] ).trim();
                if ( ( s.startsWith( s0 ) )       &&
                     ( s.length() > s0.length() ) &&
                     ( ! summaryMatches[j] ) )
                    {
                    String s1 = s.substring( s0.length() );
                    String[] words = s1.split( " " );
                    int matchCount = 0;
                    String[] matchStore = new String[summaryColumns - 1];
                    for ( String word : words ) 
                        {
                        String s2 = word.trim();
                        if ( ( s2.length() > 1 ) &&
                             ( Character.isDigit( s2.charAt( 0 ) ) ) )
                            {
                            matchStore[matchCount] = s2;
                            matchCount++;
                            
                            System.out.println( s2 );
                            
                            }
                        if ( matchCount == matchStore.length )
                            {
                            summaryMatches[j] = true;
                            System.arraycopy( matchStore, 0, 
                                              summaryStrings[j], 1,
                                              matchCount );
                            }
                        }
                    if ( summaryMatches[j] )
                        break;
                    }
                }
            // provide data for text log openable window by load result
            sb.append( "\r\n" );
            sb.append( s );
            // provide data for table openable window by load result
            
            // provide data for performance draw. openable win. by load result
            
            }
        // cycle done, update summary table at main window by load result
        
        // update text log openable window by load result
        if ( log != null )
            {
            log.overWrite( sb.toString() );
            log.repaint();
            log.revalidate();
            }
        // update results table openable window by load result
        
        // update performance drawings openable window by load result

*/