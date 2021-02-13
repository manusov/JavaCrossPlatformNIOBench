/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
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
