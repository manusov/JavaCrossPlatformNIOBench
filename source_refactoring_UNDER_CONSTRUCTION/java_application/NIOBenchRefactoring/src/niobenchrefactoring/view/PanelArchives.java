/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Sub-panel for File Archivation benchmark scenario.
Note for archivation used java built-in functionality.
One of sub-panels at NIOBench GUI application main window frame
with tabbed sub-panels.
*/

package niobenchrefactoring.view;

import javax.swing.JFrame;
import javax.swing.table.AbstractTableModel;
import niobenchrefactoring.model.TableArchives;

class PanelArchives extends ApplicationPanel 
{
@Override String getTabName()                { return "Zip archives";      }
@Override AbstractTableModel getTableModel() { return new TableArchives(); }

public PanelArchives( JFrame parentFrame )
    {
    super( parentFrame );
    }

/*
Additional build method, 
not make this operations in constructor because overridable warnings.
*/
@Override void build()
    {
    }

}
