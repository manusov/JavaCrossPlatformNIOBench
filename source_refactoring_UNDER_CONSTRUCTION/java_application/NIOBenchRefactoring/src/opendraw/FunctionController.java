/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Package for support functions Y=F(X) drawings.
Function Y=F(X) drawing controller.
*/

package opendraw;

import niobenchrefactoring.view.Application.APPLICATION_PANELS;

public class FunctionController implements FunctionControllerInterface
{
private final FunctionModelInterface model;
private final FunctionViewInterface view;

public FunctionController( APPLICATION_PANELS ap )
    {
    model = new FunctionModel( this, ap );
    view = new FunctionView( this );
    }

@Override public void setPanelType( APPLICATION_PANELS ap )
    {
    model.setPanelType( ap );
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
