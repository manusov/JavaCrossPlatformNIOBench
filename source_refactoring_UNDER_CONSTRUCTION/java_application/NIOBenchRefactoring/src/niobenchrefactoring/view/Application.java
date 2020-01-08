/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
NIOBench GUI application main window frame with tabbed sub-panels
*/

package niobenchrefactoring.view;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import javax.swing.AbstractAction;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import niobenchrefactoring.controller.HandlerAbout;
import niobenchrefactoring.controller.HandlerCancel;
import niobenchrefactoring.controller.HandlerClear;
import niobenchrefactoring.controller.HandlerDefaultIOPS;
import niobenchrefactoring.controller.HandlerDefaultMBPS;
import niobenchrefactoring.controller.HandlerDraw;
import niobenchrefactoring.controller.HandlerGraph;
import niobenchrefactoring.controller.HandlerLoad;
import niobenchrefactoring.controller.HandlerLog;
import niobenchrefactoring.controller.HandlerReport;
import niobenchrefactoring.controller.HandlerRun;
import niobenchrefactoring.controller.HandlerTable;
import static niobenchrefactoring.model.IOscenario.COMPLETE_ID;
import static niobenchrefactoring.model.IOscenario.COPY_ID;
import static niobenchrefactoring.model.IOscenario.DELAY_ID;
import static niobenchrefactoring.model.IOscenario.DELETE_ID;
import static niobenchrefactoring.model.IOscenario.ERROR_ID;
import static niobenchrefactoring.model.IOscenario.READ_ID;
import static niobenchrefactoring.model.IOscenario.STARTING_ID;
import static niobenchrefactoring.model.IOscenario.WRITE_ID;
import niobenchrefactoring.resources.About;
import niobenchrefactoring.resources.PAL;
import static niobenchrefactoring.view.PanelChannel.UP_FIRST;
import static niobenchrefactoring.view.PanelChannel.UP_LAST;
import opendraw.OpenDraw;
import openlog.OpenLog;
import opentable.OpenTable;

public class Application extends JFrame
{
/*
Global objects with getters, used by handlers
*/    
private final PAL pal;
private final int palWidth;
public PAL getPAL()      { return pal;      }
public int getPALwidth() { return palWidth; }
/*
GUI window geometry constants    
*/
private final static int X_SIZE = 520;
private final static int Y_SIZE = 590;
private final static Dimension SIZE_PROGRESS = new Dimension ( 230, 25 );
/*
Openable child windows with getters, used by handlers
*/
private final OpenLog childLog = 
        new OpenLog( this, About.getShortName() + " - Log" );
private final OpenTable childTable = 
        new OpenTable( this, About.getShortName() + " - Statistics" );
private final OpenDraw childDraw =
        new OpenDraw( this, About.getShortName() + " - Charts" );
// getters
public OpenLog   getChildLog()   { return childLog;   }
public OpenTable getChildTable() { return childTable; }
public OpenDraw  getChildDraw()  { return childDraw;  }
/*
GUI window components
*/
private final JTable table = new JTable();
private final JScrollPane tableScroll = new JScrollPane( table );
private final JTabbedPane tabs;
private final DefaultBoundedRangeModel progressModel = 
                        new DefaultBoundedRangeModel( 0, 0, 0, 100 );
private final JProgressBar progress = new JProgressBar( progressModel );
private final static String PROGRESS_STRING = "Please run...";
private final JLabel operation = new JLabel();
private final static String OPERATION_STRING = "No actions running";

/*
Panels for scenarios, selected by tabs
*/
private final ApplicationPanel[] panels;
/*
Current selected panel store and getter, used by buttons handlers
*/
private ApplicationPanel selectedPanel;
public ApplicationPanel getSelectedPanel()
    {
    return selectedPanel;
    }
/*
Buttons with it text labels
*/
private final JButton[] buttons =
    { new JButton( "Log   >" ),
      new JButton( "Table >" ),
      new JButton( "Draw  >" ),
      new JButton( "Default MBPS" ),
      new JButton( "Default IOPS" ),
      new JButton( "About" ),
      new JButton( "Load" ),
      new JButton( "Graph" ),
      new JButton( "Report" ),
      new JButton( "Clear" ),
      new JButton( "Cancel" ),
      new JButton( "Run" ) };
/*
Handlers for buttons functions
*/
private final AbstractAction[] handlers =
    { new HandlerLog         ( this ) ,
      new HandlerTable       ( this ) ,
      new HandlerDraw        ( this ) ,
      new HandlerDefaultMBPS ( this ) ,
      new HandlerDefaultIOPS ( this ) ,
      new HandlerAbout       ( this ) ,
      new HandlerLoad        ( this ) ,
      new HandlerGraph       ( this ) ,
      new HandlerReport      ( this ) ,
      new HandlerClear       ( this ) ,
      new HandlerCancel      ( this ) ,
      new HandlerRun         ( this ) };
/*
Constants for buttons addressing and allocation by Spring Layout
*/
private final static int COL_UP     = 0;     // up right corner, open buttons
private final static int COL_DOWN   = 2;
private final static int ROW1_LEFT  = 3;     // down buttons: defaults
private final static int ROW1_RIGHT = 4;
private final static int ROW2_LEFT  = 5;     // other down buttons:
private final static int ROW2_RIGHT = 10;    // About ... Cancel

private final static int DEFAULT_ID = 3;     // first run-deactivated
private final static int CANCEL_ID  = 10;    // not deactivated when run
private final static int RUN_ID     = 11;

public static enum APPLICATION_PANELS 
    { CHANNEL, 
      ASYNC_CHANNEL, 
      SCATTER_GATHER, 
      MEMORY_MAPPED, 
      ARCHIVE, 
      NATIVE, 
      SSD }

/*
GUI window constructor
*/
public Application( PAL pal, int palWidth )
    {
    super( About.getShortName() );
    this.pal = pal;
    this.palWidth = palWidth;
    // this constructors must call when valid PAL reference
    panels = new ApplicationPanel[]
        { new PanelChannel       ( this ) ,
          new PanelAsyncChannel  ( this ) ,
          new PanelScatterGather ( this ) ,
          new PanelMemoryMapped  ( this ) ,
          new PanelArchives      ( this ) ,
          new PanelNative        ( this ) ,  // this available = f(hardware)
          new PanelSSD           ( this ) };
    selectedPanel = panels[0];
    // tabbed panels and common (used for all panels) buttons
    tabs = new JTabbedPane();
    for ( ApplicationPanel panel : panels )
        {
        panel.build();
        tabs.add( panel, panel.getTabName() );
        }
    for( int i=0; i<buttons.length; i++ )
        {
        buttons[i].addActionListener( handlers[i] );
        }
    // Lock "Native OS API" tab if native library not loaded
    if ( palWidth < 0 )
        {
        tabs.setEnabledAt( APPLICATION_PANELS.NATIVE.ordinal(), false );
        tabs.setEnabledAt( APPLICATION_PANELS.SSD.ordinal(), false );
        }
    // locks for yet not supported or yet buggy panels
    /*
    tabs.setEnabledAt( APPLICATION_PANELS.CHANNEL.ordinal(), false );
    tabs.setEnabledAt( APPLICATION_PANELS.ASYNC_CHANNEL.ordinal(), false );
    tabs.setEnabledAt( APPLICATION_PANELS.SCATTER_GATHER.ordinal(), false );
    tabs.setEnabledAt( APPLICATION_PANELS.MEMORY_MAPPED.ordinal(), false );
    tabs.setEnabledAt( APPLICATION_PANELS.ARCHIVE.ordinal(), false );
    tabs.setEnabledAt( APPLICATION_PANELS.NATIVE.ordinal(), false );
    tabs.setEnabledAt( APPLICATION_PANELS.SSD.ordinal(), false );
    */
    }

/*
GUI window open when run application
*/
public void open()
    {
    SpringLayout sl = new SpringLayout();
    Container c = getContentPane();
    c.setLayout( sl );
    selectionHelper();
    
    // centering cells in table
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
    renderer.setHorizontalAlignment( SwingConstants.CENTER );
    for ( int i=0; i<table.getColumnCount(); i++ )
        { 
        table.getColumnModel().getColumn(i).setCellRenderer( renderer ); 
        }

    // positioning up right buttons, this buttons used for open child windows
    for( int i=COL_UP; i<=COL_DOWN; i++ )
        {
        if ( i == COL_UP )
            {  // first button, means up
            sl.putConstraint( SpringLayout.NORTH, buttons[i], 3, 
                              SpringLayout.NORTH, c );
            }
        else
            {  // other, non-first buttons
            sl.putConstraint( SpringLayout.NORTH, buttons[i], 3, 
                              SpringLayout.SOUTH, buttons[i-1] );
            }
        sl.putConstraint( SpringLayout.EAST, buttons[i], -3, 
                          SpringLayout.EAST, c );
        if ( i != COL_DOWN )
            {
            sl.putConstraint( SpringLayout.WEST, buttons[i], 0, 
                              SpringLayout.WEST, buttons[COL_DOWN] );
            }
        add( buttons[i] );
        }
    
    // positioning down buttons, row 1, from right to left
    for( int i=ROW1_RIGHT; i>=ROW1_LEFT; i-- )
        {
        if ( i == ROW1_RIGHT )
            {  // first button, means right
            sl.putConstraint( SpringLayout.EAST, buttons[i], -3, 
                              SpringLayout.EAST, c );
            }
        else
            {  // other, non-first buttons
            sl.putConstraint( SpringLayout.EAST, buttons[i], -3, 
                              SpringLayout.WEST, buttons[i+1] );
            }
        sl.putConstraint( SpringLayout.SOUTH, buttons[i], -4, 
                          SpringLayout.NORTH, buttons[ROW2_RIGHT] );
        add( buttons[i] );
        }
    
    // positioning down buttons, row 2, from right to left
    for( int i=ROW2_RIGHT; i>=ROW2_LEFT; i-- )
        {
        if ( i == ROW2_RIGHT )
            {  // first button, means right
            sl.putConstraint( SpringLayout.EAST, buttons[i], -3, 
                              SpringLayout.EAST, c );
            }
        else
            {  // other, non-first buttons
            sl.putConstraint( SpringLayout.EAST, buttons[i], -3, 
                              SpringLayout.WEST, buttons[i+1] );
            }
        sl.putConstraint( SpringLayout.SOUTH, buttons[i], -3, 
                          SpringLayout.SOUTH, c );
        add( buttons[i] );
        }
    
    // positioning up table with benchmarks results
    sl.putConstraint( SpringLayout.NORTH, tableScroll, 2,
                      SpringLayout.NORTH, c );
    sl.putConstraint( SpringLayout.SOUTH, tableScroll, 120,
                      SpringLayout.NORTH, c );
    sl.putConstraint( SpringLayout.WEST, tableScroll,  1,
                      SpringLayout.WEST, c );
    sl.putConstraint( SpringLayout.EAST, tableScroll, -1,
                      SpringLayout.WEST, buttons[COL_DOWN] );
    add( tableScroll );
    
    // positioning tabbed panel
    sl.putConstraint( SpringLayout.NORTH, tabs,  2,
                      SpringLayout.SOUTH, tableScroll );
    sl.putConstraint( SpringLayout.SOUTH, tabs, -99, SpringLayout.SOUTH, c );
    sl.putConstraint( SpringLayout.WEST,  tabs,  0,  SpringLayout.WEST , c );
    sl.putConstraint( SpringLayout.EAST,  tabs,  0,  SpringLayout.EAST , c );
    add( tabs );
    
    // positioning run button
    sl.putConstraint( SpringLayout.NORTH, buttons[RUN_ID], 5,
                      SpringLayout.SOUTH, tabs );
    sl.putConstraint( SpringLayout.EAST, buttons[RUN_ID], -3,
                      SpringLayout.EAST, c );
    add( buttons[RUN_ID] );
    
    // customize and positioning progress bar
    progress.setPreferredSize( SIZE_PROGRESS );
    progress.setStringPainted( true );
    progress.setString( PROGRESS_STRING );
    sl.putConstraint( SpringLayout.NORTH, progress,  0,
                      SpringLayout.NORTH, buttons[RUN_ID] );
    sl.putConstraint( SpringLayout.EAST,  progress, -4,
                      SpringLayout.WEST , buttons[RUN_ID] );
    add( progress );
    
    // customize and positioning operation name label, left progress indicator
    defaultOperationString();
    sl.putConstraint( SpringLayout.NORTH, operation, 36,
                      SpringLayout.NORTH, progress );
    sl.putConstraint( SpringLayout.WEST, operation, 15,
                      SpringLayout.WEST, c );
    add( operation );
    
    // add listener for selected tab change
    tabs.addChangeListener( ( ChangeEvent e ) -> { selectionHelper(); } );
            
    // setup GUI window
    setDefaultCloseOperation( EXIT_ON_CLOSE );
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - X_SIZE / 2;
    int y = screenSize.height / 2 - Y_SIZE / 2;
    Point p = new Point( x, y );
    setLocation( p );
    setSize( new Dimension ( X_SIZE, Y_SIZE ) );
    setResizable( true );
    setVisible( true );
    }

/*
Helpers
*/
private void selectionHelper()
    {
    int index = tabs.getSelectedIndex();
    if ( ( index >= 0 )&&( index < panels.length ) )
        {
        table.setModel( panels[index].getTableModel() );
        table.repaint();
        selectedPanel = panels[index];
        }
    }

/*
Support callbacks from "Run" button handler
*/

private int saveCloseOperation;
private boolean[] saveButtonsEnable;
private boolean[] saveTabsEnable;
private String saveRunButtonText;
private final static String STOP_BUTTON_TEXT = "Stop";

public void disableGuiBeforeRun()
    {
    saveCloseOperation = this.getDefaultCloseOperation();
    this.setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
    saveButtonsEnable = disableHelper( buttons, DEFAULT_ID, CANCEL_ID );
    saveTabsEnable = disableHelper( new JComponent[]{ tabs } );
    saveRunButtonText = buttons[RUN_ID].getText();
    buttons[RUN_ID].setText( STOP_BUTTON_TEXT );
    }

public void enableGuiAfterRun()
    {
    buttons[RUN_ID].setText( saveRunButtonText );
    enableHelper( saveTabsEnable, new JComponent[]{ tabs } );
    enableHelper( saveButtonsEnable, buttons, DEFAULT_ID, CANCEL_ID );
    this.setDefaultCloseOperation( saveCloseOperation );
    }

private boolean[] disableHelper( JComponent[] c )
    {
    return disableHelper( c, 0, Integer.MAX_VALUE );
    }

private boolean[] disableHelper( JComponent[] c, int min, int max )
    {
    boolean[] b = new boolean [c.length];
    for( int i=0; i<c.length; i++ )
        {
        b[i] = c[i].isEnabled();
        if ( ( i >= min )&&( i <= max ) )
            {
            c[i].setEnabled( false );
            c[i].repaint();
            c[i].revalidate();
            }
        }
    return b;
    }

private void enableHelper( boolean[] b, JComponent[] c )
    {
    enableHelper( b, c, 0, Integer.MAX_VALUE );
    }

private void enableHelper( boolean[] b, JComponent[] c, int min, int max )
    {
    for( int i=0; i<c.length; i++ )
        {
        if ( ( i >= min )&&( i <= max ) )
            {
            c[i].setEnabled( b[i] );
            c[i].repaint();
            c[i].revalidate();
            }
        }
    }

public void updateProgress( int percentage )
    {
    progressModel.setValue( percentage );
    progress.setString ( progressModel.getValue() + "%" );
    progress.repaint();
    progress.revalidate();
    }

public void updateProgress( String message )
    {
    progress.setString ( message );
    progress.repaint();
    progress.revalidate();
    }

public void defaultOperationString()
    {
    updateOperationString( OPERATION_STRING, false, null );
    }

public void updateOperationString( String message )
    {
    updateOperationString( message, true, Color.BLACK );
    }

public void updateOperationString( String message, boolean enable, Color color )
    {
    if ( message == null )
        message = "N/A";
    operation.setText( message );
    operation.setEnabled( enable );
    if ( color != null )
        operation.setForeground( color );
    operation.repaint();
    operation.revalidate();
    }

public void updateOperationString( String message, int phase )
    {
    boolean enable = true;
    Color color;
    switch ( phase )
        {
        case READ_ID:
        case WRITE_ID:
        case COPY_ID:
            color = Color.BLUE;
            break;
        case DELETE_ID:
            color = Color.MAGENTA;
            break;
        case STARTING_ID:
        case COMPLETE_ID:
        case DELAY_ID:
            color = Color.BLACK;
            break;
        case ERROR_ID:
            color = Color.RED;
            break;
        default:  // this includes HALTED_ID
            enable = false;
            color = Color.BLACK;
            break;
        }
    updateOperationString( message, enable, color );
    }

/*
Support callbacks from HandlerSourcePath, HandlerDestinationPath for
globally change paths strings after change at one screen
*/

public void updateAllSrcPaths( String s )
    {
    for ( ApplicationPanel panel : panels ) 
        {
        if ( panel instanceof PanelChannel )
            {
            ( (PanelChannel) panel ).texts[UP_FIRST].setText( s );
            ( (PanelChannel) panel ).texts[UP_FIRST].repaint();
            }
        }
    }

public void updateAllDstPaths( String s )
    {
    for ( ApplicationPanel panel : panels ) 
        {
        if ( panel instanceof PanelChannel )
            {
            ( (PanelChannel) panel ).texts[UP_LAST].setText( s );
            ( (PanelChannel) panel ).texts[UP_LAST].repaint();
            }
        }
    }


}
