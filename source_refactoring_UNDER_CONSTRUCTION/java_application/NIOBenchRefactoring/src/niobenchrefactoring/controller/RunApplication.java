/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Application run class.
This class is part of CONTROLLER at MODEL/VIEW/CONTROLLER (MVC) functionality.
*/

package niobenchrefactoring.controller;

import javax.swing.JDialog;
import javax.swing.JFrame;
import niobenchrefactoring.resources.PAL;
import niobenchrefactoring.view.Application;

public class RunApplication 
{
public void run()
    {
    JFrame.setDefaultLookAndFeelDecorated( true );
    JDialog.setDefaultLookAndFeelDecorated( true );
    PAL pal = new PAL();
    pal.loadUserModeLibrary();
    Application a = new Application( pal );
    a.open();
    }
}
