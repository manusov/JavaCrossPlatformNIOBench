/*

NIOBench, file I/O Benchmark utility. (C)2018 IC Book Labs
Window "Report", select location for text report save.

*/

package niobench;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.io.*;

public class ActionReport {
private static final JFileChooser FC = new JFileChooser();
private static final String FILE_NAME = "report.txt";
private static FileNameExtensionFilter filter;
    
// Entry point for RT = "Report this" dialogue method, setup GUI
public void createDialogRT
      ( JFrame parentWin ,
        AbstractTableModel atm1 , AbstractTableModel atm2 ,
        String longName , String vendorVersion ,
        String logName , AbstractTableModel atm3 )
    {
    FC.setDialogTitle("Report - select directory");
    filter = new FileNameExtensionFilter ( "Text files" , "txt" );
    FC.setFileFilter(filter);
    FC.setFileSelectionMode(JFileChooser.FILES_ONLY);
    FC.setSelectedFile(new File(FILE_NAME));
    // (re)start dialogue
    boolean inDialogue = true;
    while(inDialogue)
        {
        int select = FC.showSaveDialog(parentWin);
        // save file
        if(select==JFileChooser.APPROVE_OPTION)
            {
            String s1 = FC.getSelectedFile().getPath();
            int x0 = JOptionPane.YES_OPTION;
            // check file exist and warning message
            File file = new File(s1);
            if( file.exists() == true )
                {
                x0 = JOptionPane.showConfirmDialog
                    ( null, 
                    "File exist: " + s1 + "\noverwrite?" , "REPORT" ,
                    JOptionPane.YES_NO_CANCEL_OPTION ,
                    JOptionPane.WARNING_MESSAGE );  // or QUESTION_MESSAGE
                }
            // Select operation by user selection
            if ( ( x0 == JOptionPane.NO_OPTION  ) |
                 ( x0 == JOptionPane.CLOSED_OPTION ) )
                { continue; }
            if ( x0 == JOptionPane.CANCEL_OPTION ) 
                { inDialogue = false; continue; }
            // continue prepare for save file
            String s2 = "Report file.\r\n" + 
                        longName + "\r\n" +
                        vendorVersion + "\r\n\r\n";
            String s3 = "" , s4 = "" , s5 = "" , s6 = "";
            // make and save report
            if ( atm1 != null ) { s3 = tableReport(atm1); }
            if ( atm2 != null ) { s4 = tableReport(atm2); }
            // log support
            if ( logName != null ) { s5 = logName; }
            if ( atm3 != null )    { s6 = tableReport(atm3); }
            // save report
            saveReport( parentWin, s1, s2 + s3 + "\r\n" + s4 + s5 + s6 );
            inDialogue = false;
            }  
        else
            { 
            inDialogue = false; 
            }
        }    // End of save dialogue cycle
    }        // End of method

// Helper method for convert table model to string
private static String tableReport (AbstractTableModel atm) {
    String report="";
    if (atm==null) { return report; }
    // Continue if table exist, get geometry
    int m = atm.getColumnCount();
    int n = atm.getRowCount();
    String s;
    int a;
    int[] maxcols = new int[m];
    int maxcol=13;
    // Get column names lengths
    for (int i=0; i<m; i++)
        { maxcols[i] = atm.getColumnName(i).length(); }
    // Get column maximum lengths
    for (int j=0; j<n; j++)
        {
        for (int i=0; i<m; i++)
            {
            s = (String)atm.getValueAt(j,i);
            a = s.length();
            if (a>maxcols[i]) { maxcols[i]=a; }
            }
        }
    for ( int i=0; i<maxcols.length; i++ ) { maxcol += maxcols[i]; }
    // Write table up
    for (int i=0; i<m; i++)
        {
        s = atm.getColumnName(i);
        report = report + " " + s;
        a = maxcols[i] - s.length() + 1;
        for (int k=0; k<a; k++) { report = report + " "; }
        }
    // Write horizontal line
    report = report + "\r\n";
    for (int i=0; i<maxcol; i++) { report = report + "-"; }
    report = report + "\r\n";
    // Write table content
    for (int j=0; j<n; j++)       // this cycle for rows , n = rows count
        {
        for (int i=0; i<m; i++)   // this cycle for columns , m = columns count
            {
            s = " " + (String)atm.getValueAt(j,i);
            report = report + s;
            a = maxcols[i] - s.length() + 2;
            for (int k=0; k<a; k++) { report = report + " "; }
            }
            report = report + "\r\n";    
        }
    // Write horizontal line
    for (int i=0; i<maxcol; i++) { report = report + "-"; }
    report = report + "\r\n";
    // Return
    return report; }

// Helper method for save string to file and visual status
private static void saveReport
                   ( JFrame parentWin, String filePath, String fileData ) {
    int status=0;
    try ( FileWriter writer = new FileWriter(filePath, false) )
        { writer.write(fileData); writer.flush(); }
    catch(Exception ex) { status=1; }
            
    if (status==0)  {
                    JOptionPane.showMessageDialog
                    (parentWin, "Report saved: " + filePath, "REPORT",
                    JOptionPane.WARNING_MESSAGE); }
            else    {
                    JOptionPane.showMessageDialog
                    (parentWin, "Write report failed", "ERROR",
                    JOptionPane.ERROR_MESSAGE); }
    }

}

