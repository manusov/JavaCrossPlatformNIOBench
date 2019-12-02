/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Default MBPS" button: default settings for options,
sequental access scenario optimized for bandwidth measurement (MBPS).
*/

// TODO.
// Required some handlers by scenarios: default MBPS, IOPS, NATIVE ...


package niobenchrefactoring.controller;

import java.awt.event.ActionEvent;
import javax.swing.JFrame;

public class HandlerDefaultMBPS extends Handler
{
public HandlerDefaultMBPS( JFrame parentFrame )
    {
    super( parentFrame );
    }

@Override public void actionPerformed( ActionEvent e )
    {
    
    }
    
}
