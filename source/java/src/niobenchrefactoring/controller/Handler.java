/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Parent class for operation handlers: procedures running by buttons press.
*/

package niobenchrefactoring.controller;

import javax.swing.AbstractAction;
import niobenchrefactoring.view.Application;

abstract class Handler extends AbstractAction 
{
final Application application;

Handler( Application application )
    {
    this.application = application;
    }
}
