/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Log" button: open log window.
*/

package niobenchrefactoring.controller;

import java.awt.event.ActionEvent;
import niobenchrefactoring.view.Application;
import openlog.OpenLog;

public class HandlerLog extends Handler
{
private final OpenLog childLog;
    
public HandlerLog( Application application )
    {
    super( application );
    this.childLog = application.getChildLog();
    }
    
@Override public void actionPerformed( ActionEvent e )
    {
    if ( childLog != null )
        childLog.open();
    }
}
