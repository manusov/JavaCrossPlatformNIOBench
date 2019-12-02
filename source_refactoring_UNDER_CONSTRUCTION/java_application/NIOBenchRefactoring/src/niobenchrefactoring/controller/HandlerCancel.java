/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Cancel" button: exit from application. 
*/

package niobenchrefactoring.controller;

import java.awt.event.ActionEvent;
import javax.swing.JFrame;

public class HandlerCancel extends Handler
{
public HandlerCancel( JFrame parentFrame )
    {
    super( parentFrame );
    }

@Override public void actionPerformed( ActionEvent e )
    {
    System.exit( 0 );
    }
}
