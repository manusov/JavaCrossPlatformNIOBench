/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Package for support functions Y=F(X) drawings.
Frame for benchmarks drawings as function Y=F(X).
Openable window for drawing, performance = f ( test parameters ).
*/

package opendraw;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import niobenchrefactoring.view.Application.APPLICATION_PANELS;

public class OpenDraw extends JFrame
{
/*
Basic fields    
*/
private final JFrame parentFrame;
private final JPanel childPanel;
private final FunctionController controller;
private boolean childActive = false;
/*
Constants for screen objects sizing and positioning
*/
private final static int X_SIZE  = 740;
private final static int Y_SIZE  = 540;
private final static int X_SHIFT = 30;
private final static int Y_SHIFT = 30;

/*
This required for select color legend naming:
pack-write-unpack or read-write-copy
*/
public void setPanelType( APPLICATION_PANELS ap )
    {
    controller.setPanelType( ap );
    }

/*
Frame class constructor, f = parent frame
*/
public OpenDraw( JFrame parentFrame, String frameTitle, APPLICATION_PANELS ap )
    {
    super( frameTitle );
    this.parentFrame = parentFrame;
    controller = new FunctionController( ap );
    childPanel = controller.getView().getPanel();
    }

/*
Get controller for access MVC pattern data
*/
public FunctionController getController()
    {
    return controller;
    }

/*
This point for start drawings, checks if currently active
*/
public void open()
    {
    if ( ! childActive )
        {
        childActive = true;
        add( childPanel );
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
        setVisible( true );
        setResizable( true );
        setVisible( true );
        }
    }

/*
This point for close charts drawings window
*/
private class ChildWindowListener extends WindowAdapter 
    {
    @Override public void windowClosing( WindowEvent e )
        {
        childActive = false;
        }
    }
}
