//---------- NIOBench, file I/O Benchmark utility. (C)2018 IC Book Labs --------
// Main class module, application initializing and dialogue window.
// NOTE FROM VERSION V0.17 USE JDK7 MODE.
// BUG 1. MUST CHECK RETURN FROM METHODS transferTo() , transferFrom(),
//        repeat cycle if returned < required.
// BUG 2. FILE CLOSE MUST BE IN THE MEASUREMENT INTERVAL, BECAUSE WRITEBACK
//        close() method must be called between nanotime() method calls.
//----------
// PAL, IOPB, IPB, OPB can be centralized as part of solutions pool.
//
// NATIVE MODE:
// RUN WRITE-READ-COPY FRAGMENT FOR ONE FILE WITH N ITERATIONS
//----------

package niobench;

import java.awt.Dimension;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Random;

public class NIOBench  extends JFrame {

public Object thisFrame = this;

//--- This data moved to About.java at version 0.46 ---
// private final static String version       = "v0.46";
// private final static String vendorVersion = "(C)2016 IC Book Labs";
// private final static String shortName     = "NIOBench " + version;
// private final static String longName      = "Java " + shortName;
//---

private static String filePath = "";
private static int fileSize = 0;
private static int fileCount = 0;

private final static int fileSizes[]  = 
    { 1, 10, 100, 200, 400, 1024 };
private final static int fileCounts[] = 
    { 3, 4, 5, 6, 7, 8, 9, 10, 20, 50, 100 };
private static String dataModes[] =
    { "Zeroes" , "Software RNG" , "Hardware RNG" };   // item #2 can be updated
private static String ioModes[] =
    { 
    "Asynchronous (default)" , 
    "Synchronize write+copy" , 
    "Synchronize write"      ,
    "Synchronize+sparse"     ,
    "Unbuffered native"                    // item #4 can be updated, f(config)
    };

private final static String wrongHRNG =
        "<html><font color=gray>Hardware RNG<html><font color=red> ?";
private final static String slowHRNG =
//      "<html><font color=black>Hardware RNG<html><font color=green> ?";
        "<html>Hardware RNG<html><font color=green> ?";
private final static String wrongNative =
        "<html><font color=gray>Unbuffered native<html><font color=red> ?";

private static enum RNG_STATUS { OK , WRONG_CPU, WRONG_OS, WRONG_JRE }
private static RNG_STATUS rs = RNG_STATUS.OK;
private static enum MODE_STATUS { OK , WRONG_OS }
private static MODE_STATUS ms = MODE_STATUS.OK;

private final static Dimension sizeWindow   = new Dimension (400, 360 + 30 );
private final static Dimension sizePath     = new Dimension (200, 21);
private final static Dimension sizeCombo    = new Dimension (200,21);
private final static Dimension sizeProgress = new Dimension (200,21);
private final static Dimension sizeButton   = new Dimension (89, 24);
private final static Dimension sizeLabel    = new Dimension (44, 21);

private final static int ay1 = 1;
private final static int ay2 = - sizeWindow.height / 2 - 28 - 15;
private final static int ax1 = 1;
private final static int ax2 = 0;

private final static int by1 = 19;
private final static int by2 = 6;
private final static int bx1 = 32;
private final static int bx2 = 1;
private final static int bx3 = 5;

private final static int cy1 = -3;
private final static int cx1 = -3;
private final static int cx2 = -3;

private static SpringLayout sl1;
private static JPanel p1;
private static JTable t1;
private static NBTM nbtm1;
private static RTM rtm1, rtm2;
private static JScrollPane sp1;
private static DefaultTableCellRenderer r1;
private static JLabel l1, l2, l3, l4, l5, l6;
private static JTextField a1;
private static JComboBox c1, c2, c3, c4;
private static JProgressBar g1;
private static DefaultBoundedRangeModel gm1;
private static JButton b1, b2, b3, b4, b5;

private static BBrowse      lst1;
private static BRun         lst2;
private static BAbout       lst3;
private static BReport      lst4;
private static BCancel      lst5;
private static RNGChecks    lst6;
private static NativeChecks lst7;

private static ActionBrowse   aBrowse;
private static ActionRun      aRun;
private static ActionAbout    aAbout;
private static ActionReport   aReport;

private PAL pal;
private int jniStatus, palStatus;
private long q;
private long[] ipb, opb;
private final static int IPBQWORDS = 512, OPBQWORDS = 512;

private final static int CHECK_RDRAND = 0;
private final static int GET_RANDOM_DATA = 0;
public final static int API_WRITE_READ_COPY_DELETE = 1;

private boolean rdrandAvailable = false;

private byte[] zeroData , srngData , hrngData;
private long[] qData;
private int bSize , qSize;
private byte[] sendData;

private boolean logValid = false;
private double[][]  logValues;     // major order: Read, Write, Copy
private boolean[][] logTags;
private String logName =
        "\r\nDetail log.\r\n" +
        "All values in MBPS = Megabytes per Second.\r\n" +
        "Tag notes: M = Median\r\n\r\n";

private String[] logColumns = 
        { "Read", "Tag  ", "Write", "Tag  ", "Copy", "Tag  " };
private String logV = "M", logN = " ";
private static RTM rtm3 = null;
private static String logHeader = null;
private static final int numLog = 3;     // 0=Read, 1=Write, 2=Copy
private static final int defLog = 10;    // Default 10 lines, for debug only

//---------- Window constructor entry point ------------------------------------

public NIOBench()
    {
    super( About.getShortName() );
    setDefaultCloseOperation(EXIT_ON_CLOSE);

//---------- Initializing native class, load native library --------------------

    ipb = new long[IPBQWORDS]; for ( int i=0; i<IPBQWORDS; i++ ) { ipb[i]=0; }
    opb = new long[OPBQWORDS]; for ( int i=0; i<OPBQWORDS; i++ ) { opb[i]=0; }
    pal = new PAL();
    jniStatus = pal.loadUserModeLibrary();
    palStatus=-1;
    if (jniStatus==0)
        {
        try { palStatus = pal.checkPAL(); }
        catch (Exception e1)            { palStatus = -2; }
        catch (UnsatisfiedLinkError e2) { palStatus = -3; }
        }

// ********** DEBUG ************************************************************

/*
    System.out.println( "\r\nJNI" + palStatus );
*/

// ********** END DEBUG ********************************************************
    
    //--- check OS support by native library exist ---
    if ( palStatus<=0 )
        {
        rs = RNG_STATUS.WRONG_OS;
        ms = MODE_STATUS.WRONG_OS;
        dataModes[2] = wrongHRNG;
        ioModes[4] = wrongNative;
        }
    
    //--- check CPU support of RDRAND instruction ---
    opb[0] = 0;
    jniStatus = 0;
    if ( palStatus > 0 )
        {
        jniStatus = pal.entryPAL( null, opb, CHECK_RDRAND, OPBQWORDS );
        }
    q = opb[0];
    if ( ( jniStatus > 0 ) && ( q==1 ) )  { rdrandAvailable = true; }

    if ( ( jniStatus > 0 ) && ( rdrandAvailable==false ) )
        {
        rs = RNG_STATUS.WRONG_CPU;
        dataModes[2] = wrongHRNG;
        }
        
    if ( ( palStatus==33 ) && ( rdrandAvailable==true ) )
        {
        rs = RNG_STATUS.WRONG_JRE;
        dataModes[2] = slowHRNG;
        }
        
    //--- initializing arrays of data ---
    // better when start, otherwise performance effects
    bSize = TargetCommands.getBlockSize();
    qSize = bSize / 8;
    zeroData = new byte[bSize];
    srngData = new byte[bSize];
    hrngData = new byte[bSize];
    qData    = new long[qSize];
    //--- Initializing zeroes data array ---
    for (int i=0; i<zeroData.length; i++) { zeroData[i]=0; }
    //--- Initializing software random number generator data array ---
    Random softRng = new Random();
    softRng.nextBytes(srngData);
    //--- Initializing hardware random number generator data array ---
    if ( rdrandAvailable == true )
        { 
        ipb[0] = GET_RANDOM_DATA;
        ipb[1] = qSize;
        pal.entryPAL( ipb, qData, IPBQWORDS, qSize );
        int k=0;
        for ( int i=0; i<qData.length; i++ )
            {
            q = qData[i];
            for ( int j=0; j<8; j++ )
                {
                hrngData[k] = (byte)( q & 0xFF );
                k++;
                q >>>= 8;
                }
            }
        }
    
//---------- Default settings for log subsystem --------------------------------
// defLog = fixed items count for debug purpose only

    logValid = false;
    logValues = new double  [numLog][defLog];
    logTags   = new boolean [numLog][defLog];
    for (int i=0; i<numLog; i++)
        {
        for (int j=0; j<defLog; j++)
            {
            logValues[i][j] = 0.0;
            logTags[i][j] = false;
            }
        }

//---------- Start built user interface ----------------------------------------

    sl1 = new SpringLayout();
    p1 = new JPanel(sl1);
    //--- table ---
    nbtm1 = new NBTM();
    t1 = new JTable(nbtm1);
    sp1 = new JScrollPane(t1);
    r1 = new DefaultTableCellRenderer();
    r1.setHorizontalAlignment(SwingConstants.CENTER);
    for ( int i=0; i<t1.getColumnCount(); i++ )
        { t1.getColumnModel().getColumn(i).setCellRenderer(r1); }
    //--- labels ---
    l1 = new JLabel("Path");
    l2 = new JLabel("File");
    l3 = new JLabel("Count");
    l4 = new JLabel("Data");
    l5 = new JLabel("Mode");
    l6 = new JLabel("Run");
    l1.setPreferredSize(sizeLabel);
    l2.setPreferredSize(sizeLabel);
    l3.setPreferredSize(sizeLabel);
    l4.setPreferredSize(sizeLabel);
    l5.setPreferredSize(sizeLabel);
    l6.setPreferredSize(sizeLabel);
    //--- text field, path ---
    a1 = new JTextField();
    a1.setPreferredSize(sizePath);
    //--- combo boxes ---
    c1 = new JComboBox();
    c2 = new JComboBox();
    c3 = new JComboBox();
    c4 = new JComboBox();
    c1.setPreferredSize(sizeCombo);
    c2.setPreferredSize(sizeCombo);
    c3.setPreferredSize(sizeCombo);
    c4.setPreferredSize(sizeCombo);
    //--- setup combo 1, data for SIZES combo box ---
    for ( int i=0; i<fileSizes.length; i++ )
        { c1.addItem( Integer.toString( fileSizes[i] ) + " MB" ); }
    c1.setSelectedIndex( fileSizes.length - 1 );
    //--- setup combo 2, data for COUNTS combo box ---
    for ( int i=0; i<fileCounts.length; i++ )
        { c2.addItem( Integer.toString( fileCounts[i] ) ); }
    c2.setSelectedIndex( 7 );
    //--- setup combo 3, data for DATA combo box ---
    for ( int i=0; i<2; i++ )
        { c3.addItem( dataModes[i] ); }
    c3.setSelectedIndex( 0 );
    //--- this option f(HRNG) support ---
    // if ( rdrandAvailable == true ) { c3.addItem( dataModes[2] ); }
    // changed at v0.19
    c3.addItem( dataModes[2] );
    lst6 = new RNGChecks();
    c3.addActionListener(lst6);
    //--- setup combo 4, i/o mode ---
    for ( int i=0; i<ioModes.length; i++ )
        { c4.addItem( ioModes[i] ); }
    c4.setSelectedIndex( 0 );
    //--- this option f(OS) support ---
    lst7 = new NativeChecks();
    c4.addActionListener(lst7);
    //--- progress indicator ---
    gm1 = new DefaultBoundedRangeModel(0,0,0,100);
    g1  = new JProgressBar(gm1);
    g1.setPreferredSize(sizeProgress);
    g1.setStringPainted(true);
    g1.setString("Please run...");
    //--- buttons ---
    b1 = new JButton("Browse");
    b2 = new JButton("Run");
    b3 = new JButton("About");
    b4 = new JButton("Report");
    b5 = new JButton("Cancel");
    b1.setPreferredSize(sizeButton);
    b2.setPreferredSize(sizeButton);
    b3.setPreferredSize(sizeButton);
    b4.setPreferredSize(sizeButton);
    b5.setPreferredSize(sizeButton);
    
//---------- Connect button listeners ------------------------------------------

    lst1 = new BBrowse();
    lst2 = new BRun();
    lst3 = new BAbout();
    lst4 = new BReport();
    lst5 = new BCancel();
    b1.addActionListener(lst1);
    b2.addActionListener(lst2);
    b3.addActionListener(lst3);
    b4.addActionListener(lst4);
    b5.addActionListener(lst5);

//---------- Layout control, components positioning ----------------------------

    //--- results table ---
    sl1.putConstraint ( SpringLayout.NORTH, sp1, ay1, SpringLayout.NORTH, p1  );
    sl1.putConstraint ( SpringLayout.SOUTH, sp1, ay2, SpringLayout.SOUTH, p1  );
    sl1.putConstraint ( SpringLayout.WEST,  sp1, ax1, SpringLayout.WEST,  p1  );
    sl1.putConstraint ( SpringLayout.EAST,  sp1, ax2, SpringLayout.EAST,  p1  );
    //--- left labels ---
    sl1.putConstraint ( SpringLayout.NORTH, l1,  by1, SpringLayout.SOUTH, sp1 );
    sl1.putConstraint ( SpringLayout.WEST,  l1,  bx1, SpringLayout.WEST,  p1  );
    sl1.putConstraint ( SpringLayout.NORTH, l2,  by2, SpringLayout.SOUTH, l1  );
    sl1.putConstraint ( SpringLayout.WEST,  l2,  bx1, SpringLayout.WEST,  p1  );
    sl1.putConstraint ( SpringLayout.NORTH, l3,  by2, SpringLayout.SOUTH, l2  );
    sl1.putConstraint ( SpringLayout.WEST,  l3,  bx1, SpringLayout.WEST,  p1  );
    sl1.putConstraint ( SpringLayout.NORTH, l4,  by2, SpringLayout.SOUTH, l3  );
    sl1.putConstraint ( SpringLayout.WEST,  l4,  bx1, SpringLayout.WEST,  p1  );
    sl1.putConstraint ( SpringLayout.NORTH, l5,  by2, SpringLayout.SOUTH, l4  );
    sl1.putConstraint ( SpringLayout.WEST,  l5,  bx1, SpringLayout.WEST,  p1  );
    sl1.putConstraint ( SpringLayout.NORTH, l6,  by2, SpringLayout.SOUTH, l5  );
    sl1.putConstraint ( SpringLayout.WEST,  l6,  bx1, SpringLayout.WEST,  p1  );
    //--- edit path, combo boxes, progress indicator ---
    sl1.putConstraint ( SpringLayout.NORTH, a1,  by1, SpringLayout.SOUTH, sp1 );
    sl1.putConstraint ( SpringLayout.WEST,  a1,  bx2, SpringLayout.EAST,  l1  );
    sl1.putConstraint ( SpringLayout.NORTH, c1,  by2, SpringLayout.SOUTH, l1  );
    sl1.putConstraint ( SpringLayout.WEST,  c1,  bx2, SpringLayout.EAST,  l2  );
    sl1.putConstraint ( SpringLayout.NORTH, c2,  by2, SpringLayout.SOUTH, l2  );
    sl1.putConstraint ( SpringLayout.WEST,  c2,  bx2, SpringLayout.EAST,  l3  );
    sl1.putConstraint ( SpringLayout.NORTH, c3,  by2, SpringLayout.SOUTH, l3  );
    sl1.putConstraint ( SpringLayout.WEST,  c3,  bx2, SpringLayout.EAST,  l4  );
    sl1.putConstraint ( SpringLayout.NORTH, c4,  by2, SpringLayout.SOUTH, l4  );
    sl1.putConstraint ( SpringLayout.WEST,  c4,  bx2, SpringLayout.EAST,  l5  );
    sl1.putConstraint ( SpringLayout.NORTH, g1,  by2, SpringLayout.SOUTH, l5  );
    sl1.putConstraint ( SpringLayout.WEST,  g1,  bx2, SpringLayout.EAST,  l6  );
    //--- path browse button ---
    sl1.putConstraint ( SpringLayout.NORTH, b1,    0, SpringLayout.NORTH, a1  );
    sl1.putConstraint ( SpringLayout.WEST,  b1,  bx3, SpringLayout.EAST,  a1  );
    //--- run button ---
    sl1.putConstraint ( SpringLayout.NORTH, b2,    0, SpringLayout.NORTH, g1  );
    sl1.putConstraint ( SpringLayout.WEST,  b2,  bx3, SpringLayout.EAST,  g1  );
    //--- 4 down buttons from left to right ---
    sl1.putConstraint ( SpringLayout.SOUTH, b5,  cy1, SpringLayout.SOUTH, p1  );
    sl1.putConstraint ( SpringLayout.EAST,  b5,  cx1, SpringLayout.EAST,  p1  );
    sl1.putConstraint ( SpringLayout.SOUTH, b4,  cy1, SpringLayout.SOUTH, p1  );
    sl1.putConstraint ( SpringLayout.EAST,  b4,  cx2, SpringLayout.WEST,  b5  );
    sl1.putConstraint ( SpringLayout.SOUTH, b3,  cy1, SpringLayout.SOUTH, p1  );
    sl1.putConstraint ( SpringLayout.EAST,  b3,  cx2, SpringLayout.WEST,  b4  );
    
//---------- Add components and visual window ----------------------------------    
    
    p1.add(sp1);
    p1.add(l1);  p1.add(l2);  p1.add(l3);  p1.add(l4); p1.add(l5); p1.add(l6);
    p1.add(a1);
    p1.add(c1);  p1.add(c2); p1.add(c3); p1.add(c4);
    p1.add(g1);
    p1.add(b1);  p1.add(b2);  p1.add(b3);  p1.add(b4);  p1.add(b5);
    add(p1);
    setLocationRelativeTo(null);
    setSize(sizeWindow);
    setVisible(true);
    }

//---------- JComboBox listener for Data option --------------------------------

private class RNGChecks implements ActionListener
    {
    public void actionPerformed (ActionEvent e)
        {
        int x = c3.getSelectedIndex();
        if (x==2)    // item 2 requires hardware RNG
            {
            switch(rs)
                {
                case WRONG_CPU:
                    {
                    JOptionPane.showMessageDialog ( null,
                    "RDRAND instruction not supported by CPU:\n" +
                    "use software RNG" ,
                    About.getShortName() , JOptionPane.WARNING_MESSAGE );
                    c3.setSelectedIndex(1);
                    break;
                    }
                case WRONG_OS:
                    {
                    JOptionPane.showMessageDialog ( null, 
                    "Native library not loaded:\n" + 
                    "use software RNG" ,
                    About.getShortName() , JOptionPane.WARNING_MESSAGE );
                    c3.setSelectedIndex(1);
                    break;
                    }
                case WRONG_JRE:
                    {
                    JOptionPane.showMessageDialog ( null,
                    "JRE32 under WOW64 mode detected:\n" +
                    "hardware RNG performance downgraded" ,
                    About.getShortName() , JOptionPane.WARNING_MESSAGE );
                    break;
                    }
                }
            }
        }
    }

//---------- JComboBox listener for I/O mode option ----------------------------

private class NativeChecks implements ActionListener
    {
    public void actionPerformed (ActionEvent e)
        {
        int x = c4.getSelectedIndex();
        if (x==4)    // item 4 requires OS API access
            {
            switch(ms)
                {
                case WRONG_OS:
                    {
                    JOptionPane.showMessageDialog ( null, 
                    "Native library not loaded:\n" + 
                    "OS API direct control is not available" ,
                    About.getShortName() , JOptionPane.WARNING_MESSAGE );
                    c4.setSelectedIndex(0);
                    break;
                    }
                }                    // Settings because slow methods:
            c1.setSelectedIndex(2);  // Set file size = 10 MB
            c2.setSelectedIndex(2);  // Set files count = 5
            }
        }
    }

//---------- Buttons listeners, handlers for buttons press ---------------------

private class BBrowse implements ActionListener  // BROWSE button
    {
    public void actionPerformed (ActionEvent e)
        { 
        aBrowse = new ActionBrowse();
        String s1 = aBrowse.selectFile( a1.getText() );
        a1.setText(s1);
        }
    }

private class BRun implements ActionListener  // RUN button
    {
    public void actionPerformed (ActionEvent e)
        {
        if ( !(ActionRun.getRunning()) )
            {
            aRun = new ActionRun();
            filePath = a1.getText();
            int i = c1.getSelectedIndex();  // file size option
            int j = c2.getSelectedIndex();  // file count option
            int k = c3.getSelectedIndex();  // data mode option (zero,rng ...)
            int m = c4.getSelectedIndex();  // i/o mode option (async, sync ...)
            
            fileSize  = fileSizes[i];
            fileCount = fileCounts[j];
            
            switch(k)
                {
                case 0:  sendData = zeroData; break;
                case 1:  sendData = srngData; break;
                case 2:  sendData = hrngData; break;
                default: sendData = null;
                }
            
            Object[] disabledComponents = new Object[9];
            disabledComponents[0] = b1;    // skip b2=run, for redefine to stop
            disabledComponents[1] = b3;
            disabledComponents[2] = b4;
            disabledComponents[3] = b5;
            disabledComponents[4] = a1;
            disabledComponents[5] = c1;
            disabledComponents[6] = c2;
            disabledComponents[7] = c3;
            disabledComponents[8] = c4;
            
            logValid = true;
            logValues = new double  [numLog][fileCount];
            logTags   = new boolean [numLog][fileCount];
            for (int a=0; a<numLog; a++)
                {
                for (int b=0; b<fileCount; b++)
                    {
                    logValues[a][b] = 0.0;
                    logTags[a][b] = false;
                    }
                }
            aRun.setupBenchmark 
                ( nbtm1 , gm1 , g1 , 
                  b2 , lst2 ,
                  disabledComponents , (JFrame)thisFrame ,
                  filePath , fileSize , fileCount , sendData , m ,
                  logValues , logTags );
            
            aRun.start();
            }
        }
    }

private class BAbout implements ActionListener  // ABOUT button
    {
    public void actionPerformed (ActionEvent e)
        { 
        aAbout = new ActionAbout();
        final JDialog dialog = aAbout.createDialog
            ( (JFrame)thisFrame ,
               About.getLongName() , About.getVendorName() );
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        }
    }

private class BReport implements ActionListener  // REPORT button 
    {
    public void actionPerformed (ActionEvent e)
        { 
        String[] s1;
        String[][] s2;
        int n, m;
        //--- prepare first table model for options ( nbtm2 added at v0.16) ---
        //--- options names ---    
        s1 = nbtm1.getColumnsNames();
        //--- options values ---
        n = nbtm1.getRowCount();
        m = nbtm1.getColumnCount();
        s2 = new String[n][m];
        for ( int i=0; i<n; i++ )
            {
            for ( int j=0; j<m; j++ )
                {
                s2[i][j] = nbtm1.getValueAt(i, j);
                }
            }
        //--- make table for report measurement results ---
        rtm1 = new RTM();
        rtm1.setColumnsNames(s1);
        rtm1.setRowsValues(s2);
        //--- prepare second table model for options ---
        s1 = new String[] { "Option" , "Value" };
        //--- options names ---
        s2 = new String[5][2];
        s2[0][0] = l1.getText();
        s2[1][0] = l2.getText();
        s2[2][0] = l3.getText();
        s2[3][0] = l4.getText();
        s2[4][0] = l5.getText();
        //--- options values ---
        s2[0][1] = a1.getText();
        s2[1][1] = (String) c1.getSelectedItem();
        s2[2][1] = (String) c2.getSelectedItem();
        s2[3][1] = (String) c3.getSelectedItem();
        s2[4][1] = (String) c4.getSelectedItem();
        //--- make table for report options values ---
        rtm2 = new RTM();
        rtm2.setColumnsNames(s1);
        rtm2.setRowsValues(s2);
        //--- prepare second table model for results log ---
        rtm3 = null;
        logHeader = null;
        if ( logValid == true )
            {
            logHeader = logName;
            s1 = logColumns;
            n = logValues[0].length;
            m = logValues.length + logTags.length;
            s2 = new String[n][m];
            for (int i=0; i<n; i++)
                {
                s2[i][0] = String.format( "%.2f", logValues[0][i] );  // Read
                if ( logTags[0][i] ) 
                    { s2[i][1] = logV; } else { s2[i][1] = logN; }
                s2[i][2] = String.format( "%.2f", logValues[1][i] );  // Write
                if ( logTags[1][i] ) 
                    { s2[i][3] = logV; } else { s2[i][3] = logN; }
                s2[i][4] = String.format( "%.2f", logValues[2][i] );  // Copy
                if ( logTags[2][i] ) 
                    { s2[i][5] = logV; } else { s2[i][5] = logN; }
                }
            //--- make table for report options values ---
            rtm3 = new RTM();
            rtm3.setColumnsNames(s1);
            rtm3.setRowsValues(s2);
            }
        //--- call report dialogue and report save ---
        aReport = new ActionReport();
        aReport.createDialogRT
            // ( (JFrame)thisFrame , nbtm1 , rtm1 , longName , vendorVersion );
            ( (JFrame)thisFrame , rtm1 , rtm2 ,
               About.getLongName() , About.getVendorName() , 
              logHeader , rtm3 );
        }
    }

private class BCancel implements ActionListener  // CANCEL button
    {
    public void actionPerformed (ActionEvent e)
        {
            System.exit(0);
        }
    }

//---------- Application entry point -------------------------------------------

public static void main(String[] args) 
    {
    JFrame.setDefaultLookAndFeelDecorated(true);
    JDialog.setDefaultLookAndFeelDecorated(true);
    JFrame a = new NIOBench();
    a.setResizable(false); 
    }
    
}
