/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Openable window for show performance test log,
performance, MBPS = F ( iteration) or IOPS = F ( iteration ).
*/

package openlog;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import javax.swing.text.DefaultCaret;

public class OpenLog extends JFrame
{
/*
Basic fields    
*/
private final JFrame parentFrame;
private final JPanel txp;
private final JTextArea tx;
private boolean childActive = false;
/*
Constants for screen objects sizing and positioning
*/
private final static int X_SIZE  = 610;
private final static int Y_SIZE  = 520;
private final static int X_SHIFT = 300;
private final static int Y_SHIFT = 120;
    
public OpenLog( JFrame parentFrame, String frameTitle )
    {
    super( frameTitle );
    this.parentFrame = parentFrame;
    SpringLayout sl = new SpringLayout();
    txp = new JPanel( sl );
    tx = new JTextArea();
    Font font = new Font( "monospaced", Font.PLAIN, 12 );
    tx.setFont( font );
    tx.setEditable( false );
    DefaultCaret caret = ( DefaultCaret )( tx.getCaret() );
    caret.setUpdatePolicy( DefaultCaret.ALWAYS_UPDATE );
    JScrollPane txs = new JScrollPane( tx );
    txp.add( txs );
    // layout setup
    sl.putConstraint( SpringLayout.NORTH, txs,  1, SpringLayout.NORTH, txp );
    sl.putConstraint( SpringLayout.SOUTH, txs, -1, SpringLayout.SOUTH, txp );
    sl.putConstraint( SpringLayout.WEST,  txs,  1, SpringLayout.WEST,  txp );
    sl.putConstraint( SpringLayout.EAST,  txs, -1, SpringLayout.EAST,  txp );
    }
    
// this point for start drawings, checks if currently active
public void open()
    {
    if ( ! childActive )
        {
        childActive = true;    // prevent open >1 window
        add( txp );
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

// method for write string, append to existed
public void write( String s )
    {
    if ( tx != null )
        tx.append( s );
    }

// method for write string, overwrite existed
public void overWrite( String s )
    {
    if ( tx != null )
        tx.setText( s );
    }

// this point for stop drawings
private class ChildWindowListener extends WindowAdapter 
    {
    @Override public void windowClosing( WindowEvent e )
        {
        childActive = false;  // enable re-open this window if it closed
        }
    }

}
