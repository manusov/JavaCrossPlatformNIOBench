/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Application main class.
This class must run CONTROLLER at MODEL/VIEW/CONTROLLER (MVC) functionality.
*/


/*

UNDER CONSTRUCTION. This main module can be used as sequence of tests

Near roadmap.
---------------
1)+  "About" function and paths.
2)+  Connect templates for native libraries, get native name
     ( win 32/64, linux 32/64 ).
3)+  Native method for get random array
     ( win 32/64, linux 32/64 ).
4)+  Inspect existed classes.
5)+  Connect charts (drawings) child window. First verify at constant patterns.
6)+  Connect log child window. First verify at constant patterns.
7)+  Connect tables child window. First verify at constant patterns.
8)+  Design tabbed panel.
9)+  Design channels test panel.
10)+ Base functionality for buttons: 
      a) Open Log, Open Table, Open Drawings,
      b) Text report, Graphics save, 
      c) Default MBPS, Default IOPS, Clear, About, Cancel. 
11)+ Service functions: Clear results, Save report, Load report.
      a) + table with options settings for save report at run moment - to log
      b) + replace html colored string to "M" str. for report with medians
      c) + don't use tables for log, create and save text log, all to log
      d) + "Clear" key
      e) + "Report" key, design report save
      f) + "Load" key, design report load, update table and drawings as f(text).
12)+ Data randomization scenario include native RDRAND. Combo box = "Data".
      a) + check status of native library loader, send to application
      b) + messages for "Data" combo box
      c) + generation data, send to io scenario.


13) Unlock and verify "Threads" option.

---
14)  Run scenario for CHANNELS. Yet limited options support. Get report.
      a) + progress indicator
      b) + label left progress indicator, status, operation name or error name
      c) + add "starting..." state to phases nomenclature for label
      d) + add delays to phases nomenclature for label, verify delays options
      e) + add delete files phase to phases nomenclature for label
      f) + add halted state to phases nomenclature for label
      g) + add errors to phases nomenclature for label, error modeling
      h) + table at main window
      i) + update openable log
      j) + update openable table
      k) + update openable draw
      l) interruptable
      m) options restrictions for channels, instead debug locks.
---

15)  Design IOPS-oriented address randomization, must be valid for
     write/copy/read. Correct tasks names. First verify at channels.
     Include mixed read/write load.
     Separate mix option or add to Read/Write option ?

16)  Data randomization for all IO scenarios. Verify as MBPS, IOPS.
     First at channels.

17)  Options restrictions for child classes, overridable method.

------

... develop by tabs: same for all 7 scenarios ...
... inspect by buttons: service functions ...

---
Research notes.
---
 Archives benchmark.
 Native MBPS support.
 Native IOPS support.
 Other scenarios applications.
 Tools handlers: exit, save report, about.
 Inspect public, default, protected, private status for all classes.
 Tooltips and mnemonics.
 ...
 Verify all 7 scenarios by tabs.
 Verify all 12 handlers by buttons.

---
Remember notes.
---
 MULTI-THREAD CAN BE SUPPORT FOR ALL SCENARIO BY UNIFIED WAY (?),
 SUPPORT MULTI-THREAD FOR EACH READ-WRITE METHOD IS TOO COMPLEX.
 PLUS, CAN COMBINE DIFFERENT IO METHODS IF SUPPORTED BY ALL SCENARIO.
 CAN IMPLEMENT SERVER LOAD MODEL.
 TODO. 
 CHECK ISINTERRUPTED FOR ALL IOTASKS.
 NOTE.
 Classic MBPS and IOPS measurement first actual for NATIVE mode.
 Thread helper delays can corrupt performance measurement results.
 Plus ASYNC_PAUSE, inspect all waiting delays.
 Write sync and Copy sync options interpretetion incorrect.
 HandlerBrowse - select directory by one click, now two.

*/

package niobenchrefactoring;

import niobenchrefactoring.controller.RunApplication;
public class NIOBenchRefactoring 
{
public static void main(String[] args) 
    {
    RunApplication ra = new RunApplication();
    ra.run();
    }
}