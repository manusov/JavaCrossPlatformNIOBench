/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Report" button: 
save report with system information and previous benchmarks/tests results.
*/

package niobenchrefactoring.controller;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import niobenchrefactoring.resources.About;
import niobenchrefactoring.view.Application;

public class HandlerReport extends Handler
{
private final static JFileChooser CHOOSER = new JFileChooser();    
private final static String DEFAULT_EXT  = "txt";
private final static String DEFAULT_FILE = "report" + "." + DEFAULT_EXT;
        
public HandlerReport( Application application )
    {
    super( application );
    }
    
@Override public void actionPerformed( ActionEvent e )
    {
    // get objects for save
    String logText = 
        application.getChildLog().getText();

    // initializing file operations context
    CHOOSER.setDialogTitle( "SAVE REPORT - select directory" );
    FileNameExtensionFilter filter = 
        new FileNameExtensionFilter ( "Text files" , DEFAULT_EXT );
    CHOOSER.setFileFilter( filter );
    CHOOSER.setFileSelectionMode( JFileChooser.FILES_ONLY );
    CHOOSER.setSelectedFile( new File( DEFAULT_FILE ) );
    // (re)start dialogue
    boolean inDialogue = true;
    while( inDialogue )
        {
        int select = CHOOSER.showSaveDialog( application );
        // save file
        if( select == JFileChooser.APPROVE_OPTION )
            {
            String filePath = CHOOSER.getSelectedFile().getPath();
            int option = JOptionPane.YES_OPTION;
            // check file exist and warning message
            File file = new File( filePath );
            if( file.exists() == true )
                {
                option = JOptionPane.showConfirmDialog
                    ( null, 
                    "File exist: " + filePath + "\noverwrite?" , "REPORT" ,
                    JOptionPane.YES_NO_CANCEL_OPTION ,
                    JOptionPane.WARNING_MESSAGE );  // or QUESTION_MESSAGE
                }
            // Select operation by user selection
            if ( ( option == JOptionPane.NO_OPTION  ) |
                 ( option == JOptionPane.CLOSED_OPTION ) )
                { 
                continue; 
                }
            if ( option == JOptionPane.CANCEL_OPTION ) 
                { 
                inDialogue = false;
                continue; 
                }
            
            // continue prepare for save file.
            StringBuilder data = new StringBuilder ( "" );
            // add product and copyright information to report
            data.append( About.getLongName() );
            data.append( "\r\n" );
            data.append( About.getVendorName() );
            data.append( "\r\n" );
            data.append( About.getWebSite() );
            data.append( "\r\n" );
            data.append( "Report file." );
            data.append( "\r\n\r\n" );

            // brief report, text same as openable full report
            if ( logText != null )
                {
                int n = logText.length();
                boolean b = false;
                for( int i=0; i<n; i++ )
                    {
                    // sequences 0Ah/0Dh, 0Dh/0Ah, 0Dh, 0Ah
                    // must be replaced to sequence 0Dh/0Ah
                    char c = logText.charAt( i );
                    if ( ( c == '\r' ) || ( c == '\n' ) )
                        {
                        if ( b )
                            {
                            b = false;
                            }
                        else
                            {
                            data.append( "\r\n" );
                            b = true;
                            }
                        }
                    else
                        {
                        data.append( c );
                        b = false;
                        }
                    }
                }
            
            // save text report
            String fileData = data.toString();
            int status = 0;
            try ( FileWriter writer = new FileWriter( filePath, false ) )
                {
                writer.write( fileData );
                writer.flush(); 
                }
            catch( Exception ex )
                {
                status = 1; 
                }
            // message box after report saved
            if ( status == 0 )  
                {
                JOptionPane.showMessageDialog
                    ( application, "Report saved: " + filePath, "REPORT",
                      JOptionPane.WARNING_MESSAGE ); 
                }
            else
                {
                JOptionPane.showMessageDialog
                    ( application, "Write report failed", "ERROR",
                      JOptionPane.ERROR_MESSAGE ); 
                }
            inDialogue = false;
            }  
        else
            { 
            inDialogue = false; 
            }
        }
    }
}
