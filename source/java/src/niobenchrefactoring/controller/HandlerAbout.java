/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "About" button: show About Info, include web site link.
*/

package niobenchrefactoring.controller;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import java.net.URI;
import java.net.URISyntaxException;
import niobenchrefactoring.resources.About;
import niobenchrefactoring.view.Application;

public class HandlerAbout extends Handler
{
private final static Color LOGO_COLOR = new Color( 143,49,40 );
private final static Dimension SIZE_BUTTON_HTTP   = new Dimension ( 198, 25 );
private final static Dimension SIZE_BUTTON_CANCEL = new Dimension ( 75, 25 );

public HandlerAbout( Application application )
    {
    super( application );
    }

@Override public void actionPerformed( ActionEvent event )
    {
    JDialog dialog = new JDialog( application, "About", true );
    dialog.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
    // Create GUI components
    SpringLayout sl1 = new SpringLayout();
    JPanel p1 = new JPanel( sl1 );
    // String for web site
    final String sHttp = About.getWebSite();
    String sCancel = "Cancel";
    JLabel l1 = new JLabel ();
    try { l1.setIcon(new javax.swing.ImageIcon(getClass().
           getResource( About.getVendorIcon() ))); } 
    catch ( Exception e ) { }
    JLabel l2 = new JLabel  ( About.getLongName()   );
    JLabel l3 = new JLabel  ( About.getVendorName() );
    l2.setForeground( LOGO_COLOR );
    l3.setForeground( LOGO_COLOR );
    Font font1 = new Font ( "Verdana", Font.PLAIN, 12 );
    l2.setFont( font1 );
    l3.setFont( font1 );
    JButton b1 = new JButton( sHttp );
    JButton b2 = new JButton( sCancel );
    // Set buttons sizes
    b1.setPreferredSize( SIZE_BUTTON_HTTP );
    b2.setPreferredSize( SIZE_BUTTON_CANCEL );
    Font font2 = new Font ( "Verdana", Font.PLAIN, 11 );  // font for buttons
    b1.setFont( font2 );
    b2.setFont( font2 );
    // Layout management for panels and labels
    sl1.putConstraint ( SpringLayout.NORTH, l1,  24, SpringLayout.NORTH, p1 );
    sl1.putConstraint ( SpringLayout.WEST,  l1,  28, SpringLayout.WEST,  p1 );
    sl1.putConstraint ( SpringLayout.NORTH, l2,  24, SpringLayout.NORTH, p1 );
    sl1.putConstraint ( SpringLayout.WEST,  l2,   4, SpringLayout.EAST,  l1 );
    sl1.putConstraint ( SpringLayout.NORTH, l3,   0, SpringLayout.SOUTH, l2 );
    sl1.putConstraint ( SpringLayout.WEST,  l3,   4, SpringLayout.EAST,  l1 );
    // Layout management for panels and buttons
    sl1.putConstraint ( SpringLayout.SOUTH, b1, -10, SpringLayout.SOUTH, p1 );
    sl1.putConstraint ( SpringLayout.WEST,  b1,   8, SpringLayout.WEST,  p1 );
    sl1.putConstraint ( SpringLayout.SOUTH, b2, -10, SpringLayout.SOUTH, p1 );
    sl1.putConstraint ( SpringLayout.WEST,  b2,   3, SpringLayout.EAST,  b1 );
    // Add labels and buttons to panel
    p1.add( l1 );
    p1.add( l2 );
    p1.add( l3 );
    p1.add( b1 );
    p1.add( b2 );
    // Action listener for web button
    b1.addActionListener( ( ActionEvent e1 ) -> 
        {
        if(Desktop.isDesktopSupported())
            {   // web access
            try 
                { 
                Desktop.getDesktop().browse(new URI( sHttp ) ); 
                }
            catch ( IOException | URISyntaxException e2 )
                { 
                } 
            }
        });
    // Action listener for cancel button
    b2.addActionListener( ( ActionEvent e ) -> 
        {
        dialog.dispose();
        });
    // Visual window and return    
    dialog.setContentPane( p1 );
    dialog.setSize( 300, 150 );
    dialog.setResizable( false );
    dialog.setLocationRelativeTo( application );
    dialog.setVisible( true );
    }
}

