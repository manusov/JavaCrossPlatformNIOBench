package niobenchrefactoring.model;

public class IOtaskNativeLinearCopy extends IOtask
{
private final static String IOTASK_NAME =
    "Native OS API disk linear copy, MBPS";

/*
Constructor stores IO scenario object
*/
IOtaskNativeLinearCopy( IOscenarioChannel ios )
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
