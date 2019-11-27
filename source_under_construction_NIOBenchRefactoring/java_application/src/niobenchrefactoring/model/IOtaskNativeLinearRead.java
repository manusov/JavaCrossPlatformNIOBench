package niobenchrefactoring.model;

public class IOtaskNativeLinearRead extends IOtask
{
private final static String IOTASK_NAME =
    "Native OS API disk linear read, MBPS";

/*
Constructor stores IO scenario object
*/
IOtaskNativeLinearRead( IOscenarioChannel ios )
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
