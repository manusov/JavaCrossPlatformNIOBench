/*

NIOBench, file I/O Benchmark utility. (C)2018 IC Book Labs
NBTM is a NIO Benchmark Table Model.

*/

package niobench;

import javax.swing.table.AbstractTableModel;

// NBTM, NIO Benchmark Table Model, used for results visual and report
public class NBTM extends AbstractTableModel
    {
    // table model class fields
    private String[] columnsNames = 
           { "Value, MBPS" , "Actual" , "Minimum" , "Maximum" };
    private String[][] rowsValues =
        { 
            { "Median, Read"    , "-" , "-" , "-" } ,
            { "Write"           , "-" , "-" , "-" } ,
            { "Copy"            , "-" , "-" , "-" } ,
            { "Average, Read"   , "-" , "-" , "-" } ,
            { "Write"           , "-" , "-" , "-" } ,
            { "Copy"            , "-" , "-" , "-" }
        };
    // table model this application-specific public methods
    public void setColumnsNames(String[] s)  { columnsNames = s; }
    public void setRowsValues(String[][] s)  { rowsValues = s; }
    public String[] getColumnsNames()        { return columnsNames; }
    public String[][] getRowsValues()        { return rowsValues; }
    // table model standard required public methods
    @Override public int getColumnCount()    { return columnsNames.length; }
    @Override public int getRowCount()       { return rowsValues.length; }
    @Override public String getColumnName(int column)
        { return columnsNames[column]; }
    @Override public String getValueAt( int row, int column )
        { 
        if   ( row<rowsValues.length ) { return rowsValues[row][column]; }
        else return "";
        }
    @Override public boolean isCellEditable(int row, int column)
        { return false; }
    }
