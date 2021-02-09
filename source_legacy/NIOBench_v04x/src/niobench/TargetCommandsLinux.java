//---------- NIOBench, file I/O Benchmark utility. (C)2018 IC Book Labs --------
// Benchmarks target operation, child class for unbuffered native mode, Linux.
// This NATIVE mode not use NIO methods.

package niobench;

import static niobench.NIOBench.API_WRITE_READ_COPY_DELETE;

public class TargetCommandsLinux extends TargetCommands  
{
private final static int  API_BLOCK = 1048576;     // Associated with caller
private final static int  API_BLOCK_ALIGNED = 1048576+4096;
private final static long API_MODE = 0x00004042L;  // Unbuffered, Uncached mode
private final static long API_REPEATS = 2;         // Internal measurement
private final static int  API_NS = 1; // 100; for Linux = 1 ns

@Override public void run()
    {
    errorCode = 0;        
    String srcString = filePath + srcName + "0" + extension;
    String dstString = filePath + dstName + "0" + extension;
    long outW, outR, outC, outD;

    int nI = API_BLOCK_ALIGNED/8 + 2048/8;   // Size of Input Parameters Block, qwords
    int nO = API_BLOCK_ALIGNED/8 + 2048/8;   // Size of Output Parameters Block, qwords
    long[] mIpb = new long[nI];  for ( int i=0; i<nI; i++ ) { mIpb[i]=0; }
    long[] mOpb = new long[nO];  for ( int i=0; i<nO; i++ ) { mOpb[i]=0; }
    PAL mPal = new PAL();

    for ( int i=0; i<fileCount; i++ )
        {
        if (interrupt) break;
        
        mIpb[0] = API_WRITE_READ_COPY_DELETE;
        mIpb[1] = API_BLOCK;
        mIpb[2] = fileSize;
        mIpb[3] = API_MODE;
        mIpb[4] = API_MODE;
        mIpb[5] = API_REPEATS;
        IOPB.transmitString( srcString , mIpb   ,   6 , 122 );
        IOPB.transmitString( dstString , mIpb   , 128 , 122 );
        IOPB.transmitBytes( data , mIpb, 256        , API_BLOCK/8 );
        IOPB.transmitBytes( data , mIpb, 131072+256 , 4096/8      );
        
        mPal.entryPAL( mIpb, mOpb, nI, nO );
        
        outW = mOpb[0];
        outR = mOpb[1];
        outC = mOpb[2];
        outD = mOpb[3];
        if ((outW==0)|(outR==0)|(outC==0)|(outD==0))
            {
            errorCode=1; 
            break;
            }
        
        nanosecondsWrite[i] = outW * API_NS / API_REPEATS;
        nanosecondsRead[i]  = outR * API_NS / API_REPEATS;
        nanosecondsCopy[i]  = outC * API_NS / API_REPEATS;
        counter+=3;
        }

    flag = false;
    }

    
}
