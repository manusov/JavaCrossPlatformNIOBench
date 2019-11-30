/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Class for return detail info about benchmarks measurement session.
Used for communication with table models.
*/

package opentable;

import java.util.concurrent.CopyOnWriteArrayList;

public class EntryStatistics 
{
// data
public CopyOnWriteArrayList<NumericEntry>
    dataArray = new CopyOnWriteArrayList<>();
// statistics
public double[] blockArray;       // data blocks sizes
public double[] mbpsArray;
public double[] iopsArray;
public double[] reservedArray;
public EntryDetail blockEntry;    // Entries for current point
public EntryDetail mbpsEntry;
public EntryDetail iopsEntry;
public EntryDetail reservedEntry;
public String[][] statTable;      // strings for statistics in the GUI tables
}
