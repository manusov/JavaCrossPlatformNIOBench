package niobenchrefactoring.model;

public class IOtaskArchivePack extends IOtask
{
private final static String IOTASK_NAME =
    "Pack files with zip, MBPS";

/*
Constructor stores IO scenario object
*/
IOtaskArchivePack( IOscenarioChannel ios )
    {
    super( ios );
    }

/*
Run IO task
*/
@Override public void run()
    {
    }
    
}
