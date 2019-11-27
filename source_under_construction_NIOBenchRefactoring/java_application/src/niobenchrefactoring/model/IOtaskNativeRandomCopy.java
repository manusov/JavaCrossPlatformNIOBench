package niobenchrefactoring.model;

public class IOtaskNativeRandomCopy extends IOtask
{
private final static String IOTASK_NAME =
    "Native OS API disk random copy, IOPS";

/*
Constructor stores IO scenario object
*/
IOtaskNativeRandomCopy( IOscenarioChannel ios )
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
