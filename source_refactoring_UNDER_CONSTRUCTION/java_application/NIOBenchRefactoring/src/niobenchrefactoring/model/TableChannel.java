/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Benchmark results table model for "NIO channels" scenario.
Dual purpose: table model used for build GUI and save text report.
*/

package niobenchrefactoring.model;

import javax.swing.table.AbstractTableModel;

public class TableChannel extends AbstractTableModel
{
/*    
Table model this application-specific fields and public methods
Can override for use this table model at different IO scenarios
*/
private boolean units = false;
    
public void setUnits( boolean units )
    {
    this.units = units;
    }

private String unitsSelect()
    {
    return units ? "IOPS" : "MBPS";
    }

public String[] getColumnsNames()
    {
    return new String[] 
        { "Value, " + unitsSelect() , "Actual" , "Minimum" , "Maximum" };
    }

public String[][] getRowsValues()
    {
    return new String[][]
        { { "Median, Read"    , "-" , "-" , "-" } ,
          { "Write"           , "-" , "-" , "-" } ,
          { "Copy"            , "-" , "-" , "-" } ,
          { "Average, Read"   , "-" , "-" , "-" } ,
          { "Write"           , "-" , "-" , "-" } ,
          { "Copy"            , "-" , "-" , "-" } };
    }

/*
Table model standard required public methods
*/
@Override public int getColumnCount()
    {
    return getColumnsNames().length; 
    }
@Override public int getRowCount()
    {
    return getRowsValues().length;   
    }
@Override public String getColumnName( int column )
    { 
    return getColumnsNames()[column]; 
    }
@Override public String getValueAt( int row, int column )
    { 
    if ( row < getRowsValues().length ) 
        { 
        return " " + getRowsValues()[row][column]; 
        }
    else return "";
    }
@Override public boolean isCellEditable( int row, int column )
    { 
    return false; 
    }
}
