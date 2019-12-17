/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Benchmark results table model for "SSD specific" scenario.
Dual purpose: table model used for build GUI and save text report.
*/

package niobenchrefactoring.model;

public class TableSSD extends TableChannel
{

/*
Update table for each measured value from Report Monitor.
*/
@Override public void measurementNotify( StateAsync[] async )
    {
    
    
    // notify changes
    fireTableDataChanged();
    }

    
}