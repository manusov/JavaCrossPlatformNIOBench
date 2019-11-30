/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Representation of measurement result.
*/

package opentable;

import java.math.BigDecimal;

public class NumericEntry 
{
public int num;                   // measurement iteration number
public double[] doubles;          // array[4] = parms
public BigDecimal[] bigdecs;      // same array as BigDecimal
public NumericEntry( int n, double[] d, BigDecimal[] b )
    {
    num = n;
    doubles = d;
    bigdecs = b;
    }
}
