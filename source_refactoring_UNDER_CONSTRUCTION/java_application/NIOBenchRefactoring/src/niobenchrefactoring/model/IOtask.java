/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Parent class for all IO tasks.
Note IO task is lowest hierarchy level, IO scenario is highest level, 
IO scenario runs a group of IO tasks.
*/

package niobenchrefactoring.model;

class IOtask extends Thread
{
    
final IOscenario ios;
boolean interrupt = false;

IOtask( IOscenario ios )
    {
    this.ios = ios;
    }

@Override public void interrupt()
    {
    super.interrupt();
    interrupt = true;
    }

}
