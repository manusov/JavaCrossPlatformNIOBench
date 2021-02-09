/*

NIOBench, file I/O Benchmark utility. (C)2018 IC Book Labs
Benchmark operation, run target file IO and visual results.

*/

package niobench;

import static java.lang.Thread.sleep;
import java.awt.event.*;
import javax.swing.*;
import static javax.swing.JFrame.*;

public class ActionRun extends Thread 
{
private static TargetCommands tCommands;
private static String[][] rowsValues;
private static double[] mbps , deviation;
private static double megabytes, passes;

private static double average1, minimum1, maximum1,
                      average2, minimum2, maximum2;

private static double percentage, weight;
private static int n, k;
private static boolean localflag = false;

private static NBTM tableModel;
private static DefaultBoundedRangeModel progressModel;
private static JProgressBar progressBar;

private static JButton runStopButton;
private static String runStopName;
private static ActionListener runStopListener , bstop;

private static Object[] userInterface;
private static JFrame benchFrame;

private static int e1;
private static boolean b1;
private static boolean lastRequired;

private static double[][] refLogValues;
private static boolean[][] refLogTags;

// Helper service methods
public void setupBenchmark 
    ( NBTM tm1 , DefaultBoundedRangeModel gm1 , JProgressBar g1 ,
      JButton rsb , ActionListener rsl ,
      Object[] ui ,
      JFrame f1 ,
      String filePath , int fileSize , int fileCount ,
      byte[] testData , int ioMode ,
      double[][] logValues , boolean[][] logTags )
    {
    // parameters for log
    refLogValues = logValues;
    refLogTags = logTags;
    // selection by IO mode
    switch (ioMode)
        {
        case 0 : { tCommands  = new TargetCommands();         break; }
        case 1 : { tCommands  = new TargetCommandsSync();     break; }
        case 2 : { tCommands  = new TargetCommandsSyncW();    break; }
        case 3 : { tCommands  = new TargetCommandsSyncRW();   break; }
        case 4 : { int x = PAL.getNativeType();
                   if ( (x==0)|(x==1) )
                   { tCommands  = new TargetCommandsWindows(); }
                   if ( (x==2)|(x==3) )
                   { tCommands  = new TargetCommandsLinux(); }
                   break; }
        }
    // pass parameters, prepare for run thread
    tCommands.clearNanoseconds(fileCount);
    tCommands.setFilePath  ( filePath  );
    tCommands.setFileSize  ( fileSize  );
    tCommands.setFileCount ( fileCount );
    tCommands.setData      ( testData  );
    tCommands.setCounter   ( 0         );
    tCommands.setFlag      ( true      );   // this critical for button change
    tCommands.setInterrupt ( false     );   // this for interruptable
    // initializing thread objects
    tableModel = tm1;
    progressModel = gm1;
    progressBar = g1;
    // button and listener
    runStopButton = rsb;
    runStopListener = rsl;
    // disabled components and frame
    userInterface = ui;
    benchFrame = f1;
    // operational parameters
    megabytes = fileSize;
    passes = fileCount;
    // change run flag and button text from "Run" to "Stop"
    localflag = true;                                    // ENTER TO CRITICAL
    // this for benchmarks interruptable, setup context
    runStopName = runStopButton.getText();
    runStopButton.setText("Stop");
    bstop = new BStop();
    runStopButton.removeActionListener(runStopListener);
    runStopButton.addActionListener(bstop);
    // end of prepare interruptable context
    // disable (make gray) some GUI objects during test
    for (Object uitemp : userInterface) 
        {
        ( (JComponent) uitemp ).setEnabled(false);
        ( (JComponent) uitemp ).repaint();
        ( (JComponent) uitemp ).revalidate();
        }
    // prevent application window close during test
    benchFrame.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    // show progress indicator 0%
    percentage = 0.0;
    weight = 95.0 / ( fileCount * 3 );
    progressModel.setValue( (int)percentage );
    progressBar.setString ( progressModel.getValue() + "%" );
    // initializing context for next dynamical updates
    average1=0; minimum1=0; maximum1=0;
    average2=0; minimum2=0; maximum2=0;
    rowsValues = tableModel.getRowsValues();
    n = fileCount;
    mbps = new double[n];
    deviation = new double[n];
    for ( int i=0; i<n; i++ ) { mbps[i] = 0; deviation[i] = 0; }
    }

// Get running status
public static boolean getRunning() 
    {
    return localflag; 
    }
    
// Thread execution
@Override public void run()
    {
    tCommands.start();
    lastRequired = true;
    // Begin execution cycle with dynamical revisual, get results
    // Bug with lastRequired redundant or required for phase 0 must be executed
    while ( tCommands.getFlag() | lastRequired ) 
        {
        // get and visual results
        resultsVisual ( tCommands.getNanosecondsCopy()  , 2 );  // Copy
        resultsVisual ( tCommands.getNanosecondsWrite() , 1 );  // Write
        resultsVisual ( tCommands.getNanosecondsRead()  , 0 );  // Read
        // revisual and some wait
        percentage = weight * tCommands.getCounter();
        progressModel.setValue( (int)percentage );
        progressBar.setString ( progressModel.getValue() + "%" );
        tableModel.setRowsValues(rowsValues);
        tableModel.fireTableDataChanged();
        // break if error or interrupted (v0.20)
        if ( ( e1 != 0 ) | ( b1==true ) ) break;
        try { sleep(50); } catch ( Exception e ) { }
        }
    // End cycle
    // Set progress indicator to 100 percents 
    percentage = 100.0;
    progressModel.setValue( (int)percentage );
    progressBar.setString ( progressModel.getValue() + "%" );
    // this for benchmarks interruptable
    runStopButton.setText(runStopName);
    runStopButton.removeActionListener(bstop);
    runStopButton.addActionListener(runStopListener);
    // end of prepare interruptable context
    // enable (make non-gray) some GUI objects during test
    for (Object uitemp : userInterface) 
        {
        ( (JComponent) uitemp ).setEnabled(true);
        ( (JComponent) uitemp ).repaint();
        ( (JComponent) uitemp ).revalidate();
        }
    // re-enable application window close after test
    benchFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
    // end thread execution
    localflag = false;                                    // EXIT FROM CRITICAL 
    }

// Helper method for results calculate and visual
private void resultsVisual ( long[] nanoseconds , int step )
    {    
    // analyse results, part 1, total averaging
    // k = number of valid results
    k = 0;
    double seconds = 0;
    average1=0; minimum1=0; maximum1=0;
    average2=0; minimum2=0; maximum2=0;
    for ( int i=0; i<n; i++ ) 
        { 
        mbps[i] = 0; 
        deviation[i] = 0;
        refLogValues[step][i] = 0.0;
        refLogTags[step][i] = false;
        }
    for ( int i=0; i<n; i++ )
        {
        seconds = nanoseconds[i];
        seconds /= 1000000000;    // divide must be floating point, not int
        if ( seconds != 0.0 ) 
            {
            mbps[i] = megabytes / seconds; 
            k++;
            refLogValues[step][i] = mbps[i];
            }
        }
    minimum1 = mbps[0];
    maximum1 = mbps[0];
    for ( int i=0; i<k; i++ )
        {
        average1 += mbps[i];
        if ( mbps[i] < minimum1 ) { minimum1 = mbps[i]; }
        if ( mbps[i] > maximum1 ) { maximum1 = mbps[i]; }
        }
    if ( k != 0) { average1 /= k; }
    // analyse results, part 2 median
    if ( k == passes )
    {
        double temp1;
        int temp2;
        boolean flag1;
        int n1 = refLogValues[0].length;
        int m1;
        int k1,k2,k3;
        int[] serv = new int[n1];
        for ( int i1=0; i1<n1; i1++ ) { serv[i1] = i1; }
        flag1 = true;
        while ( flag1 == true )
        {
        flag1 = false;
        for ( int i1=0; i1<n1-1; i1++ )
            {
            if ( mbps[i1] > mbps[i1+1] )
                {
                temp1=mbps[i1]; mbps[i1]=mbps[i1+1]; mbps[i1+1]=temp1;
                temp2=serv[i1]; serv[i1]=serv[i1+1]; serv[i1+1]=temp2;
                flag1 = true;
                }
            }
        }            
        average2=0.0; minimum2=0.0; maximum2=0.0;
        m1 = n1/2;
        if ( n1 % 2 == 0 )
        {                               // median for EVEN length case
        k1 = serv[m1-1];                // k = elements numbers
        k2 = serv[m1];
        minimum2 = mbps[m1-1];          // elements valuse
        maximum2 = mbps[m1];
        average2 = ( minimum2 + maximum2 ) / 2.0;
        refLogTags[step][k1] = true;    // mark 2 elements "M"
        refLogTags[step][k2] = true;
        }
        else
        {                               // median for ODD length case
        k1 = serv[m1-1];                // k = elements numbers
        k2 = serv[m1+1];
        k3 = serv[m1];
        minimum2 = mbps[m1-1];          // elements valuse
        maximum2 = mbps[m1+1];
        average2 = mbps[m1];
        refLogTags[step][k1] = true;    // mark 2 elements "M"
        refLogTags[step][k2] = true;
        refLogTags[step][k3] = true;
        }
        // condition = for Read operation reached all iterations done
        if ( step==0 ) { lastRequired = false; }
    }
    // write strings to table model
    e1 = tCommands.getErrorCode();
    b1 = tCommands.getInterrupt();
    // check skipped or error
    if ( ( e1 != 0 ) | ( b1 == true ) )
        {
        String s1;
        if ( b1==true ) { s1 = "skipped"; }
        else { s1 = "ERROR # " +  e1; }
        // cycle for fill all table with error string
        for( int i=0; i<tableModel.getRowCount(); i++ ) 
            { for ( int j=1; j<tableModel.getColumnCount(); j++ )
                { rowsValues[i][j] = s1; } 
            }
        }
        // write results if no errors
        else 
        {
        rowsValues[3+step][1] = String.format( "%.2f", average1 );
        rowsValues[3+step][2] = String.format( "%.2f", minimum1 );
        rowsValues[3+step][3] = String.format( "%.2f", maximum1 );
        rowsValues[0+step][1] = String.format( "%.2f", average2 );
        rowsValues[0+step][2] = String.format( "%.2f", minimum2 );
        rowsValues[0+step][3] = String.format( "%.2f", maximum2 );
        }
    }

// Handler for "Stop" button, redefined from "Run" button
private class BStop implements ActionListener
    {
    @Override public void actionPerformed (ActionEvent e)
        {
        tCommands.setInterrupt(true);
        }
    }

}
