/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Table" button: open statistics table window.
*/

package niobenchrefactoring.controller;

import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import opentable.OpenTable;

public class HandlerTable extends Handler
{
private final OpenTable childTable;
    
public HandlerTable( JFrame parentFrame, OpenTable childTable )
    {
    super( parentFrame );
    this.childTable = childTable;
    }
    
@Override public void actionPerformed( ActionEvent e )
    {
    childTable.open();
    }
    
}
