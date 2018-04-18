/*

NIOBench, file I/O Benchmark utility. (C)2018 IC Book Labs
Benchmarks target operation, child class for unbuffered native mode, Windows.
This NATIVE mode not use NIO methods.
BUG: Required make alignment optimization, same as for Linux.

*/

package niobench;

import static niobench.NIOBench.API_WRITE_READ_COPY_DELETE;

public class TargetCommandsWindows extends TargetCommands 
{
private final static int  API_BLOCK = 1048576;     // Associated with caller
private final static long API_MODE = 0xA0000080L;  // Unbuffered, Uncached mode
private final static long API_REPEATS = 2;         // Internal measurement
private final static int  API_NS = 100;            // Nanoseconds per Unit
 
// Thread execution method
@Override public void run()
    {
    errorCode = 0;        
    String srcString = filePath + SRC_NAME + "0" + EXTENSION;
    String dstString = filePath + DST_NAME + "0" + EXTENSION;
    long outW, outR, outC, outD;
    // Size of Input Parameters Block, qwords
    int nI = API_BLOCK/8 + 2048/8;
    // Size of Output Parameters Block, qwords
    int nO = API_BLOCK/8 + 2048/8;
    // Initialize arrays and Platform Abstraction Layer
    long[] mIpb = new long[nI];  for ( int i=0; i<nI; i++ ) { mIpb[i]=0; }
    long[] mOpb = new long[nO];  for ( int i=0; i<nO; i++ ) { mOpb[i]=0; }
    PAL mPal = new PAL();
    // Benchmarks cycle
    for ( int i=0; i<fileCount; i++ )
        {
        if (interrupt) break;
        // Set input parameters before native method call
        mIpb[0] = API_WRITE_READ_COPY_DELETE;
        mIpb[1] = API_BLOCK;
        mIpb[2] = fileSize;
        mIpb[3] = API_MODE;
        mIpb[4] = API_MODE;
        mIpb[5] = API_REPEATS;
        IOPB.transmitString( srcString , mIpb ,   6 , 122 );  // src. file name
        IOPB.transmitString( dstString , mIpb , 128 , 122 );  // dst. file name
        IOPB.transmitBytes( data , mIpb, 256 , API_BLOCK/8 ); // data can random
        // Native method call
        mPal.entryPAL( mIpb, mOpb, nI, nO );
        // Get output parameters after native method call
        outW = mOpb[0];
        outR = mOpb[1];
        outC = mOpb[2];
        outD = mOpb[3];
        if ((outW==0)|(outR==0)|(outC==0)|(outD==0))
            {
            errorCode=1;
            break;
            }
        // store results per iteration
        nanosecondsWrite[i] = outW * API_NS / API_REPEATS;
        nanosecondsRead[i]  = outR * API_NS / API_REPEATS;
        nanosecondsCopy[i]  = outC * API_NS / API_REPEATS;
        counter+=3;
        }

    // Signal end thread execution
    flag = false;
    }
}    