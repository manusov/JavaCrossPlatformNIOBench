package niobenchrefactoring.model;

public class IOtaskArchiveUnpack extends IOtask
{
private final static String IOTASK_NAME =
    "Unpack files with zip, MBPS";

/*
Constructor stores IO scenario object
*/
IOtaskArchiveUnpack( IOscenarioChannel ios )
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
