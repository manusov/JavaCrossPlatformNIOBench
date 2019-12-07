/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Statistics library root class.
Model contain data about performance process and library methods.
*/

package niobenchrefactoring.model;

import java.util.ArrayList;
import java.util.Arrays;

public class StatisticsModel 
{
private final ArrayList<Double>[] arrays;
    
StatisticsModel( int arraysCount )
    {
    arrays = new ArrayList[arraysCount];
    for( int i=0; i<arraysCount; i++ )
        {
        arrays[i] = new ArrayList<>();
        }
    nanoseconds = new long[arraysCount];
    }

/*
Send current results from performance process, as double
functionally, this method can be not public, but public for testability
*/
public boolean send( int selector, double value )
    {
    boolean b = false;
    synchronized( arrays )
        {
        if ( ( arrays != null )&&( selector < arrays.length )&&
             ( arrays[selector] != null ) )
            {
            b = arrays[selector].add( value );
            }
        }
    return b;
    }

/*
Timer values at start of measured interval, one value per operation ID
*/
private final long[] nanoseconds;

/*
Send timer value for start of interval
*/
void startInterval( int selector, long nanoseconds )
    {
    if ( selector < this.nanoseconds.length )
        this.nanoseconds[selector] = nanoseconds;
    }

/*
Send current results from performance process, 
as arguments for calculate megabytes per second
*/
boolean sendMBPS( int selector, long bytes, long nanoseconds )
    {
    if ( selector < this.nanoseconds.length )
        {
        double seconds = ( nanoseconds - this.nanoseconds[selector] ) / 1E9;
        double megabytes = bytes / 1E6;
        double mbps = megabytes / seconds;
        return send( selector, mbps );
        }
    else
        {
        return false;
        }
    }

/*
Send current results from performance process, 
as arguments for calculate IO transactions per second
*/
boolean sendIOPS( int selector, long transactions, long nanoseconds )
    {
    if ( selector < this.nanoseconds.length )
        {
        double seconds = ( nanoseconds - this.nanoseconds[selector] ) / 1E9;
        double iops = transactions / seconds;
        return send( selector, iops );
        }
    else
        {
        return false;
        }
    }

/*
This class used for internal ordering results of performance process.
*/
private class ValueIndexed implements Comparable<ValueIndexed>
    {
    final double value;
    final int index;
    ValueIndexed( double value, int index )
        {
        this.value = value;
        this.index = index;
        }
    @Override public int compareTo( ValueIndexed x )
        {
        return (int) Math.signum( this.value - x.value );
        }
    }

/*
Receive array and median results with indexes.
*/
public StateAsync receive( int selector )
    {
    double[] valuesOriginal = null;
    ValueIndexed[] valuesSorted = null;
    
    synchronized( arrays )
        {
        if ( ( arrays != null )&&( selector < arrays.length )&&
             ( arrays[selector] != null ) )
            {
            ArrayList<Double> a = arrays[selector];
            int n = a.size();
            valuesOriginal = new double[n];
            valuesSorted = new ValueIndexed[n];
            for( int i=0; i<n; i++ )
                {
                valuesOriginal[i] = a.get( i );
                valuesSorted[i] = new ValueIndexed( a.get( i ), i );
                }
            }
        }
    
    StateAsync entry = null;
    int n;
    if ( ( valuesSorted != null )&&( ( n = valuesSorted.length ) > 0 ) )
        {
        Arrays.sort( valuesSorted );
        int indexMin = -1;
        int indexCenter = -1;
        int indexMax = -1;
        double average = 0.0;
        for( int i=0; i<n; i++ )
            {
            average += valuesOriginal[i];
            }
        average /= n;
        double median;
        if ( n == 1 )
            {
            indexCenter = 0;
            median = valuesSorted[indexCenter].value;
            }
        else if ( n%2 == 0 )
            {
            indexMin = n / 2 - 1;
            indexMax = n / 2;
            median = ( valuesSorted[indexMin].value + 
                       valuesSorted[indexMax].value ) / 2.0;
            }
        else
            {
            indexMin    = n / 2 - 1;
            indexCenter = n / 2;
            indexMax    = n / 2 + 1;
            median = valuesSorted[indexCenter].value;
            }

        if ( indexMin >= 0 )
            indexMin = valuesSorted[indexMin].index;
        if ( indexCenter >= 0 )
            indexCenter = valuesSorted[indexCenter].index;
        if ( indexMax >= 0 )
            indexMax = valuesSorted[indexMax].index;

        entry = new StateAsync
            ( valuesOriginal, valuesOriginal[n-1],
              valuesSorted[0].value, valuesSorted[n-1].value, average, median,
              indexMin, indexCenter, indexMax );
        }
    return entry;
    }
}
