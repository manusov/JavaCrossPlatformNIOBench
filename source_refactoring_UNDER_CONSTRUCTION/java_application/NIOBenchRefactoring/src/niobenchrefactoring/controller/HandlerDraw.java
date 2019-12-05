/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Draw" button: open draw charts window, Y=F(X).
*/

package niobenchrefactoring.controller;

import java.awt.event.ActionEvent;
import niobenchrefactoring.view.Application;
import opendraw.OpenDraw;

public class HandlerDraw extends Handler
{
private final OpenDraw childDraw;    
    
public HandlerDraw( Application application )
    {
    super( application );
    this.childDraw = application.getChildDraw();
    }

@Override public void actionPerformed( ActionEvent e )
    {
    if ( childDraw != null )
        childDraw.open();
    }
    
}
