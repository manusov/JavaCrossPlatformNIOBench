/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Application main class.
This class must run CONTROLLER at MODEL/VIEW/CONTROLLER (MVC) functionality.

UNDER CONSTRUCTION. 
This main module also can be used as sequence of tests
*/

package niobenchrefactoring;

import niobenchrefactoring.controller.RunApplication;

public class NIOBenchRefactoring 
{
public static void main(String[] args) 
    {
/*
    // TODO. Use total size calculation:
    // TotalSize =  fileSize * fileCount * measurementResults
    // for all native scenarios, implement for windows and linux.
    // Linux native libraries yet not calculate TotalSize.
    DebugNativeIO d = new DebugNativeIO();
    d.testWrite();
    d.testCopy();
    d.testRead();
    d.testDelete();
*/

//        
    RunApplication ra = new RunApplication();
    ra.run();
//        
    }
}
