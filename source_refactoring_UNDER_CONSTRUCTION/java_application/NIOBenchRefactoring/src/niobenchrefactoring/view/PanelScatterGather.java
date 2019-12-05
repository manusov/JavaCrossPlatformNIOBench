/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Sub-panel for Java NIO Scatter-Gather file IO benchmark scenario.
One of sub-panels at NIOBench GUI application main window frame
with tabbed sub-panels.
*/

package niobenchrefactoring.view;

import javax.swing.table.AbstractTableModel;
import niobenchrefactoring.model.IOscenario;
import niobenchrefactoring.model.TableScatterGather;

class PanelScatterGather extends ApplicationPanel 
{
private final AbstractTableModel tableModel = new TableScatterGather();
@Override String getTabName() { return "NIO scatter-gather"; }
@Override public AbstractTableModel getTableModel() { return tableModel; }

public PanelScatterGather( Application application )
    {
    super( application );
    }

/*
Additional build method, 
not make this operations in constructor because overridable warnings.
*/
@Override void build()
    {
    }

/*
Public method for initializing at start and re-initializing by buttons:
"Default MBPS" , "Default IOPS".
This method can be called from button handler.
*/
@Override public void setDefaults( SCENARIO scenario )
    {
    }

/*
Public method for clear benchmarks results by button: "Clear".
This method can be called from button handler.
*/
@Override public void clearResults()
    {
    // TODO.
    }

/*
Support "Run" button
*/

@Override public void disableGuiBeforeRun()     {              }
@Override public void enableGuiAfterRun()       {              }
@Override public String optionSourcePath()      { return null; }
@Override public String optionDestinationPath() { return null; }
@Override public int optionFileSize()           { return 0;    }
@Override public int optionBlockSize()          { return 0;    }
@Override public int optionFileCount()          { return 0;    }
@Override public int optionThreadCount()        { return 0;    }
@Override public int optionDataMode()           { return 0;    }
@Override public int optionAddressMode()        { return 0;    }
@Override public int optionRwMode()             { return 0;    }
@Override public int optionFastCopy()           { return 0;    }
@Override public int optionReadSync()           { return 0;    }
@Override public int optionWriteSync()          { return 0;    }
@Override public int optionCopySync()           { return 0;    }
@Override public int optionReadDelay()          { return 0;    }
@Override public int optionWriteDelay()         { return 0;    }
@Override public int optionCopyDelay()          { return 0;    }

/*
Build IO scenario with options settings, defined in this panel
*/
@Override public IOscenario buildIOscenario()
    {
    
    return null;
    }

}
