/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Draw" button: open draw charts window, Y=F(X).
*/

package niobenchrefactoring.controller;

import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import opendraw.OpenDraw;

public class HandlerDraw extends Handler
{
private final OpenDraw childDraw;    
    
public HandlerDraw( JFrame parentFrame, OpenDraw childDraw )
    {
    super( parentFrame );
    this.childDraw = childDraw;
    }

@Override public void actionPerformed( ActionEvent e )
    {
    childDraw.open();
    }
    
}
