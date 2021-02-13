/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Sub-panel for File Archivation benchmark scenario.
Note for archivation used java built-in functionality.
One of sub-panels at NIOBench GUI application main window frame
with tabbed sub-panels.
*/

package niobenchrefactoring.view;

import niobenchrefactoring.model.*;

class PanelArchives extends PanelChannel  // ApplicationPanel
{
private final TableChannel tableModel = new TableArchives();

@Override String getTabName() 
    {
    return "Zip archives"; 
    }

@Override public TableChannel getTableModel() 
    {
    return tableModel; 
    }

public PanelArchives( Application application )
    {
    super( application );
    }

/*
Customize panel with combo boxes, by restrictions for options settings.
Differrent panels has different options restrictions.
*/
@Override void buildRestrictions()
    {
    // set parent class restrictions, PanelChannel.java
    super.buildRestrictions();
    // add restrictions for this panel, text labels for combo boxes
    labels[TEXT_COUNT + ID_FAST_COPY].setEnabled( false );
    labels[TEXT_COUNT + ID_WRITE_SYNC].setEnabled( false );
    labels[TEXT_COUNT + ID_COPY_SYNC].setEnabled( false );
    labels[TEXT_COUNT + ID_READ_DELAY].setEnabled( false );
    labels[TEXT_COUNT + ID_WRITE_DELAY].setEnabled( false );
    labels[TEXT_COUNT + ID_COPY_DELAY].setEnabled( false );
    // combo boxes
    boxes[ID_FAST_COPY].setEnabled( false );
    boxes[ID_WRITE_SYNC].setEnabled( false );
    boxes[ID_COPY_SYNC].setEnabled( false );
    boxes[ID_READ_DELAY].setEnabled( false );
    boxes[ID_WRITE_DELAY].setEnabled( false );
    boxes[ID_COPY_DELAY].setEnabled( false );
    }

/*
Build IO scenario with options settings, defined in this panel
*/
@Override public IOscenario buildIOscenario()
    {
    IOscenario ios = new IOscenarioArchives
        ( // String pathSrc, String prefixSrc, String postfixSrc,
          optionSourcePath(),      null, null,
          // String pathDst, String prefixDst, String postfixDst,
          optionDestinationPath(), null, null,
          // String pathZip, String prefixZip, String postfixZip,
          optionDestinationPath(), null, null,
          // int fileCount, int fileSize, int blockSize,
          optionFileCount(), optionFileSize(), optionBlockSize(),
          // int threadCount,
          optionThreadCount(),
          // boolean readSync, boolean writeSync, boolean copySync,
          optionReadSync() > 0, optionWriteSync() > 0, optionCopySync() > 0,
          // boolean writeSyncSparse, boolean copySyncSparse, boolean fastCopy, 
          optionWriteSync() > 1, optionCopySync() > 1, optionFastCopy() > 0,
          // int readWriteMode, int addressMode, int dataMode,
          optionRwMode(), optionAddressMode(), optionDataMode(),
          // int readDelay, int writeDelay, int copyDelay,
          optionReadDelay(), optionWriteDelay(), optionCopyDelay(),
          // byte[] dataBlock
          dataBlock );
    return ios;
    }
}
