package niobenchrefactoring.model;

import static niobenchrefactoring.model.IOscenario.TOTAL_WRITE_ID;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import java.util.concurrent.CountDownLatch;

public class IOtaskAsyncChannelWrite extends IOtask
{
private final static String IOTASK_NAME =
    "NIO asynchronous channel MBPS, Write";

IOtaskAsyncChannelWrite( IOscenarioAsyncChannel ios )
    {
    super( ios );
    }

/*
Run IO task
*/
@Override public void run()
    {
    IOscenarioAsyncChannel iosac = (IOscenarioAsyncChannel)ios;
    CountDownLatch latch = new CountDownLatch( iosac.fileCount );
    Object attachment = null;
    try
        {
        iosac.totalBuffer.rewind();
        ByteBuffer[] duplicates = new ByteBuffer[iosac.fileCount];
        for( int i=0; i<iosac.fileCount; i++ )
            {
            iosac.channelsSrc[i] = AsynchronousFileChannel.open
                ( iosac.pathsSrc[i], CREATE , WRITE );
            duplicates[i] = iosac.totalBuffer.duplicate();
            }
            iosac.statistics.startInterval
                ( TOTAL_WRITE_ID, System.nanoTime() );
            for( int i=0; i<iosac.fileCount; i++ )
                {
                iosac.channelsSrc[i].write( duplicates[i], 0, attachment,
                                      new CompletionHandler<Integer,Object>()
                    {
                    @Override
                    public void completed( Integer result, Object attachment )
                        {
                        latch.countDown();
                        }
                    @Override
                    public void failed( Throwable e, Object attachment )
                        {
                        iosac.lastError = new StatusEntry( false, 
                                "Failed write completion" + e.getMessage() );
                        }
                    });
                }
            try
                {
                latch.await();
                }
            catch ( InterruptedException e )
                {
                iosac.lastError = new StatusEntry( false, 
                        "countdown write wait interrupted" + e.getMessage() );
                }
            iosac.statistics.sendMBPS
                ( TOTAL_WRITE_ID, iosac.totalSize, System.nanoTime() );
        }
    catch( IOException e )
        {
        iosac.delete( iosac.pathsSrc, iosac.channelsSrc );
        iosac.lastError = 
            new StatusEntry( false, "Write error: " + e.getMessage() );
        }
    }
}
