/*

NIOBench, file I/O Benchmark utility. (C)2018 IC Book Labs
RTM is Report Table Model, used when text report save.

*/

package niobench;

import javax.swing.table.AbstractTableModel;

// RTM, NIO Benchmark Report Table Model, used for options values add to report
public class RTM extends AbstractTableModel
    {
    // table model class fields
    private String[] columnsNames;
    private String[][] rowsValues;
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
        if ( row<rowsValues.length ) 
             { return rowsValues[row][column]; }
        else { return ""; }
        }
    };
