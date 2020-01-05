/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Sub-panel for SNIA SSD-oriented test and benchmark scenario.
One of sub-panels at NIOBench GUI application main window frame
with tabbed sub-panels.
*/

package niobenchrefactoring.view;

import niobenchrefactoring.model.TableChannel;
import niobenchrefactoring.model.TableSSD;

class PanelSSD extends PanelNative // ApplicationPanel 
{
private final TableChannel tableModel = new TableSSD();

@Override String getTabName() 
    {
    return "SSD specific"; 
    }

@Override public TableChannel getTableModel() 
    {
    return tableModel; 
    }

public PanelSSD( Application application )
    {
    super( application );
    }

/*
Public method for initializing at start and re-initializing by buttons:
"Default MBPS" , "Default IOPS".
This method can be called from button handler.
*/
@Override public void setDefaults( SCENARIO scenario )
    {
    super.setDefaults( scenario );
    boxes[ID_READ_WRITE].removeAllItems();
    helperComboString( ID_READ_WRITE,
                       SET_READ_WRITE, DEFAULT_READ_WRITE_SSD );
    }
}
