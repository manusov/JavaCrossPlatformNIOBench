package niobenchrefactoring.view;

import javax.swing.JPanel;
import javax.swing.table.AbstractTableModel;

public abstract class ApplicationPanel extends JPanel
{
abstract String getTabName();
abstract AbstractTableModel getTableModel();
    
}
