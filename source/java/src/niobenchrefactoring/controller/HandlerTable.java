/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Table" button: open statistics table window.
*/

package niobenchrefactoring.controller;

import java.awt.event.ActionEvent;
import niobenchrefactoring.view.Application;
import opentable.OpenTable;

public class HandlerTable extends Handler
{
private final OpenTable childTable;
    
public HandlerTable( Application application )
    {
    super( application );
    this.childTable = application.getChildTable();
    }
    
@Override public void actionPerformed( ActionEvent e )
    {
    if ( childTable != null )
        childTable.open();
    }
}
