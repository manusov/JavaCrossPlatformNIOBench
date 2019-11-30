/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Benchmark results table model for "NIO asynchronous channels" scenario.
Dual purpose: table model used for build GUI and save text report.
*/

package niobenchrefactoring.model;

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
}
