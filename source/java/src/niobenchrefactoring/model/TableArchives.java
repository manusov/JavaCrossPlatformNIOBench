/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Benchmark results table model for "Archives" scenario.
Dual purpose: table model used for build GUI and save text report.
*/

package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenario.*;

public class TableArchives extends TableChannel
{
@Override public String[][] getRowsValues()
    {
    return new String[][]
        { { "Median, Write"   , "-" , "-" , "-" , "-" } ,
          { "Pack"            , "-" , "-" , "-" , "-" } ,
          { "Unpack"          , "-" , "-" , "-" , "-" } ,
          { "Average, Write"  , "-" , "-" , "-" , "-" } ,
          { "Pack"            , "-" , "-" , "-" , "-" } ,
          { "Unpack"          , "-" , "-" , "-" , "-" } };
    }

/*
Update table for each measured value from Report Monitor.
*/
@Override public void measurementNotify( StateAsync[] async )
    {
    if ( ( async.length >= WRITE_ID )&&( async.length >= READ_ID ) )
        {
        StateAsync temp = async[WRITE_ID];
        async[WRITE_ID] = async[READ_ID];
        async[READ_ID] = temp;
        }

    if ( ( async.length >= TOTAL_WRITE_ID )&&( async.length >= TOTAL_READ_ID ) )
        {
        StateAsync temp = async[TOTAL_WRITE_ID];
        async[TOTAL_WRITE_ID] = async[TOTAL_READ_ID];
        async[TOTAL_READ_ID] = temp;
        }

    super.measurementNotify( async );
    }
}
