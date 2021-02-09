/* 
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Operation handler for "Browse" button for destination path: select path.
*/

package niobenchrefactoring.controller;

import javax.swing.JTextField;
import niobenchrefactoring.view.Application;

public class HandlerDestinationPath extends HandlerSourcePath
{
public HandlerDestinationPath( Application application, String name, 
                               JTextField field )
    {
    super( application, name, field, null );
    }
}
