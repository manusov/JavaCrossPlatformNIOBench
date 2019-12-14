/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Clear" button: clear previous results.
*/

package niobenchrefactoring.controller;

import java.awt.event.ActionEvent;
import niobenchrefactoring.model.TableChannel;
import niobenchrefactoring.view.Application;
import niobenchrefactoring.view.ApplicationPanel;
import opendraw.FunctionController;
import opendraw.FunctionModelInterface;
import opendraw.OpenDraw;
import openlog.OpenLog;
import opentable.OpenTable;

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
        {
        // unified clear operation: clear summary table at main window
        TableChannel tc = selectedPanel.getTableModel();
        if ( tc != null )
            {
            tc.clear();
            }
        // unified clear operation: clear openable text log window
        OpenLog log = application.getChildLog();
        if ( log != null )
            {
            String s = log.getText();
            if ( ! ( s.trim() ).equals( "" ) )
                {
                log.overWrite( "Log is cleared by user.\r\n" );
                log.repaint();
                log.revalidate();
                }
            }
        // unified clear operation: clear openable statistics table window
        OpenTable table = application.getChildTable();
        if ( table != null )
            {
            table.blankTable();
            table.repaint();
            table.revalidate();
            }
        // unified clear operation: clear openable performance drawing window
        OpenDraw draw = application.getChildDraw();
        if ( draw != null )
            {
            FunctionController fc = draw.getController();
            if ( fc != null )
                {
                FunctionModelInterface fim = fc.getModel();
                if ( fim != null )
                    {
                    fim.blankModel();
                    draw.repaint();
                    draw.revalidate();
                    }
                }
            }
        // panel-specific clear operations
        selectedPanel.clearResults();
        }
    }
}
