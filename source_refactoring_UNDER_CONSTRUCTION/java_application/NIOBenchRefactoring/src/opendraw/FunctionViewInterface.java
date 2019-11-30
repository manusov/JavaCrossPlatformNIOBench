/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Package for support functions Y=F(X) drawings.
View interface for MVC (Model, View, Controller) pattern.
*/

package opendraw;

import javax.swing.JPanel;

public interface FunctionViewInterface 
{
public JPanel getPanel();  // get drawings view panel with own paint method
}
