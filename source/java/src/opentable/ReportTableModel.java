/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Customization for log and statistics Table Model for storage benchmark, 
openable window.
This customization required for represent median values as text string
"NUMBER + M" instead colored HTML string.
*/

package opentable;

import niobenchrefactoring.view.Application;

public class ReportTableModel extends StatisticsTableModel
{
public ReportTableModel
        ( StatisticsTableModel m, Application.APPLICATION_PANELS ap )
    {
    super( ap );
    setRowsValues( m.getRowsValues() );
    }
    
/*
Detect HTML-marked median values and replace to "NUMBER + M".
input string from table used for GUI visualization
output string from table used for text report.
*/
@Override public String getValueAt( int row, int column )
    {
    String s = super.getValueAt( row, column );
    if ( ( s != null )                         &&
         ( s.length() > MARK_STRING.length() ) &&   
         ( s.startsWith( MARK_STRING ) )       )
        {
        int begin = MARK_STRING.length();
        s = s.substring( begin );
        s = s + " M";
        }
    return s;
    }
}
