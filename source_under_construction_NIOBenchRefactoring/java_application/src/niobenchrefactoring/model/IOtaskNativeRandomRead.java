package niobenchrefactoring.model;

public class IOtaskNativeRandomRead extends IOtask
{
private final static String IOTASK_NAME =
    "Native OS API disk random read, IOPS";

/*
Constructor stores IO scenario object
*/
IOtaskNativeRandomRead( IOscenarioChannel ios )
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
