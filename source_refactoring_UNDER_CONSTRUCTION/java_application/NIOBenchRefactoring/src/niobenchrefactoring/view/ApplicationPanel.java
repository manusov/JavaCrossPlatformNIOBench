package niobenchrefactoring.view;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.table.AbstractTableModel;
import niobenchrefactoring.resources.PAL;

public abstract class ApplicationPanel extends JPanel
{
final JFrame parentFrame;
PAL pal;
public ApplicationPanel( JFrame parentFrame )
    {
    this.parentFrame = parentFrame;
    }
abstract String getTabName();
abstract AbstractTableModel getTableModel();
abstract void build();
}
