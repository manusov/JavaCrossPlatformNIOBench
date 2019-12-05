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
import niobenchrefactoring.view.Application;

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
    CopyOnWriteArrayList<String> report = null;
    if( select == JFileChooser.APPROVE_OPTION )
        {
        String filePath = CHOOSER.getSelectedFile().getPath();
        report = new CopyOnWriteArrayList<>();
                File file = new File( filePath );
        try ( FileReader fileReader = 
                new FileReader( file ); 
              BufferedReader bufferedReader = 
                new BufferedReader( fileReader ); )
            {
            report.clear();
            String line;
            while ( ( line = bufferedReader.readLine() ) != null )
                report.add( line );
            loaded = true;
            }
        catch ( Exception e1 )
            {
            loaded = false;
            }
        }
    // interpreting list of strings, if loaded successfully
    if ( ( loaded ) && ( report != null ) )
        {
            
/*

TODO. UNDER CONSTRUCTION, ADAPTATION FROM MPE SHELL.
            
        ReportToGuiListener listener = new ReportToGuiListener( mglst );
        OpStatus status = new OpStatus( true, "OK" );
        listener.dataHandler( report, status );  // report parsing
        // set progress indicator to 100 percents
        JProgressBar progressBar = mglst.getMpeGui().getProgressBar();
        DefaultBoundedRangeModel progressModel = ( DefaultBoundedRangeModel )
            progressBar.getModel();
        ActionRun ar = mglst.getMpeGui().getTaskShell();
        ar.progressUpdate( progressModel, progressBar, 100 );
        // re-initializing GUI (combo boxes) by system information,
        // extracted from loaded report
        mglst.getMpeGui().updateGuiBySysInfo();
*/

        
            
            
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
