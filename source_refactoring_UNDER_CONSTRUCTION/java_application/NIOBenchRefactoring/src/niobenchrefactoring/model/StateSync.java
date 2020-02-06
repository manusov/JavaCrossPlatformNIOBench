/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
This class used for receive single operation ( Read, Write or Copy ) result.
*/

package niobenchrefactoring.model;

public class StateSync 
{
public final int count;
public final StatusEntry statusEntry;
public final int phaseID;
public final String phaseName;
public final double current, min, max, average, median;

StateSync
    ( int count, StatusEntry statusEntry, int phaseID, String phaseName, 
      double current, double min, double max, double average, double median )
    {
    this.count = count;
    this.statusEntry = statusEntry;
    this.phaseID = phaseID;
    this.current = current;
    this.min = min;
    this.max = max;
    this.average = average;
    this.median = median;
    this.phaseName = phaseName;
    }

StateSync( int count, StatusEntry statusEntry, int phaseID, String phaseName )
    {
    this.count = count;
    this.statusEntry = statusEntry;
    this.phaseID = phaseID;
    this.current = Double.NaN;
    this.min = Double.NaN;
    this.max = Double.NaN;
    this.average = Double.NaN;
    this.median = Double.NaN;
    this.phaseName = phaseName;
    }
}
