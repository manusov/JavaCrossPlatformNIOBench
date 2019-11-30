/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Package for support functions Y=F(X) drawings.
Function Y=F(X) drawing controller.
*/

package opendraw;

public class FunctionController implements FunctionControllerInterface
{
private final FunctionModelInterface model;
private final FunctionViewInterface view;

public FunctionController()
    {
    model = new FunctionModel( this );
    view = new FunctionView( this );
    }

@Override public FunctionModelInterface getModel()
    {
    return model;
    }

@Override public FunctionViewInterface getView()
    {
    return view;
    }

}
