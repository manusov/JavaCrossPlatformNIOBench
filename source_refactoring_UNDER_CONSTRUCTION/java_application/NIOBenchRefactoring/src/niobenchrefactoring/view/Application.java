/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
NIOBench GUI application main window frame with tabbed sub-panels
*/

package niobenchrefactoring.view;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import javax.swing.AbstractAction;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
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
import niobenchrefactoring.resources.About;
import niobenchrefactoring.resources.PAL;
import opendraw.OpenDraw;
import openlog.OpenLog;
import opentable.OpenTable;

public class Application extends JFrame
{
/*
Global objects
*/    
private final PAL pal;
/*
GUI window geometry constants    
*/
private final static int X_SIZE = 520;
private final static int Y_SIZE = 590;
private final static Dimension SIZE_PROGRESS = new Dimension ( 230, 25 );
/*
Openable child windows.
*/
private final OpenLog childLog = 
        new OpenLog( this, About.getShortName() + " - Log" );
private final OpenTable childTable = 
        new OpenTable( this, About.getShortName() + " - Statistics" );
private final OpenDraw childDraw =
        new OpenDraw( this, About.getShortName() + " - Charts" );
/*
GUI window components
*/
private final JTable table = new JTable();
private final JScrollPane tableScroll = new JScrollPane( table );
private final JTabbedPane tabs;
private final DefaultBoundedRangeModel progressModel = 
                        new DefaultBoundedRangeModel( 0, 0, 0, 100 );
private final JProgressBar progress = new JProgressBar( progressModel );
private final String progressString = "Please run...";
/*
Panels for scenarios, selected by tabs
*/
private final ApplicationPanel[] panels =
    { new PanelChannel       ( this ) ,
      new PanelAsyncChannel  ( this ) ,
      new PanelScatterGather ( this ) ,
      new PanelMemoryMapped  ( this ) ,
      new PanelArchives      ( this ) ,
      new PanelNative        ( this ) ,
      new PanelSSD           ( this ) };
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
    { new HandlerLog         ( this, childLog   ) ,
      new HandlerTable       ( this, childTable ) ,
      new HandlerDraw        ( this, childDraw  ) ,
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
private final static int RUN_ID     = 11;

/*
GUI window constructor
*/
public Application( PAL pal )
    {
    super( About.getShortName() );
    this.pal = pal;
    tabs = new JTabbedPane();
    for ( ApplicationPanel panel : panels )
        {
        panel.pal = pal;
        panel.build();
        tabs.add( panel, panel.getTabName() );
        }
    for( int i=0; i<buttons.length; i++ )
        {
        buttons[i].addActionListener( handlers[i] );
        }
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
    progress.setString( progressString );
    sl.putConstraint( SpringLayout.NORTH, progress,  0,
                      SpringLayout.NORTH, buttons[RUN_ID] );
    sl.putConstraint( SpringLayout.EAST,  progress, -4,
                      SpringLayout.WEST , buttons[RUN_ID] );
    add( progress );
    
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
        }
    }

}
