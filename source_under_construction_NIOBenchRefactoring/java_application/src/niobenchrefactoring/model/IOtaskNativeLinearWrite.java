package niobenchrefactoring.model;

public class IOtaskNativeLinearWrite extends IOtask
{
private final static String IOTASK_NAME =
    "Native OS API disk linear write, MBPS";

/*
Constructor stores IO scenario object
*/
IOtaskNativeLinearWrite( IOscenarioChannel ios )
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
