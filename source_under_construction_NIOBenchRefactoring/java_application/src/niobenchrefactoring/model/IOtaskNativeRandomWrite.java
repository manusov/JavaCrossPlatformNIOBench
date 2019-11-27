package niobenchrefactoring.model;

public class IOtaskNativeRandomWrite extends IOtask
{
private final static String IOTASK_NAME =
    "Native OS API disk random write, IOPS";

/*
Constructor stores IO scenario object
*/
IOtaskNativeRandomWrite( IOscenarioChannel ios )
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
