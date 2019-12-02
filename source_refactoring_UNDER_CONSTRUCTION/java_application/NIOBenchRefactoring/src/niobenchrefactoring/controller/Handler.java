/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Parent class for operation handlers.
*/

package niobenchrefactoring.controller;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

abstract public class Handler extends AbstractAction 
{
final JFrame parentFrame;

public Handler( JFrame parentFrame )
    {
    this.parentFrame = parentFrame;
    }

}
