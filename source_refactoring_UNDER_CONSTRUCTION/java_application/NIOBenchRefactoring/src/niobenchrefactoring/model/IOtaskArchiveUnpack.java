/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Unpack phase at archives IO scenario.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

public class IOtaskArchiveUnpack extends IOtask
{
private final static String IOTASK_NAME =
    "Unpack files with zip, MBPS";

/*
Constructor stores IO scenario object
*/
IOtaskArchiveUnpack( IOscenarioChannel ios )
    {
    super( ios );
    }

/*
Run IO task
*/
@Override public void run()
    {
    }
    
}
