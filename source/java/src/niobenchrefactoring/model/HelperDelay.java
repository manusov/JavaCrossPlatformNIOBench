/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Static helper class for time delays.
*/

package niobenchrefactoring.model;

public class HelperDelay 
{
/*
Delay, input value in milliseconds
return status string
*/
public static StatusEntry delay( int milliseconds )
    {
    boolean b = true;
    String s = "OK";
    if ( milliseconds > 0 )
        {
        try
            {
            Thread.sleep( milliseconds );
            }
        catch ( InterruptedException e )
            {
            b = false;
            s = "Wait exception: " + e.getMessage();
            }
        }
    else if ( milliseconds < 0 )
        {
        b = false;
        s = "Wrong wait time value, must be positive or zero";
        }
    return new StatusEntry( b, s );
    }
}
