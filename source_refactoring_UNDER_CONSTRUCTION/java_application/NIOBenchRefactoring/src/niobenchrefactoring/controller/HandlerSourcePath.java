/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Browse" button for source path: select path.
*/

package niobenchrefactoring.controller;

import java.awt.event.ActionEvent;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import niobenchrefactoring.resources.PAL;
import niobenchrefactoring.view.Application;

public class HandlerSourcePath extends Handler
{
private final PAL pal;
private final String name;
private final JTextField field1, field2;
private final static JFileChooser CHOOSER = new JFileChooser();

public HandlerSourcePath( Application application, String name, 
                          JTextField field1, JTextField field2 )
    {
    super( application );
    this.pal = application.getPAL();
    this.name = name;
    this.field1 = field1;
    this.field2 = field2;
    }

@Override public void actionPerformed( ActionEvent e )
    {
    String s1 = field1.getText();
    // Setup component for file selection
    CHOOSER.setDialogTitle( name );
    CHOOSER.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
    CHOOSER.setAcceptAllFileFilterUsed( false );
    // Show dialogue window
    int selection = CHOOSER.showOpenDialog( null );
    // Interpreting file choosing results
    if( selection == JFileChooser.APPROVE_OPTION )
        { 
        String s2 = "" + CHOOSER.getSelectedFile();
        s1 = s2;
        char a = s1.toCharArray()[s1.length() - 1];
        if ( pal != null )
            {
            int b = pal.getBinaryType();
            if ( ( ( b == 0 )|( b == 1 ) )&( a != '\\' ) )
                {
                s1 = s1 + "\\"; 
                }
            if ( ( ( b == 2 )|( b == 3 ) )&( a != '/' ) )
                {
                s1 = s1 + "/";  
                }
            }
        }
    field1.setText( s1 );
    field1.repaint();
    if ( field2 != null )
        {
        field2.setText( s1 );
        field2.repaint();
        }
    }
}
