/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Sub-panel for Java NIO Channels and Buffers benchmark scenario.
One of sub-panels at NIOBench GUI application main window frame
with tabbed sub-panels.
*/

package niobenchrefactoring.view;

import javax.swing.table.AbstractTableModel;
import niobenchrefactoring.model.TableChannel;

public class PanelChannel extends ApplicationPanel 
{
@Override String getTabName()                { return "NIO channels";     }
@Override AbstractTableModel getTableModel() { return new TableChannel(); }

}
