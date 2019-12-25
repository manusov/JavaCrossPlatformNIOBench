/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Sub-panel for Native OS API files operations benchmark scenario.
One of sub-panels at NIOBench GUI application main window frame
with tabbed sub-panels.
*/

package niobenchrefactoring.view;

import niobenchrefactoring.model.IOscenario;
import niobenchrefactoring.model.TableChannel;
import niobenchrefactoring.model.TableNative;

class PanelNative extends PanelChannel  // ApplicationPanel
{
private final TableChannel tableModel = new TableNative();

@Override String getTabName() 
    {
    return "Native OS API"; 
    }

@Override public TableChannel getTableModel() 
    {
    return tableModel; 
    }

public PanelNative( Application application )
    {
    super( application );
    }

/*
Additional build method, 
not make this operations in constructor because overridable warnings.
*/
/*
@Override void build()
    {
    // reserved, because same as parent panel - PanelChannel.java
    }
*/

/*
Customize panel with combo boxes, by restrictions for options settings.
Differrent panels has different options restrictions.
*/
@Override void buildRestrictions()
    {
    // no restrictions for this panel, all combo boxes active
    }

/*
Public method for initializing at start and re-initializing by buttons:
"Default MBPS" , "Default IOPS".
This method can be called from button handler.
*/
/*
@Override public void setDefaults( SCENARIO scenario )
    {
    // reserved, because same as parent panel - PanelChannel.java    
    }
*/

/*
Public method for clear benchmarks results by button: "Clear".
This method can be called from button handler.
*/
@Override public void clearResults()
    {
    // reserved, because same as parent panel - PanelChannel.java
    // reserved for panel-specific clear, additional to HandlerClear action.
    }

/*
Support "Run" button
*/
/*
// reserved, because same as parent panel - PanelChannel.java
//
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
*/

/*
Build IO scenario with options settings, defined in this panel
*/
@Override public IOscenario buildIOscenario()
    {
    
    return null;
    }

/*
Return text information about options settings at start IO scenario
*/
@Override public String reportIOscenario()
    {
    // reserved, because same as parent panel - PanelChannel.java    
    return "";
    }

}
