/*
Status entry for error reporting, 
include status ( true=PASS, false=FAIL ) and error description string.
*/

package niobenchrefactoring.model;

public class StatusEntry 
{
public final boolean flag;
public final String string;
StatusEntry( boolean flag, String string )
    {
    this.flag = flag;
    this.string = string;
    }
}
