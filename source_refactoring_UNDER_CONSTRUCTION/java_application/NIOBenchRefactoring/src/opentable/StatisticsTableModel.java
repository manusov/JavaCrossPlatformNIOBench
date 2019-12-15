/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Log and statistics Table Model for storage benchmark, openable window.
*/

package opentable;

import java.util.Arrays;
import javax.swing.table.AbstractTableModel;
import static niobenchrefactoring.model.IOscenario.COPY_ID;
import static niobenchrefactoring.model.IOscenario.READ_ID;
import static niobenchrefactoring.model.IOscenario.WRITE_ID;
import niobenchrefactoring.model.StateAsync;
import niobenchrefactoring.model.StateSync;

public class StatisticsTableModel extends AbstractTableModel 
{
final static String MARK_STRING = "<html><b><font color=blue>";
    
public StatisticsTableModel()
    {
    blank( 0 );
    }

// clear table for next measurement session, n = measurement entries count
final void blank( int n ) 
    {
    maxIndex = n;
    rowsValues = new String[ n + 5 ][ getColumnCount() ];
    int i;
    for( i=0; i<n; i++ )
        {
        String[] s = Arrays.copyOf( initValues[0], initValues[0].length );
        s[0] = "" + ( i + 1 );
        rowsValues[i] = s;
        }
    if ( n == 0 )
        rowsValues[i][0] = "#";
    else
        rowsValues[i][0] = "";
    for( int j=1; j<rowsValues[i].length; j++ )
        {
        rowsValues[i][j] = "";
        }
    i++;  // skip 1 string
    
    for( int j=1; j<5; j++ )
        {
        rowsValues[i] = Arrays.copyOf ( initValues[j], initValues[j].length );
        i++;
        }
    fireTableStructureChanged();
    }

// table model class fields
private final String[] COLUMNS_NAMES = { "Iteration", "Read", "Write", "Copy" };
private String[][] rowsValues = null;
private final String[][] initValues =
    { { "#"        , "-" , "-" , "-" } ,
      { "Median"   , "-" , "-" , "-" } ,
      { "Average"  , "-" , "-" , "-" } ,
      { "Minimum"  , "-" , "-" , "-" } ,
      { "Maximum"  , "-" , "-" , "-" } , };

// table model this application-specific public methods
final String[] getColumnsNames()          { return COLUMNS_NAMES; }
final String[][] getRowsValues()          { return rowsValues;    }
final void setRowsValues( String[][] s )  { rowsValues = s;       }
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


private int maxIndex;

/*
Update table for each measured value
*/
public void notifySync( StateSync sync )
    {
    int index = sync.count - 1;
    if ( ( index >= 0 ) && ( index < maxIndex ) )
        {
        switch ( sync.phaseID )
            {
            case READ_ID:
                valueHelper( sync, index, 1 );
                break;
            case WRITE_ID:
                valueHelper( sync, index, 2 );
                break;
            case COPY_ID:
                valueHelper( sync, index, 3 );
                break;
            }
        }
    // notify changes
    fireTableDataChanged();
    }

private void valueHelper( StateSync sync , int row, int column )
    {
    int n = rowsValues.length;
    rowsValues[row][column] = String.format( "%.2f", sync.current );
    rowsValues[n-4][column] = String.format( "%.2f", sync.median );
    rowsValues[n-3][column] = String.format( "%.2f", sync.average );
    rowsValues[n-2][column] = String.format( "%.2f", sync.min );
    rowsValues[n-1][column] = String.format( "%.2f", sync.max );
    }

/*
Update table for medians elements marking
*/
public void notifyMedians( StateAsync[] async )
    {
    if ( async != null )
        {
        for( int i=0; i<async.length; i++ )
            {
            if( async[i] != null )
                {
                switch ( i )
                    {
                    case READ_ID:
                        medianHelper( async[i], 1 );
                        break;
                    case WRITE_ID:
                        medianHelper( async[i], 2 );
                        break;
                    case COPY_ID:
                        medianHelper( async[i], 3 );
                        break;
                    }
                }
            }
        // notify changes
        fireTableDataChanged();
        }
    }

private void medianHelper( StateAsync async, int column )
    {
    markHelper( async.medianIndexCenter, column );
    markHelper( async.medianIndexMin,    column );
    markHelper( async.medianIndexMax,    column );
    }

private void markHelper( int row, int column )
    {
    if ( row >= 0 )
        {
        String s = getValueAt( row, column );
        s = MARK_STRING + s ;
        setValueAt( s, row, column );
        }
    }

/*
This method used for mark medians after report load by "Load" button
*/
public void markMedian( int row, int column )
    {
    String s = getValueAt( row, column );
    s = MARK_STRING + s;
    setValueAt( s, row, column );
    }

}
