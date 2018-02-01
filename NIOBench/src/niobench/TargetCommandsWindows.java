//---------- NIOBench, file I/O Benchmark utility. (C)2018 IC Book Labs --------
// Benchmarks target operation, child class for unbuffered native mode, Windows.
// This NATIVE mode not use NIO methods.
// BUG: Required make alignment optimization, same as for Linux.

package niobench;

import static niobench.NIOBench.API_WRITE_READ_COPY_DELETE;

public class TargetCommandsWindows extends TargetCommands 
{
private final static int  API_BLOCK = 1048576;     // Associated with caller
private final static long API_MODE = 0xA0000080L;  // Unbuffered, Uncached mode
private final static long API_REPEATS = 2;         // Internal measurement
private final static int  API_NS = 100;            // Nanoseconds per Unit
    
@Override public void run()
    {
    errorCode = 0;        
    String srcString = filePath + srcName + "0" + extension;
    String dstString = filePath + dstName + "0" + extension;
    long outW, outR, outC, outD;

    int nI = API_BLOCK/8 + 2048/8;   // Size of Input Parameters Block, qwords
    int nO = API_BLOCK/8 + 2048/8;   // Size of Output Parameters Block, qwords
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
        
        IOPB.transmitString( srcString , mIpb ,   6 , 122 );  // src. file name
        IOPB.transmitString( dstString , mIpb , 128 , 122 );  // dst. file name
        IOPB.transmitBytes( data , mIpb, 256 , API_BLOCK/8 ); // data can random
        
        mPal.entryPAL( mIpb, mOpb, nI, nO );
        
        outW = mOpb[0];
        outR = mOpb[1];
        outC = mOpb[2];
        outD = mOpb[3];
        
// ********** DEBUG ************************************************************

/*
        // FOR DEBUG: force nonzero time result
        if ( outW == 0 ) outW++;
        if ( outR == 0 ) outR++;
        if ( outC == 0 ) outC++;
*/

/*        
        // FOR DEBUG: force zero speed result
        long a = 1024*1024*1024*1024L;
        outW = a;
        outR = a;
        outC = a;
        outD = a;
*/

/*
        int a1 = (int)( outW & 0xFFFFFFFF );
        int a2 = (int)( (outW >> 32) & 0xFFFFFFFF );
        int a3 = (int)( outR & 0xFFFFFFFF );
        int a4 = (int)( (outR >> 32)  & 0xFFFFFFFF );
        int a5 = (int)( outC & 0xFFFFFFFF );
        int a6 = (int)( (outC >> 32)  & 0xFFFFFFFF );
        int a7 = (int)( outD & 0xFFFFFFFF );
        int a8 = (int)( (outD >> 32)  & 0xFFFFFFFF );
        
        String s1 = String.format( "%08X", a1 );
        String s2 = String.format( "%08X", a2 );
        String s3 = String.format( "%08X", a3 );
        String s4 = String.format( "%08X", a4 );
        String s5 = String.format( "%08X", a5 );
        String s6 = String.format( "%08X", a6 );
        String s7 = String.format( "%08X", a7 );
        String s8 = String.format( "%08X", a8 );
        
        System.out.println(  s2 + ":" + s1 +" | " + s4 + ":" + s3 
                                + " | " + s6 +":" + s5 +" | " + s8 +":" + s7 );

*/

// ********** END DEBUG ********************************************************
        
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