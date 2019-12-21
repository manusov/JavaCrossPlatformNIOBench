/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Benchmark results table model for "NIO asynchronous channels" scenario.
Dual purpose: table model used for build GUI and save text report.
*/

package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenario.READ_ID;
import static niobenchrefactoring.model.IOscenario.WRITE_ID;

public class TableAsyncChannel extends TableChannel
{
@Override public String[][] getRowsValues()
    {
    return new String[][]
        { { "Median, Read"    , "-" , "-" , "-" } ,
          { "Write"           , "-" , "-" , "-" } ,
          { "Average, Read"   , "-" , "-" , "-" } ,
          { "Write"           , "-" , "-" , "-" } };
    }

/*
Update table for each measured value from Report Monitor.
*/
@Override public void measurementNotify( StateAsync[] async )
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

private void cellsHelper( StateAsync entry, int row )
    {
    valueHelper( entry.median,  row,   1 );
    double[] minmax = medianHelper( entry );
    valueHelper( minmax[0],     row,   2 );
    valueHelper( minmax[1],     row,   3 );
    valueHelper( entry.average, row+2, 1 );
    valueHelper( entry.min,     row+2, 2 );
    valueHelper( entry.max,     row+2, 3 );
    }


}
