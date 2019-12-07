/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Parent class for sub-panels, used at NIOBench GUI application
as tabbed sub-panels.
*/

package niobenchrefactoring.view;

import javax.swing.JPanel;
import niobenchrefactoring.model.IOscenario;
import niobenchrefactoring.model.TableChannel;

public abstract class ApplicationPanel extends JPanel
{
public enum SCENARIO { MBPS, IOPS };
SCENARIO scenario = SCENARIO.MBPS;

final Application application;
public ApplicationPanel( Application application )
    {
    this.application = application;
    }

abstract String getTabName();
abstract void build();

abstract public void setDefaults( SCENARIO scenario );
abstract public void clearResults();
abstract public TableChannel getTableModel();

// support "Run" button
abstract public void disableGuiBeforeRun();
abstract public void enableGuiAfterRun();
abstract public String optionSourcePath();
abstract public String optionDestinationPath();
abstract public int optionFileSize();
abstract public int optionBlockSize();
abstract public int optionFileCount();
abstract public int optionThreadCount();
abstract public int optionDataMode();
abstract public int optionAddressMode();
abstract public int optionRwMode();
abstract public int optionFastCopy();
abstract public int optionReadSync();
abstract public int optionWriteSync();
abstract public int optionCopySync();
abstract public int optionReadDelay();
abstract public int optionWriteDelay();
abstract public int optionCopyDelay();

abstract public IOscenario buildIOscenario();
}
