package niobenchrefactoring.model;

public class HelperDelay 
{

/*
functionally, this method can be not public, but it public for testability    
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
