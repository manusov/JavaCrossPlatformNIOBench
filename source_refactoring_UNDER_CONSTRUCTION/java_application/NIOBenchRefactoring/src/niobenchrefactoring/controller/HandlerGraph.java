/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Graph" button: 
save graphics image after previous drawings benchmarks/tests results.
*/

package niobenchrefactoring.controller;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import niobenchrefactoring.view.Application;

public class HandlerGraph extends Handler
{
private final static JFileChooser CHOOSER = new JFileChooser();
private final static String DEFAULT_EXT  = "png";
private final static String DEFAULT_FILE = "picture" + "." + DEFAULT_EXT;
    
public HandlerGraph( Application application )
    {
    super( application );
    }
    
@Override public void actionPerformed( ActionEvent e )
    {
    // get objects for save
    JPanel savedPanel = 
            application.getChildDraw().getController().getView().getPanel();
    // generate graphics image from drawings GUI panel
    int w = savedPanel.getWidth();
    int h = savedPanel.getHeight();
    // check for run save image when drawings window opened
    if ( ( w <= 0 ) || ( h <= 0 ) )
        {
        JOptionPane.showMessageDialog
            ( application,
              "Drawings window must\r\nbe opened for write image\r\n" +
              "first press \" Draw > \"",
              "ERROR",
              JOptionPane.ERROR_MESSAGE ); 
        return;  // return if image not available
        }
    
    // image available, initializing file operations context
    CHOOSER.setDialogTitle( "SAVE GRAPHICS - select directory" );
    FileNameExtensionFilter filter = 
        new FileNameExtensionFilter ( "Picture files" , DEFAULT_EXT );
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
                      "File exist: " + filePath + "\noverwrite?" , "IMAGE" ,
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
            
            BufferedImage image = 
                new BufferedImage( w, h, BufferedImage.TYPE_INT_RGB );
            Graphics2D g = image.createGraphics();
            g.setClip( 0, 0, w, h );
            savedPanel.paint( g );
            g.drawImage( image, w, h, savedPanel );
            g.dispose();
            // save graphics image to file
            int status = 0;
            try
                {
                ImageIO.write( image, DEFAULT_EXT, new File( filePath ) );
                }
            catch( IOException ex )
                {
                status = 1; 
                }
            // message box after report saved
            if ( status == 0 )  
                {
                JOptionPane.showMessageDialog
                    ( savedPanel, "Image saved: " + filePath, "IMAGE",
                      JOptionPane.WARNING_MESSAGE ); 
                }
            else
                {
                JOptionPane.showMessageDialog
                    ( savedPanel, "Write image failed", "ERROR",
                      JOptionPane.ERROR_MESSAGE ); 
                }
            inDialogue = false;
            }  
        else
            { 
            inDialogue = false; 
            }
        }   // End of save dialogue cycle
    }
}
