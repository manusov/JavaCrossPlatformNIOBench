/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Handler for open statistics table window with benchmark results as
performance = f ( test parameters ).
*/

package opentable;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import javax.swing.table.DefaultTableCellRenderer;
import niobenchrefactoring.view.Application.APPLICATION_PANELS;

public class OpenTable extends JFrame
{
/*
Basic fields    
*/
private final JFrame parentFrame;
private final JPanel tbp;
private final JTable table;
private final StatisticsTableModel stm;
private boolean childActive = false;
/*
Constants for screen objects sizing and positioning
*/
private final static int X_SIZE  = 510;
private final static int Y_SIZE  = 520;
private final static int X_SHIFT = 400;
private final static int Y_SHIFT = 220;

public void setPanelType( APPLICATION_PANELS ap )
    {
    stm.setPanelType( ap );
    }

public OpenTable( JFrame parentFrame, String frameTitle, APPLICATION_PANELS ap )
    {
    super( frameTitle );
    this.parentFrame = parentFrame;
    SpringLayout sl = new SpringLayout();
    tbp = new JPanel( sl );
    stm = new StatisticsTableModel( ap );
    table = new JTable( stm );
        JScrollPane tbs = new JScrollPane( table );
    DefaultTableCellRenderer tr = new DefaultTableCellRenderer();
    tr.setHorizontalAlignment( SwingConstants.CENTER );
    for ( int i=0; i<table.getColumnCount(); i++ )
        table.getColumnModel().getColumn(i).setCellRenderer( tr );
    tbp.add( tbs );
    // layout setup
    sl.putConstraint( SpringLayout.NORTH, tbs,  1, SpringLayout.NORTH, tbp );
    sl.putConstraint( SpringLayout.SOUTH, tbs, -1, SpringLayout.SOUTH, tbp );
    sl.putConstraint( SpringLayout.WEST,  tbs,  1, SpringLayout.WEST,  tbp );
    sl.putConstraint( SpringLayout.EAST,  tbs, -1, SpringLayout.EAST,  tbp );
    }

// method for blank table and set maximum lines = n,
// n exclude 4 additional Median, Average, Minimum, Maximum
public void blankTable( int n )
    {
    if ( stm != null )
        {
        stm.blank( n );
        DefaultTableCellRenderer tr = new DefaultTableCellRenderer();
        tr.setHorizontalAlignment( SwingConstants.CENTER );
        for ( int i=0; i<table.getColumnCount(); i++ )
            table.getColumnModel().getColumn(i).setCellRenderer( tr );
        }
    }

// method overload for blank and remove statistics lines
public void blankTable()
    {
    blankTable( 0 );
    }

// method gets table model, used for text report
public StatisticsTableModel getTableModel()
    {
    return stm;
    }

// this point for start drawings, checks if currently active
public void open()
    {
    if ( ! childActive )
        {
        childActive = true;    // prevent open >1 window
        add( tbp );
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        addWindowListener( new ChildWindowListener() );
        Point p;
        if ( parentFrame != null )
            {
            p = parentFrame.getLocation();
            p.x = p.x + X_SHIFT;
            p.y = p.y + Y_SHIFT;
            setLocation( p );
            }
        else
            {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = screenSize.width / 2 - X_SIZE / 2;
            int y = screenSize.height / 2 - Y_SIZE / 2;
            p = new Point( x, y );
            }
        this.setLocation( p );
        setSize( new Dimension ( X_SIZE, Y_SIZE ) );
        setResizable( true );
        setVisible( true );
        }
    }

/*
This point for close statistics table window
*/
private class ChildWindowListener extends WindowAdapter 
    {
    @Override public void windowClosing( WindowEvent e )
        {
        childActive = false;  // enable re-open this window if it closed
        }
    }
}
