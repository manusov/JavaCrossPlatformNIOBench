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
import javax.swing.JFrame;
import javax.swing.JTextField;
import niobenchrefactoring.resources.PAL;

public class HandlerSourcePath extends Handler
{
private final String name;
private final JTextField field;
public PAL pal;
private final static JFileChooser CHOOSER = new JFileChooser();

public HandlerSourcePath
        ( JFrame parentFrame, String name, JTextField field  )
    {
    super( parentFrame );
    this.name = name;
    this.field = field;
    }

@Override public void actionPerformed( ActionEvent e )
    {
    String s1 = field.getText();
    // Setup component for file selection
    CHOOSER.setDialogTitle( name );
    CHOOSER.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
    CHOOSER.setAcceptAllFileFilterUsed( false );
    // Show dialogue window
    int res = CHOOSER.showOpenDialog( null );
    // Interpreting file choosing results
    if( res == JFileChooser.APPROVE_OPTION )
        { 
        String s2 = "" + CHOOSER.getSelectedFile();
        s1 = s2;
        int i=s1.length()-1;
        char a = s1.toCharArray()[i];
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
    field.setText( s1 );
    field.repaint();
    }
}
