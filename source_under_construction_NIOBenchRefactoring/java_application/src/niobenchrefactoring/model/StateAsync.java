/*
This class used for receive process summary data from statistics model.
*/

package niobenchrefactoring.model;

public class StateAsync 
{
public final double[] array;
public final double current, min, max, average, median;
public final int medianIndexMin, medianIndexCenter, medianIndexMax;

StateAsync
    ( double[] array, double current,
      double min, double max, double average, double median,
      int medianIndexMin, int medianIndexCenter, int medianIndexMax )
    {
    this.array = array;
    this.current = current;
    this.min = min;
    this.max = max;
    this.average = average;
    this.median = median;
    this.medianIndexMin = medianIndexMin;
    this.medianIndexCenter = medianIndexCenter;
    this.medianIndexMax = medianIndexMax;
    }
}
