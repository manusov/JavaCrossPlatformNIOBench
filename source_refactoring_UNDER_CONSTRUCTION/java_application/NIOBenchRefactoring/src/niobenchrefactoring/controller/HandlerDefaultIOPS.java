/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Default IOPS" button: default settings for options,
random access scenario optimized for performance measurement (IOPS).
*/

package niobenchrefactoring.controller;

import java.awt.event.ActionEvent;
import niobenchrefactoring.view.Application;
import niobenchrefactoring.view.ApplicationPanel;
import niobenchrefactoring.view.ApplicationPanel.SCENARIO;

public class HandlerDefaultIOPS extends Handler
{
public HandlerDefaultIOPS( Application application )
    {
    super( application );
    }

@Override public void actionPerformed( ActionEvent e )
    {
    ApplicationPanel selectedPanel = application.getSelectedPanel();
    if ( selectedPanel != null )
        selectedPanel.setDefaults( SCENARIO.IOPS );
    }
}
