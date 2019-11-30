/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Package for support functions Y=F(X) drawings.
Controller interface for MVC (Model, View, Controller) pattern.
*/

package opendraw;

public interface FunctionControllerInterface 
{
public FunctionModelInterface getModel(); // get connected model = function
public FunctionViewInterface getView();   // get connected view = panel
}
