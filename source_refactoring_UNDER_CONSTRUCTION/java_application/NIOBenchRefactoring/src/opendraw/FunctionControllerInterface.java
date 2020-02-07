/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Package for support functions Y=F(X) drawings.
Controller interface for MVC (Model, View, Controller) pattern.
*/

package opendraw;

import niobenchrefactoring.view.Application.APPLICATION_PANELS;

public interface FunctionControllerInterface 
{
public void setPanelType( APPLICATION_PANELS ap );  // used for legend naming
    
public FunctionModelInterface getModel(); // get connected model = function
public FunctionViewInterface getView();   // get connected view = panel
}
