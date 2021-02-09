/*

NIOBench, file I/O Benchmark utility. (C)2018 IC Book Labs
Window "Browse", select location for file operations benchmark.

*/

package niobench;

import javax.swing.*;

public class ActionBrowse {
    public String selectFile(String s1) 
    {
    // Setup component for file selection
    JFileChooser chooser = new JFileChooser();    
    chooser.setDialogTitle("Select target drive and directory for benchmarks");
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.setAcceptAllFileFilterUsed(false);
    // Show dialogue window
    int res = chooser.showOpenDialog(null);
    // Interpreting file choosing results
    if(res==JFileChooser.APPROVE_OPTION)
        { 
        String s2 = "" + chooser.getSelectedFile();
        s1 = s2;
        int i=s1.length()-1;
        char a = s1.toCharArray()[i];
        int b = PAL.getNativeType();
        if (((b==0)|(b==1))&(a!='\\')) { s1 = s1 + "\\"; }
        if (((b==2)|(b==3))&(a!='/'))  { s1 = s1 + "/";  }
        }
    return s1; 
    }
}
