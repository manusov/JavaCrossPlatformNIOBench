/*

NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Application main class.
This class must run CONTROLLER at MODEL/VIEW/CONTROLLER (MVC) functionality.
This main module also can be used for call sequence of tests
at application debug phase.

*/

package niobenchrefactoring;

import niobenchrefactoring.controller.RunApplication;

public class NIOBenchRefactoring 
{
public static void main( String[] args ) 
    {
    // Debug (1) MBPS: separate methods for Write, Copy, Read, Delete.
    // DebugNative d = new DebugNative();
    // d.testWrite();
    // d.testCopy();
    // d.testRead();
    // d.testDelete();
  
    // Debug (2) MBPS: single method for Write, Copy, Read, Delete. 
    // DebugNativeMBPS d = new DebugNativeMBPS();
    // d.testLinear();
        
    // Debug (3) IOPS.
    // Under Construction.

    // Run CONTROLLER at MODEL/VIEW/CONTROLLER (MVC) functionality,
    // Initialize native layer, set GUI style options, run application.
    RunApplication ra = new RunApplication();
    ra.run();
    }
}
