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
import static niobenchrefactoring.model.IOscenario.*;

public class TableChannel extends AbstractTableModel
{
/*    
Table model this application-specific fields and public methods
Can override for use this table model at different IO scenarios
*/
private boolean units = false;
String[][] rows = getRowsValues();
    
public void setUnits( boolean units )
    {
    this.units = units;
    }

public void clear()
    {
    rows = getRowsValues();
    fireTableDataChanged();
    }

private String unitsSelect()
    {
    return units ? "IOPS" : "MBPS";
    }

String[] getColumnsNames()
    {
    return new String[] 
        { "Value, " + unitsSelect() , 
          "Actual" , "Minimum" , "Maximum" , "Integral" };
    }

String[][] getRowsValues()
    {
    return new String[][]
        { { "Median, Read"    , "-" , "-" , "-" , "-" } ,
          { "Write"           , "-" , "-" , "-" , "-" } ,
          { "Copy"            , "-" , "-" , "-" , "-" } ,
          { "Average, Read"   , "-" , "-" , "-" , "-" } ,
          { "Write"           , "-" , "-" , "-" , "-" } ,
          { "Copy"            , "-" , "-" , "-" , "-" } };
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
    return rows.length;   
    }

@Override public String getColumnName( int column )
    { 
    return getColumnsNames()[column]; 
    }

@Override public String getValueAt( int row, int column )
    { 
    if ( row < rows.length ) 
        { 
        return " " + rows[row][column]; 
        }
    else return "";
    }

@Override public void setValueAt( Object x, int row, int column )
    {
    if ( ( row < rows.length ) && 
         ( column < getColumnsNames().length ) &&
         ( x instanceof String )  )
        {
        rows[row][column] = ( String ) x;
        }
    }

@Override public boolean isCellEditable( int row, int column )
    { 
    return false; 
    }

/*
Update table for each measured value from Report Monitor.
*/
public void measurementNotify( StateAsync[] async )
    {
    boolean show = false;
    if ( async != null )
        {
        for( int i=0; i<async.length; i++ )
            {
            if ( async[i] != null )
                {
                switch( i )
                    {
                    case READ_ID:
                        cellsHelper( async[i], 0 );
                        show = true;
                        break;
                    case WRITE_ID:
                        cellsHelper( async[i], 1 );
                        show = true;
                        break;
                    case COPY_ID:
                        cellsHelper( async[i], 2 );
                        show = true;
                        break;
                    case TOTAL_READ_ID:
                        valueHelper( async[i].current, 0, 4 );
                        valueHelper( async[i].current, 3, 4 );
                        show = true;
                        break;
                    case TOTAL_WRITE_ID:
                        valueHelper( async[i].current, 1, 4 );
                        valueHelper( async[i].current, 4, 4 );
                        show = true;
                        break;
                    case TOTAL_COPY_ID:
                        valueHelper( async[i].current, 2, 4 );
                        valueHelper( async[i].current, 5, 4 );
                        show = true;
                        break;
                    }
                }
            }
        }
    // notify changes, if detected
    if ( show )
        {
        fireTableDataChanged();
        }
    }

/*
Some of this methods not private, can be used by child classes
*/
private void cellsHelper( StateAsync entry, int row )
    {
    valueHelper( entry.median,  row,   1 );
    double[] minmax = medianHelper( entry );
    valueHelper( minmax[0],     row,   2 );
    valueHelper( minmax[1],     row,   3 );
    valueHelper( entry.average, row+3, 1 );
    valueHelper( entry.min,     row+3, 2 );
    valueHelper( entry.max,     row+3, 3 );
    }

void valueHelper( double value, int row, int column )
    {
    String s = String.format( "%.2f", value );
    setValueAt( s, row, column );
    }

double[] medianHelper( StateAsync entry )
    {
    double[] minmax = new double[]{ Double.NaN, Double.NaN };
    double[] data = entry.array;
    if ( ( data != null )&&( data.length > 0 ) )
        {
        if ( entry.medianIndexMin >= 0 )
            {
            minmax[0] = data[entry.medianIndexMin];
            }
        if ( entry.medianIndexMax >= 0 )
            {
            minmax[1] = data[entry.medianIndexMax];
            }
        }
    return minmax;
    }
}
