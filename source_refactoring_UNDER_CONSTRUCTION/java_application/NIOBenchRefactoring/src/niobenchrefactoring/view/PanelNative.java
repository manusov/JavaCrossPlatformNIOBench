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

import niobenchrefactoring.model.*;
import static niobenchrefactoring.model.IOscenarioNative.*;

class PanelNative extends PanelChannel
{
private final TableChannel tableModel = new TableNative();
/*
Overrides for some defaults at "Native OS API" panel
*/
private final static int DEFAULT_FILE_SIZE_MBPS_NATIVE  = 13;
private final static int DEFAULT_BLOCK_SIZE_MBPS_NATIVE = 12;
private final static int DEFAULT_READ_SYNC_NATIVE       = 1;
/*
special support for "R/W" option at "Native OS API" panel
*/
private final static int DEFAULT_READ_WRITE_NATIVE_MBPS = 0;
private final static int DEFAULT_READ_WRITE_NATIVE_IOPS = 1;
private final static int[] NATIVE_RW_ENCODER = 
    { RW_GROUP_5, RW_GROUP_1, RW_SINGLE_5, RW_SINGLE_1 };
private final static String SET_READ_WRITE_NATIVE[] =
    { "File group 5 repeats"  , "File group no repeats"  ,
      "Single file 5 repeats" , "Single file no repeats" };
/*
special support for "Write sync", "Copy sync" options at "Native OS API" panel
*/
private final static int DEFAULT_WRITE_SYNC_NATIVE      = 1;
private final static int DEFAULT_COPY_SYNC_NATIVE       = 1;
private final static String SET_WRITE_SYNC_NATIVE[] =
    { SET_WRITE_SYNC[0], SET_WRITE_SYNC[1]  };
private final static String SET_COPY_SYNC_NATIVE[] =
    { SET_COPY_SYNC[0], SET_COPY_SYNC[1]  };

/*
Base functionality
*/
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
Customize panel with combo boxes, by restrictions for options settings.
Differrent panels has different options restrictions.
*/
@Override void buildRestrictions()
    {
    // labels with combo names, located left
    labels[TEXT_COUNT + ID_ADDRESS_PATTERN].setEnabled( false );
    // labels[TEXT_COUNT + ID_READ_WRITE].setEnabled( false );
    labels[TEXT_COUNT + ID_THREAD_COUNT].setEnabled( false );
    labels[TEXT_COUNT + ID_FAST_COPY].setEnabled( false );
    // combo boxes
    boxes[ID_ADDRESS_PATTERN].setEnabled( false );
    // boxes[ID_READ_WRITE].setEnabled( false );
    boxes[ID_THREAD_COUNT].setEnabled( false );
    boxes[ID_FAST_COPY].setEnabled( false );
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
    if ( scenario == SCENARIO.MBPS )
        {  // this settings for MBPS scenario, not for IOPS scenario
        boxes[ID_FILE_SIZE].setSelectedIndex( DEFAULT_FILE_SIZE_MBPS_NATIVE );
        boxes[ID_BLOCK_SIZE].setSelectedIndex( DEFAULT_BLOCK_SIZE_MBPS_NATIVE );
        helperComboString( ID_READ_WRITE, SET_READ_WRITE_NATIVE, 
                           DEFAULT_READ_WRITE_NATIVE_MBPS );
        }
    else
        {
        helperComboString( ID_READ_WRITE, SET_READ_WRITE_NATIVE, 
                           DEFAULT_READ_WRITE_NATIVE_IOPS );
        }
    // this options sets + settings assignment for both MBPS and IOPS scenarios
    boxes[ID_WRITE_SYNC].removeAllItems();
    helperComboString( ID_WRITE_SYNC, SET_WRITE_SYNC_NATIVE, 
                       DEFAULT_WRITE_SYNC_NATIVE );
    boxes[ID_COPY_SYNC].removeAllItems();
    helperComboString( ID_COPY_SYNC, SET_COPY_SYNC_NATIVE, 
                       DEFAULT_COPY_SYNC_NATIVE );
    // this settings for both MBPS and IOPS scenarios
    boxes[ID_READ_SYNC].setSelectedIndex( DEFAULT_READ_SYNC_NATIVE );
    }

@Override public int optionRwMode()
    {
    int n = super.optionRwMode();
    int m = NATIVE_RW_ENCODER.length;
    if ( n >= m ) n = 0;
    return NATIVE_RW_ENCODER[n];
    }

/*
Build IO scenario with options settings, defined in this panel
*/
@Override public IOscenario buildIOscenario()
    {
    IOscenario ios = new IOscenarioNative
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
          // boolean dataSparse, boolean fastCopy, 
          optionWriteSync() > 0, optionFastCopy() > 0,
          // int readWriteMode, int addressMode, int dataMode,
          optionRwMode(), optionAddressMode(), optionDataMode(),
          // int readDelay, int writeDelay, int copyDelay,
          optionReadDelay(), optionWriteDelay(), optionCopyDelay(),
          // byte[] dataBlock
          dataBlock,
          application.getPAL() );
    return ios;
    }
}
