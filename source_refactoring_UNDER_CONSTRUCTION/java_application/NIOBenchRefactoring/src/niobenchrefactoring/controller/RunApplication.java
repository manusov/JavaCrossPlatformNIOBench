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
    /*
    Initializing native layer
    variable loadStatus: >=0 means load OK, <0 means error
    variable palWidth: native width: 32 or 64, 33 means JRE32 under Win64,
    and errors encoding: -1 = not loaded, -2 = exception, -3 = unsatisfied link
    */
    PAL pal = new PAL();
    int loadStatus = pal.loadUserModeLibrary();
    int palWidth = -1;
    if ( loadStatus==0 )
        {
        try 
            {
            palWidth = pal.checkBinary(); 
            }
        catch ( Exception e )
            {
            palWidth = -2; 
            }
        catch ( UnsatisfiedLinkError e ) 
            { 
            palWidth = -3; 
            }
        }
    /*
    Set GUI style options    
    */
    JFrame.setDefaultLookAndFeelDecorated( true );
    JDialog.setDefaultLookAndFeelDecorated( true );
    /*
    Run NIOBench application
    */
    Application a = new Application( pal, palWidth );
    a.open();
    }
}
