/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Parent class for operation handlers.
*/

package niobenchrefactoring.controller;

import javax.swing.AbstractAction;
import niobenchrefactoring.view.Application;

abstract public class Handler extends AbstractAction 
{
final Application application;

public Handler( Application application )
    {
    this.application = application;
    }

}