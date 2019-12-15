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

import java.awt.Container;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import niobenchrefactoring.controller.HandlerDestinationPath;
import niobenchrefactoring.controller.HandlerSourcePath;
import niobenchrefactoring.model.IOscenario;
import niobenchrefactoring.model.IOscenarioChannel;
import niobenchrefactoring.model.TableChannel;

public class PanelChannel extends ApplicationPanel 
{
/*
Application Panel common functionality, include defined by parent class    
*/
private final TableChannel tableModel = new TableChannel();
@Override String getTabName() { return "NIO channels"; }
@Override public TableChannel getTableModel() { return tableModel; }

/*
GUI window geometry constants    
*/
private final static Dimension SIZE_BIG_LABEL   = new Dimension ( 101, 21 );
private final static Dimension SIZE_SMALL_LABEL = new Dimension (  70, 21 );
private final static Dimension SIZE_TEXT_PATH   = new Dimension ( 260, 23 );
private final static Dimension SIZE_COMBO       = new Dimension ( 150, 21 );
private final static Dimension SIZE_BUTTON      = new Dimension (  89, 24 );
/*
Text labels for combo boxes
*/
private final JLabel[] labels =
    { new JLabel( "Source path" ),
      new JLabel( "Destination path" ),
      new JLabel( "File" ),
      new JLabel( "Block" ),
      new JLabel( "Count" ),
      new JLabel( "Threads" ),
      new JLabel( "Data" ),
      new JLabel( "Address" ),
      new JLabel( "R/W" ),
      new JLabel( "Fast copy" ),
      new JLabel( "Read sync" ),
      new JLabel( "Write sync" ),
      new JLabel( "Copy sync" ),
      new JLabel( "Read delay" ),
      new JLabel( "Write delay" ),
      new JLabel( "Copy delay" ), };
/*
Text Fields, Buttons and Combo Boxes
*/
private final JTextField[] texts =
    { new JTextField() ,
      new JTextField() };
private final JButton[] buttons  = new JButton[TEXT_COUNT];
private final JComboBox[] boxes = new JComboBox[COMBO_COUNT];
/*
Handlers for buttons functions
*/
private final static String NAME_SRC = 
    "SOURCE drive and directory for benchmarks";
private final static String NAME_DST = 
    "DESTINATION drive and directory for benchmarks";
private final HandlerSourcePath[] panelButtonsHandlers;
/*
Constants for labels, text fields and combo boxes
addressing and allocation by Spring Layout
*/
private final static int UP_FIRST = 0;
private final static int UP_LAST = 1;
private final static int LEFT_UP = 2;
private final static int LEFT_DOWN = 8;
private final static int RIGHT_UP = 9;
private final static int RIGHT_DOWN = 15;
/*
Text fields and combos arrays addressing
*/
private final static int TEXT_COUNT = 2;
private final static int COMBO_COUNT = 14;
private final static int TEXT_FIRST = 0;
private final static int COMBO_FIRST = 0;
private final static int COMBO_MIDDLE = 7;
/*
Support combo boxes set of available values
*/
// select performance scenario
// support sizes for files and blocks
private final static int K = 1024;
private final static int M = 1024*1024;
private final static String UNITS_B = " bytes";
private final static String UNITS_K = " KB";
private final static String UNITS_M = " MB";
// support time units, milliseconds-based
private final static int SECOND = 1000;
private final static int MINUTE = 1000*60;
private final static String UNITS_MS = " ms";
private final static String UNITS_SECOND  = " second";
private final static String UNITS_SECONDS = " seconds";
private final static String UNITS_MINUTE  = " minute";
private final static String UNITS_MINUTES = " minutes";
// file size option
private final static int ID_FILE_SIZE = 0;
private final static int DEFAULT_FILE_SIZE_MBPS = 16;  // means index of default
private final static int DEFAULT_FILE_SIZE_IOPS = 3;
private final static int SET_FILE_SIZE_BYTES[] =
    { 512, 1024, 2*1024, 4*1024, 8*1024, 
      16*1024, 32*1024, 64*1024, 128*1024, 256*1024, 
      512*1024, 1024*1024, 10*1024*1024, 100*1024*1024, 200*1024*1024,
      400*1024*1024, 1024*1024*1024, 1536*1024*1024 };
// block size option
private final static int ID_BLOCK_SIZE = 1;
private final static int DEFAULT_BLOCK_SIZE_MBPS = 13;
private final static int DEFAULT_BLOCK_SIZE_IOPS = 3;
private final static int SET_BLOCK_SIZE_BYTES[] =
    { 512, 1024, 2*1024, 4*1024, 8*1024, 
      16*1024, 32*1024, 64*1024, 128*1024, 256*1024, 
      512*1024, 1024*1024, 10*1024*1024, 128*1024*1024, 256*1024*1024 };
// file count option
private final static int ID_FILE_COUNT = 2;
private final static int DEFAULT_FILE_COUNT_MBPS = 9;
private final static int DEFAULT_FILE_COUNT_IOPS = 21;
private final static int SET_FILE_COUNT[] = 
    { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
      20, 40, 50, 100, 500, 1000, 5000, 10000, 50000, 100000,
      200000, 500000, 1000000, 2500000, 5000000  };
// thread count option
private final static int ID_THREAD_COUNT = 3;
private final static int DEFAULT_THREAD_COUNT = 0;
private final static int SET_THREAD_COUNT[] =
    { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
      16, 24, 32, 48, 64, 128, 256 };
// data randomization option
private final static int ID_DATA_PATTERN = 4;
private final static int DEFAULT_DATA_PATTERN = 0;
private final static String SET_DATA_PATTERN[] =
    { "Zeroes", "Ones", "Sequental", "Software RNG", "Hardware RNG" };
// address randomization option
private final static int ID_ADDRESS_PATTERN = 5;
private final static int DEFAULT_ADDRESS_MBPS = 0;
private final static int DEFAULT_ADDRESS_IOPS = 2;
private final static String SET_ADDRESS_PATTERN[] =
    { "Sequental", "Batterfly", "Software RNG", "Hardware RNG" };
// read-write mode option
private final static int ID_READ_WRITE = 6;
private final static int DEFAULT_READ_WRITE = 0;
private final static String SET_READ_WRITE[] =
    { "Read/Write no mix", "R/W 50/50 mixed", "R/W 70/30 mixed", "R/W 30/70 mixed",
      "Read only", "Write only" };
// fast copy option
private final static int ID_FAST_COPY = 7;
private final static int DEFAULT_FAST_COPY = 1;
private final static String SET_FAST_COPY[] =
    { "Disabled", "Enabled" };
// read synchronization option
private final static int ID_READ_SYNC = 8;
private final static int DEFAULT_READ_SYNC = 0;
private final static String SET_READ_SYNC[] =
    { "Buffered", "Unbuffered" };
// write synchronization option
private final static int ID_WRITE_SYNC = 9;
private final static int DEFAULT_WRITE_SYNC = 0;
private final static String SET_WRITE_SYNC[] =
    { "Write back", "Write through", "WT + Sparse"  };
// copy synchronization option
private final static int ID_COPY_SYNC = 10;
private final static int DEFAULT_COPY_SYNC = 0;
private final static String SET_COPY_SYNC[] =
    { "Write back", "Write through", "WT + Sparse"  };
// read delay option
private final static int ID_READ_DELAY = 11;
private final static int DEFAULT_READ_DELAY = 0;
private final static int SET_READ_DELAY[] =
    { 0, 1, 5, 10, 100, 500, 1000, 10000, 20000, 30000, 
      60000, 120000, 180000, 240000, 300000 };
// write delay option
private final static int ID_WRITE_DELAY = 12;
private final static int DEFAULT_WRITE_DELAY = DEFAULT_READ_DELAY;
private final static int SET_WRITE_DELAY[] = SET_READ_DELAY;
// copy delay option
private final static int ID_COPY_DELAY = 13;
private final static int DEFAULT_COPY_DELAY = DEFAULT_READ_DELAY;
private final static int SET_COPY_DELAY[] = SET_READ_DELAY;

/*
Panel constructor
*/
public PanelChannel( Application application )
    {
    super( application );
    // this constructors must call when valid APPLICATION reference
    panelButtonsHandlers = new HandlerSourcePath[]
        { new HandlerSourcePath 
            ( application, NAME_SRC, texts[UP_FIRST], texts[UP_LAST] ) ,
          new HandlerDestinationPath
            ( application, NAME_DST, texts[UP_LAST] ) };
    }

/*
Additional build method, 
not make this operations in constructor because overridable warnings.
*/
@Override void build()
    {
    SpringLayout sl = new SpringLayout();
    Container c = this;
    c.setLayout( sl );
    /*
    Positioning and add up labels: source and destination paths.
    */
    int j = TEXT_FIRST;
    for( int i=UP_FIRST; i<=UP_LAST; i++ )
        {
        // text label, located left
        if ( i == UP_FIRST )
            {
            sl.putConstraint( SpringLayout.NORTH, labels[i], 12, 
                              SpringLayout.NORTH, c );
            }
        else
            {
            sl.putConstraint( SpringLayout.NORTH, labels[i], 6, 
                              SpringLayout.SOUTH, labels[i-1] );
            }
        sl.putConstraint( SpringLayout.WEST, labels[i], 11, 
                          SpringLayout.WEST, c );
        labels[i].setPreferredSize( SIZE_BIG_LABEL );
        add( labels[i] );
        // text input field, located middle
        texts[j].setPreferredSize( SIZE_TEXT_PATH );
        sl.putConstraint( SpringLayout.NORTH, texts[j], 0, 
                          SpringLayout.NORTH, labels[i] );
        sl.putConstraint( SpringLayout.WEST, texts[j], 0, 
                          SpringLayout.EAST, labels[i] );
        add( texts[j] );
        // button "Browse" located right
        buttons[j] = new JButton( "Browse" );
        buttons[j].setPreferredSize( SIZE_BUTTON );
        sl.putConstraint( SpringLayout.NORTH, buttons[j], 0, 
                          SpringLayout.NORTH, labels[i] );
        sl.putConstraint( SpringLayout.WEST, buttons[j], 4, 
                          SpringLayout.EAST, texts[j] );
        buttons[j].addActionListener( panelButtonsHandlers[j] );
        add( buttons[j++] );
        }
        
    /*
    Positioning and add left labels: file operations options.
    */
    j = COMBO_FIRST;
    for( int i=LEFT_UP; i<=LEFT_DOWN; i++ )
        {
        // text label, located left
        if( i == LEFT_UP )
            {
            sl.putConstraint( SpringLayout.NORTH, labels[i], 19, 
                              SpringLayout.SOUTH, labels[UP_LAST] );
            }
        else
            {
            sl.putConstraint( SpringLayout.NORTH, labels[i], 6, 
                              SpringLayout.SOUTH, labels[i-1] );
            }
        sl.putConstraint( SpringLayout.WEST, labels[i], 11, 
                          SpringLayout.WEST, c );
        labels[i].setPreferredSize( SIZE_SMALL_LABEL );
        add( labels[i] );
        // combo box, located right
        boxes[j] = new JComboBox();
        boxes[j].setPreferredSize( SIZE_COMBO );
        sl.putConstraint( SpringLayout.NORTH, boxes[j], 0, 
                          SpringLayout.NORTH, labels[i] );
        sl.putConstraint( SpringLayout.WEST, boxes[j], 0, 
                          SpringLayout.EAST, labels[i] );
        add( boxes[j++] );
        }

    /*
    Positioning and add left labels: additional (timing) options.
    */
    j = COMBO_MIDDLE;
    for( int i=RIGHT_UP; i<=RIGHT_DOWN; i++ )
        {
        if( i == RIGHT_UP )
            {
            sl.putConstraint( SpringLayout.NORTH, labels[i], 19, 
                              SpringLayout.SOUTH, labels[UP_LAST] );
            }
        else
            {
            sl.putConstraint( SpringLayout.NORTH, labels[i], 6, 
                              SpringLayout.SOUTH, labels[i-1] );
            }
        sl.putConstraint( SpringLayout.WEST, labels[i], 270, 
                          SpringLayout.WEST, c );
        labels[i].setPreferredSize( SIZE_SMALL_LABEL );
        add( labels[i] );
        // combo box, located right
        boxes[j] = new JComboBox();
        boxes[j].setPreferredSize( SIZE_COMBO );
        sl.putConstraint( SpringLayout.NORTH, boxes[j], 0, 
                          SpringLayout.NORTH, labels[i] );
        sl.putConstraint( SpringLayout.WEST, boxes[j], 0, 
                          SpringLayout.EAST, labels[i] );
        add( boxes[j++] );
        }
    /*
    Assign default values for combo boxes
    first combo box with index = 0 : file size
    */
    setDefaults( SCENARIO.MBPS );
    
    
    // ========== DEBUG LOCKS ==========
    labels[5].setEnabled( false );
    labels[6].setEnabled( false );
    labels[7].setEnabled( false );
    labels[8].setEnabled( false );
    labels[10].setEnabled( false );
    boxes[3].setEnabled( false );
    boxes[4].setEnabled( false );
    boxes[5].setEnabled( false );
    boxes[6].setEnabled( false );
    boxes[8].setEnabled( false );
    // ========== END OF DEBUG LOCKS ==========
    }

/*
Customize panel with combo boxes, by restrictions for options settings,
static restrictions: disable some options,
dynamic restrictions: automatically update option X after modify option Y,
for example, automatically update destination path after modify source path,
set block size not above file size.
*/
@Override void buildRestrictions()
    {
    
    }

/*
Public method for initializing at start and re-initializing by buttons:
"Default MBPS" , "Default IOPS".
This method can be called from buttons handlers.
*/
@Override public void setDefaults( SCENARIO scenario )
    {
    this.scenario = scenario;
    
    for( JComboBox box : boxes )
        {
        box.removeAllItems();
        }
    
    if ( scenario == SCENARIO.MBPS )
        {
        helperComboSize  ( ID_FILE_SIZE,
                           SET_FILE_SIZE_BYTES, DEFAULT_FILE_SIZE_MBPS );
        helperComboSize  ( ID_BLOCK_SIZE,
                           SET_BLOCK_SIZE_BYTES, DEFAULT_BLOCK_SIZE_MBPS );
        helperComboCount ( ID_FILE_COUNT,
                           SET_FILE_COUNT, DEFAULT_FILE_COUNT_MBPS );
        helperComboString( ID_ADDRESS_PATTERN,
                           SET_ADDRESS_PATTERN, DEFAULT_ADDRESS_MBPS );
        }
    else
        {
        helperComboSize  ( ID_FILE_SIZE,
                           SET_FILE_SIZE_BYTES, DEFAULT_FILE_SIZE_IOPS );
        helperComboSize  ( ID_BLOCK_SIZE,
                           SET_BLOCK_SIZE_BYTES, DEFAULT_BLOCK_SIZE_IOPS );
        helperComboCount ( ID_FILE_COUNT,
                           SET_FILE_COUNT, DEFAULT_FILE_COUNT_IOPS );
        helperComboString( ID_ADDRESS_PATTERN,
                           SET_ADDRESS_PATTERN, DEFAULT_ADDRESS_IOPS );
        }
    helperComboCount ( ID_THREAD_COUNT,
                       SET_THREAD_COUNT, DEFAULT_THREAD_COUNT );
    helperComboString( ID_DATA_PATTERN,
                       SET_DATA_PATTERN, DEFAULT_DATA_PATTERN );
    helperComboString( ID_READ_WRITE,
                       SET_READ_WRITE, DEFAULT_READ_WRITE );
    helperComboString( ID_FAST_COPY,
                       SET_FAST_COPY, DEFAULT_FAST_COPY );
    helperComboString( ID_READ_SYNC,
                       SET_READ_SYNC, DEFAULT_READ_SYNC );
    helperComboString( ID_WRITE_SYNC,
                       SET_WRITE_SYNC, DEFAULT_WRITE_SYNC );
    helperComboString( ID_COPY_SYNC,
                       SET_COPY_SYNC, DEFAULT_COPY_SYNC );
    helperComboTime  ( ID_READ_DELAY,
                       SET_READ_DELAY, DEFAULT_READ_DELAY );
    helperComboTime  ( ID_WRITE_DELAY,
                       SET_WRITE_DELAY, DEFAULT_WRITE_DELAY );
    helperComboTime  ( ID_COPY_DELAY,
                       SET_COPY_DELAY, DEFAULT_COPY_DELAY );
    }


/*
Public method for clear benchmarks results by button: "Clear".
This method can be called from button handler.
*/
@Override public void clearResults()
    {
    // Reserved for panel-specific clear, additional to HandlerClear action.
    }

/*
Helpers for build combo boxes content,
*/

private void helperComboSize( int id, int[] valuesArray, int selection )
    {
    for( int value : valuesArray )
        {
        String units;
        if ( value % M == 0 )
            {
            value /= M;
            units = UNITS_M;
            }
        else if ( value % K == 0 )
            {
            value /= K;
            units = UNITS_K;
            }
        else
            {
            units = UNITS_B;
            }
        boxes[id].addItem( " " + Integer.toString( value ) + units );
        }
    boxes[id].setSelectedIndex( selection );
    }

private void helperComboCount( int id, int[] valuesArray, int selection )
    {
    for( int value : valuesArray )
        {
        boxes[id].addItem( " " + Integer.toString( value ) );
        }
    boxes[id].setSelectedIndex( selection );    
    }

private void helperComboString( int id, String[] namesArray, int selection )
    {
    for ( String name : namesArray ) 
        {
        boxes[id].addItem( " " + name );
        }
    boxes[id].setSelectedIndex( selection );
    }

private void helperComboTime( int id, int[] valuesArray, int selection )
    {
    for( int value : valuesArray )
        {
        String units;
        if ( value % MINUTE == 0 )
            {
            value /= MINUTE;
            units = ( value == 1 ) ? UNITS_MINUTE : UNITS_MINUTES;
            }
        else if ( value % SECOND == 0 )
            {
            value /= SECOND;
            units = ( value == 1 ) ? UNITS_SECOND : UNITS_SECONDS;
            }
        else
            {
            units = UNITS_MS;
            }
        String s = ( value == 0 ) 
            ? "None" 
            : Integer.toString( value ) + units;
        boxes[id].addItem( " " + s );
        }
    boxes[id].setSelectedIndex( selection );
    }

/*
Support callbacks from "Run" button handler
*/

private boolean[] saveButtonsEnable;
private boolean[] saveBoxesEnable;
private boolean[] saveTextsEnable;
private boolean[] saveLabelsEnable;

@Override public void disableGuiBeforeRun()
    {
    saveButtonsEnable = disableHelper( buttons );
    saveBoxesEnable = disableHelper( boxes );
    saveTextsEnable = disableHelper( texts );
    saveLabelsEnable = disableHelper( labels );
    }

@Override public void enableGuiAfterRun()
    {
    enableHelper( saveLabelsEnable, labels );
    enableHelper( saveTextsEnable, texts );
    enableHelper( saveBoxesEnable, boxes );
    enableHelper( saveButtonsEnable, buttons );
    }

private boolean[] disableHelper( JComponent[] c )
    {
    boolean[] b = new boolean [c.length];
    for( int i=0; i<c.length; i++ )
        {
        b[i] = c[i].isEnabled();
        c[i].setEnabled( false );
        c[i].repaint();
        c[i].revalidate();
        }
    return b;
    }

private void enableHelper( boolean[] b, JComponent[] c )
    {
    for( int i=0; i<c.length; i++ )
        {
        c[i].setEnabled( b[i] );
        c[i].repaint();
        c[i].revalidate();
        }
    }

@Override public String optionSourcePath()
    { return texts[UP_FIRST].getText(); }

@Override public String optionDestinationPath()
    { return texts[UP_LAST].getText(); }

@Override public int optionFileSize()
    { return helperValue( ID_FILE_SIZE, SET_FILE_SIZE_BYTES ); }

@Override public int optionBlockSize()
    { return helperValue( ID_BLOCK_SIZE, SET_BLOCK_SIZE_BYTES ); }
        
@Override public int optionFileCount()
    { return helperValue( ID_FILE_COUNT, SET_FILE_COUNT ); }
        
@Override public int optionThreadCount()
    { return helperValue( ID_THREAD_COUNT, SET_THREAD_COUNT ); }
        
@Override public int optionDataMode()
    { return helperIndex( ID_DATA_PATTERN ); }
        
@Override public int optionAddressMode()
    { return helperIndex( ID_ADDRESS_PATTERN ); }
        
@Override public int optionRwMode()
    { return helperIndex( ID_READ_WRITE ); }
        
@Override public int optionFastCopy()
    { return helperIndex( ID_FAST_COPY ); }
        
@Override public int optionReadSync()
    { return helperIndex( ID_READ_SYNC ); }

@Override public int optionWriteSync()
    { return helperIndex( ID_WRITE_SYNC ); }

@Override public int optionCopySync()
    { return helperIndex( ID_COPY_SYNC ); }
        
@Override public int optionReadDelay()
    { return helperValue( ID_READ_DELAY, SET_READ_DELAY ); }
        
@Override public int optionWriteDelay()
    { return helperValue( ID_WRITE_DELAY, SET_WRITE_DELAY ); }
        
@Override public int optionCopyDelay()
    { return helperValue( ID_COPY_DELAY, SET_COPY_DELAY ); }

private int helperValue( int id, int[] valuesArray )
    {
    int index = helperIndex( id );
    int value = -1;
    if ( ( index >= 0 )&&( index < valuesArray.length ) )
        {
        value = valuesArray[index];
        }
    return value;
    }

private int helperIndex( int id )
    {
    return boxes[id].getSelectedIndex();
    }

/*
Build IO scenario with options settings, defined in this panel
*/
@Override public IOscenario buildIOscenario()
    {
    IOscenario ios = new IOscenarioChannel
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
          null );
    return ios;
    }

/*
Return text information about options settings at start IO scenario
*/
@Override public String reportIOscenario()
    {
    String s = "\r\n--- NIO Channels IO scenario options ---\r\n";
    StringBuilder sb = new StringBuilder( s );
    int n = labels.length;
    int maxLabel = 0;
    for ( JLabel label : labels )
        {
        int m = label.getText().length();
        if ( m > maxLabel )
            {
            maxLabel = m;
            }
        }
    for ( int i=0; i<n; i++ )
        {
        int j = sb.length();
        sb.append( labels[i].getText() );
        j = sb.length() - j;
        for( ; j<maxLabel; j++ )
            {
            sb.append( " " );
            }
        sb.append( " : " );
        if ( i < TEXT_COUNT )
            {
            sb.append( texts[i].getText() );
            }
        else if ( i < TEXT_COUNT + COMBO_COUNT )
            {
            s = (String)( boxes[i - TEXT_COUNT].getSelectedItem() );
            sb.append( s.trim() );
            }
        else
            {
            sb.append( "?" );
            }
        sb.append( "\r\n" );
        }
    sb.append( "\r\n" );
    return sb.toString();
    }

}
