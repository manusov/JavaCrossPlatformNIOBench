/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Clear" button: clear previous results.
*/

package niobenchrefactoring.controller;

import java.awt.event.ActionEvent;
import niobenchrefactoring.view.Application;
import niobenchrefactoring.view.ApplicationPanel;

public class HandlerClear extends Handler
{
public HandlerClear( Application application )
    {
    super( application );
    }

@Override public void actionPerformed( ActionEvent e )
    {
    ApplicationPanel selectedPanel = application.getSelectedPanel();
    if ( selectedPanel != null )
        selectedPanel.clearResults();
    }
}
