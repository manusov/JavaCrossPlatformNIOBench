/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO task for files Copy phase at Native OS API IO scenario.
Version for linear access.
IO tasks is basic components for build IO scenarios.
*/

package niobenchrefactoring.model;

public class IOtaskNativeLinearCopy extends IOtask
{
private final static String IOTASK_NAME =
    "Native OS API disk linear copy, MBPS";

/*
Constructor stores IO scenario object
*/
IOtaskNativeLinearCopy( IOscenarioChannel ios )
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
