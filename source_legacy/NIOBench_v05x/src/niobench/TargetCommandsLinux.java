/*

NIOBench, file I/O Benchmark utility. (C)2018 IC Book Labs
Benchmarks target operation, child class for unbuffered native mode, Linux.
This NATIVE mode not use NIO methods.

*/

package niobench;

import static niobench.NIOBench.API_WRITE_READ_COPY_DELETE;

public class TargetCommandsLinux extends TargetCommands  
{
private final static int  API_BLOCK = 1048576;     // Associated with caller
private final static int  BLOCK_MUL = 10;
private final static int  API_BLOCK_TAIL = API_BLOCK * ( BLOCK_MUL + 1 );

private final static long API_MODE = 0x00004042L;  // Unbuffered, Uncached mode
private final static long API_REPEATS = 2;         // Internal measurement
private final static int  API_NS = 1; // time unit for Linux = 1 ns

// Thread execution method
@Override public void run()
    {
    // Status initialization
    errorCode = 0;
    long outW, outR, outC, outD;
    
    // Files names initialization
    String srcString = filePath + SRC_NAME + "0" + EXTENSION;
    String dstString = filePath + DST_NAME + "0" + EXTENSION;
    
    // Size of Input Parameters Block, qwords
    int nI = API_BLOCK_TAIL / 8;
    
    // Size of Output Parameters Block, qwords
    int nO = API_BLOCK_TAIL / 8;
    
    // Initialize Input Parameter Block (IPB) array, 
    // data can be from Random Number Generator (RNG)
    long[] mIpb = new long[nI];
        for ( int i=0; i<nI; i++ )
        {
        mIpb[i]=0; 
        }
    int j = 0;
    while ( j < API_BLOCK_TAIL )
        {
        IOPB.transmitBytes( data , mIpb, j/8 , API_BLOCK/8 );
        j += API_BLOCK;
        }
    
    // Initialize Output Parameter Block (OPB) array
    long[] mOpb = new long[nO];
    for ( int i=0; i<nO; i++ )
        {
        mOpb[i]=0; 
        }
    
    // Adaptive block size detection, if >= 10MB use 10MB blocks, otherwise 1MB
    int blockSize = API_BLOCK;
    int blockCount = fileSize;
    if ( fileSize / BLOCK_MUL >= 10 )
        {
        blockSize *= BLOCK_MUL;
        blockCount /= BLOCK_MUL;    
        }

    // Initialize Platform Abstraction Layer
    PAL mPal = new PAL();
    
    // Benchmarks cycle
    for ( int i=0; i<fileCount; i++ )
        {
        if (interrupt) break;
        // Set input parameters before native method call
        mIpb[0] = API_WRITE_READ_COPY_DELETE;
        mIpb[1] = blockSize;
        mIpb[2] = blockCount;
        mIpb[3] = API_MODE;
        mIpb[4] = API_MODE;
        mIpb[5] = API_REPEATS;
        IOPB.transmitString( srcString , mIpb ,   6 , 122 );  // src. file name
        IOPB.transmitString( dstString , mIpb , 128 , 122 );  // dst. file name
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
