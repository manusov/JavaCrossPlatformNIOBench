//---------- NIOBench, file I/O Benchmark utility. (C)2018 IC Book Labs --------
// Window "About", vendor and product info, web site, logo picture.

package niobench;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import java.awt.Desktop;
import java.net.URI;

public class ActionAbout {

private final static Color logoColor = new Color(143,49,40);
private final static Dimension sizeBhttp   = new Dimension (180, 25);
private final static Dimension sizeBcancel = new Dimension (89, 25);
    
//--- Entry point for "About" dialogue method, setup GUI ---
public JDialog createDialog
    ( JFrame parentWin , String longName , String vendorVersion )
    {
    final JDialog dialog = new JDialog(parentWin, "About", true);
    dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    
//--- Create GUI components ---    
    SpringLayout sl1 = new SpringLayout();
    JPanel p1 = new JPanel(sl1);
    
    final String sHttp = About.getWebSite();
    String sCancel = "Cancel";
    JLabel l1 = new JLabel ();
    try { l1.setIcon(new javax.swing.ImageIcon(getClass().
           getResource( About.getVendorIcon() ))); } 
    catch (Exception e) { }
    JLabel l2 = new JLabel  ( About.getLongName()   );
    JLabel l3 = new JLabel  ( About.getVendorName() );
    l2.setForeground(logoColor);
    l3.setForeground(logoColor);
    Font font1 = new Font ( "Verdana", Font.PLAIN, 12 );
    l2.setFont(font1);
    l3.setFont(font1);
    JButton b1 = new JButton(sHttp);
    JButton b2 = new JButton(sCancel);
    
    b1.setPreferredSize(sizeBhttp);
    b2.setPreferredSize(sizeBcancel);
                                                
    sl1.putConstraint ( SpringLayout.NORTH, l1,  24, SpringLayout.NORTH, p1 );
    sl1.putConstraint ( SpringLayout.WEST,  l1,  28, SpringLayout.WEST,  p1 );
    sl1.putConstraint ( SpringLayout.NORTH, l2,  24, SpringLayout.NORTH, p1 );
    sl1.putConstraint ( SpringLayout.WEST,  l2,   4, SpringLayout.EAST,  l1 );
    sl1.putConstraint ( SpringLayout.NORTH, l3,   0, SpringLayout.SOUTH, l2 );
    sl1.putConstraint ( SpringLayout.WEST,  l3,   4, SpringLayout.EAST,  l1 );
    
    sl1.putConstraint ( SpringLayout.SOUTH, b1, -10, SpringLayout.SOUTH, p1 );
    sl1.putConstraint ( SpringLayout.WEST,  b1,   8, SpringLayout.WEST,  p1 );  // old 36
    sl1.putConstraint ( SpringLayout.SOUTH, b2, -10, SpringLayout.SOUTH, p1 );
    sl1.putConstraint ( SpringLayout.WEST,  b2,   3, SpringLayout.EAST,  b1 );

    p1.add(l1);
    p1.add(l2);
    p1.add(l3);
    p1.add(b1);
    p1.add(b2);

//--- Action listener for web button ---
    b1.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent ae1) {
    if(Desktop.isDesktopSupported())
        { try { Desktop.getDesktop().browse(new URI(sHttp)); }
          catch (Exception ex1) { } }; }});

//--- Action listener for cancel button ---
    b2.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) { dialog.dispose(); } } );

//--- Visual window and return ---    
    dialog.setContentPane(p1);
    dialog.setSize(300,150);
    dialog.setResizable(false);
    return dialog;  }

}
