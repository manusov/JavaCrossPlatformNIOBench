/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Static helper class for storage IO address randomization purposes.
Support sequence randomization by sorting array of entries:
entry = { randomOrderCriteria, linearIndex };
*/

package niobenchrefactoring.model;

import java.util.Arrays;

public class HelperRandom 
{
/*
Object for sorting by criteria = x with randomize array of integers = y    
*/
private static class Entry implements Comparable<Entry>
    {
    final double x;
    final int y;
    Entry( double x, int y )
        {
        this.x = x;
        this.y = y;
        }
    @Override public int compareTo( Entry entry )
        {
        return (int) Math.signum( this.x - entry.x );
        }
    }

/*
Return random sorted array of integers with unique elements [ o...size )
*/
public static int[] randomArray( int size )
    {
    Entry[] entries = new Entry[size];
    for( int i=0; i<size; i++ )
        entries[i] = new Entry( Math.random(), i );
    Arrays.sort( entries );
    int[] values = new int[size];
    for( int i=0; i<size; i++ )
        values[i] = entries[i].y;
    return values;
    }
}
