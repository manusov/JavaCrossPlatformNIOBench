/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Log and statistics Table Model for memory benchmark, openable window.
*/

package opentable;

import java.util.Arrays;
import javax.swing.table.AbstractTableModel;

public class StatisticsTableModel extends AbstractTableModel 
{
public StatisticsTableModel()
    {
    rowsValues = initValues;
    }

// clear table for next measurement session, n = measurement entries count
protected void blank( int n ) 
    { 
    rowsValues = new String[ n + 5 ][ getColumnCount() ];
    int i;
    for( i=0; i<n; i++ )
        {
        String[] s = Arrays.copyOf( initValues[0], initValues[0].length );
        s[0] = "" + ( i + 1 );
        rowsValues[i] = s;
        }
    
    // rowsValues[i] = blankValues;
    System.arraycopy( blankValues, 0, rowsValues[i], 0, blankValues.length );
    
    if ( n == 0 ) rowsValues[i][0] = "#";
    else rowsValues[i][0] = "";
    i++;  // skip 1 string
    
    for( int j=1; j<5; j++ )
        {
        rowsValues[i] = Arrays.copyOf ( initValues[j], initValues[j].length );
        i++;
        }
    fireTableStructureChanged();
    }

// table model class fields
private final String[] COLUMNS_NAMES = 
    { "Iteration", "Size" , "MBPS" , "IOPS", "Reserved" };
private String[][] rowsValues = null;
private final String[] blankValues =
        { ""         , "" ,  "" ,  "",  ""  };
private final String[][] initValues =
    { 
        { "#"        , "-" , "-" , "-", "-" } ,
        { "Median"   , "-" , "-" , "-", "-" } ,
        { "Average"  , "-" , "-" , "-", "-" } ,
        { "Minimum"  , "-" , "-" , "-", "-" } ,
        { "Maximum"  , "-" , "-" , "-", "-" } ,
    };
// table model this application-specific public methods
protected String[] getColumnsNames()          { return COLUMNS_NAMES; }
protected String[][] getRowsValues()          { return rowsValues;    }
protected void setRowsValues( String[][] s )  { rowsValues = s;       }
// table model standard required public methods
@Override public int getColumnCount()    { return COLUMNS_NAMES.length; }
@Override public int getRowCount()       { return rowsValues.length;    }
@Override public String getColumnName( int column )
    {
    if ( column < COLUMNS_NAMES.length )   
        return COLUMNS_NAMES[column];
    else
        return "?";
    }
@Override public String getValueAt( int row, int column )
    { 
    if ( ( row < rowsValues.length ) && ( column < COLUMNS_NAMES.length ) )
        return rowsValues[row][column];
    else
        return "";
    }

@Override public void setValueAt( Object x, int row, int column )
    {
    if ( ( row < rowsValues.length ) && ( column < COLUMNS_NAMES.length ) &&
         ( x instanceof String )  )
        rowsValues[row][column] = ( String ) x;
    }

@Override public boolean isCellEditable( int row, int column )
    { return false; }

// update table for each measured value from Report Monitor.
public void measurementNotify( EntryStatistics ests )
    {
    // write new data string
    int count = ests.dataArray.size();
    rowsValues = new String[ count + 1 + 4 ][ getColumnCount() ];
    for( int i=0; i<count; i++ )
        {
        rowsValues[i][0] = "" + ests.dataArray.get( i ).num;
        rowsValues[i][1] = String.format
            ( "%.1f K" , ests.dataArray.get( i ).doubles[0] / 1024.0 );
        rowsValues[i][2] =  String.format
            ( "%.3f" , ests.dataArray.get( i ).doubles[1] );
        rowsValues[i][3] =  String.format
            ( "%.3f" , ests.dataArray.get( i ).doubles[2] );
        rowsValues[i][4] =  String.format
            ( "%.3f" , ests.dataArray.get( i ).doubles[3] );
        }
    // write empty string
    System.arraycopy
        ( blankValues, 0, rowsValues[count], 0, blankValues.length );
    // write statistics, last 4 strings
    for( int i=0; i<4; i++ )
        {
        rowsValues[i+1+count][0] = initValues[i+1][0];
        for ( int j=0; j<4; j++ )
            {
            rowsValues[i+1+count][j+1] = ests.statTable[j][i];
            }
        }
    // notify changes
    fireTableDataChanged();
    }

}
