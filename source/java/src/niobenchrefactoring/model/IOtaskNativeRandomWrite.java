/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Write phase at Native OS API IO scenario.
Version for randomized address access.
Io tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

class IOtaskNativeRandomWrite extends IOtask
{
private final static String IOTASK_NAME = "Write/Random/Native";

/*
Constructor stores IO scenario object
*/
IOtaskNativeRandomWrite( IOscenarioNative ios )
    {
    super( ios );
    }

/*
Run IO task
*/
@Override public void run()
    {
    // UNDER CONSTRUCTION
    }
}
