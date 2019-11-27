package niobenchrefactoring.model;

class IOtask extends Thread
{
    
final IOscenario ios;

IOtask( IOscenario ios )
    {
    this.ios = ios;
    }
}
