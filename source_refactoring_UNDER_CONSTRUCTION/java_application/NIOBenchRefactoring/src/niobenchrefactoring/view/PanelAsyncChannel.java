/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Sub-panel for Java NIO Asynchronous Channels benchmark scenario.
One of sub-panels at NIOBench GUI application main window frame
with tabbed sub-panels.
*/

package niobenchrefactoring.view;

import java.awt.event.*;
import niobenchrefactoring.model.*;

public class PanelAsyncChannel extends PanelChannel  // ApplicationPanel
{
private final TableChannel tableModel = new TableAsyncChannel();

@Override String getTabName()
    { 
    return "NIO async channels"; 
    }

@Override public TableChannel getTableModel() 
    {
    return tableModel; 
    }

public PanelAsyncChannel( Application application )
    {
    super( application );
    }

/*
Additional build method, 
not make this operations in constructor because overridable warnings.
*/
@Override void build()
    {
    super.build();
    /*
    Add listener for automatically set threads count = files count
    required for this panel.
    */
    ActionListener a = new FileCountOptionListener();
    boxes[ID_FILE_COUNT].addActionListener( a );
    a.actionPerformed( null );  // must be set even if yet no clicks
    }

/*
Customize panel with combo boxes, by restrictions for options settings.
Differrent panels has different options restrictions.
Note thread count disabled for async channel, but threads replicated
per each file for this IO scenario.
*/
@Override void buildRestrictions()
    {
    // set parent class restrictions, PanelChannel.java
    super.buildRestrictions();
    // add restrictions for this panel, text labels for combo boxes
    labels[TEXT_COUNT + ID_BLOCK_SIZE].setEnabled( false );
    labels[TEXT_COUNT + ID_THREAD_COUNT].setEnabled( false );
    labels[TEXT_COUNT + ID_FAST_COPY].setEnabled( false );
    labels[TEXT_COUNT + ID_WRITE_SYNC].setEnabled( false );
    labels[TEXT_COUNT + ID_COPY_SYNC].setEnabled( false );
    labels[TEXT_COUNT + ID_COPY_DELAY].setEnabled( false );
    // disable combo boxes, not used for this panel
    boxes[ID_BLOCK_SIZE].setEnabled( false );
    boxes[ID_THREAD_COUNT].setEnabled( false );
    boxes[ID_FAST_COPY].setEnabled( false );
    boxes[ID_WRITE_SYNC].setEnabled( false );
    boxes[ID_COPY_SYNC].setEnabled( false );
    boxes[ID_COPY_DELAY].setEnabled( false );
    // set block size not available
    boxes[ID_BLOCK_SIZE].removeAllItems();
    boxes[ID_BLOCK_SIZE].addItem( " " + ITEMS_NOT_AVAILABLE );
    }

/*
JComboBox listener for "File" (file count) option, it required because
threads count for asynchronous channel is equal to files count, one
thread per file automatically and fixed.
*/
private class FileCountOptionListener implements ActionListener
    {
    @Override public void actionPerformed ( ActionEvent e )
        {
        Object ob = boxes[ID_FILE_COUNT].getSelectedItem();
        if ( ob instanceof String )
            {
            boxes[ID_THREAD_COUNT].removeAllItems();
            boxes[ID_THREAD_COUNT].addItem( ob );
            }
        }
    }

/*
Build IO scenario with options settings, defined in this panel
*/
@Override public IOscenario buildIOscenario()
    {
    IOscenario ios = new IOscenarioAsyncChannel
        ( // String pathSrc, String prefixSrc, String postfixSrc,
          optionSourcePath(),      null, null,
          // String pathDst, String prefixDst, String postfixDst,
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
