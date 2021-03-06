public class TST32
{
   public static void main(String[] args)
   {
   JNITESTLINUX32 jt = new JNITESTLINUX32();

   
   int x1 = jt.checkPAL();
   // System.out.println (x1);

   /*
   int x = jt.entryPAL( 10, 15, 17, 13 );
   System.out.println(x);
   */

   /*
   long[] a1 = { 3, 3, 3, 3, 3, 3, 3, 3, 3, 3 };
   long[] b1 = { 7, 7, 7, 7, 7, 7, 7, 7, 7, 7 };
   long c1 = 8;
   long d1 = 10;
   int x2 = jt.entryPAL( a1, b1, c1, d1 );
   System.out.println (x2);
   for(int i=0; i<a1.length; i++)
     { System.out.printf ( "" + a1[i] + " | " ); }
   System.out.println();
   for(int i=0; i<b1.length; i++)
     { System.out.printf ( "" + b1[i] + " | " ); }
   System.out.println();
   */

   
   //--- CPUID test ---
   /*
   long[] a1 = { 3, 3, 3, 3, 3, 3, 3, 3, 3, 3 };
   long[] b1 = { 7, 7, 7, 7, 7, 7, 7, 7, 7, 7 };
   long c1 = 0;
   long d1 = 10;
   int x2 = jt.entryPAL( null, b1, c1, d1 );
   System.out.println (x2);
   for(int i=0; i<a1.length; i++)
     { System.out.printf ( "" + a1[i] + " | " ); }
   System.out.println();
   for(int i=0; i<b1.length; i++)
     { System.out.printf ( "" + b1[i] + " | " ); }
   System.out.println();
   */
   
   

   //--- RDRAND test ---
   /*
   c1=10;
   d1=10;
   a1[0]=0;
   a1[1]=5;
   x2 = jt.entryPAL( a1, b1, c1, d1 );
   System.out.println (x2);
   for(int i=0; i<a1.length; i++)
     { System.out.printf ( "" + a1[i] + " | " ); }
   System.out.println();
   for(int i=0; i<b1.length; i++)
     { System.out.printf ( "" + b1[i] + " | " ); }
   System.out.println();
   */
   
   
   
   System.out.println("Java NativeBench v0.30 for Linux32");
   System.out.println("(C)IC Book Labs.");
   System.out.println("Benchmarking...");

   int API_WRITE_READ_COPY_DELETE = 1;

   int API_BLOCK = 1024*1024;         
   int API_BLOCK_ALIGN = API_BLOCK + 4096;   // 4096 for page align

   int fileSize = 100;  // units=API_BLOCK

// int API_MODE = 0x00000042;
   int API_MODE = 0x00004042;

   int API_REPEATS = 5;

   String srcString = "mysrcfile.bin";
   String dstString = "mydstfile.bin";
   long outW, outR, outC, outD, outE;
   int nI = API_BLOCK_ALIGN/8 + 2048/8;   // Size of IPB, qwords
   int nO = API_BLOCK_ALIGN/8 + 2048/8;   // Size of OPB, qwords
   long[] mIpb = new long[nI];  
     for ( int i=0; i<nI; i++ ) { mIpb[i]=0; }
   long[] mOpb = new long[nO];  
     for ( int i=0; i<nO; i++ ) { mOpb[i]=0; }

   mIpb[0] = API_WRITE_READ_COPY_DELETE;
   mIpb[1] = API_BLOCK;
   mIpb[2] = fileSize;
   mIpb[3] = API_MODE;
   mIpb[4] = API_MODE;
   mIpb[5] = API_REPEATS;
   IOPB.transmitString( srcString , mIpb ,   6 , 122 );
   IOPB.transmitString( dstString , mIpb , 128 , 122 );
   // IOPB.transmitBytes( data , mIpb, 256 , API_BLOCK/8 );

   jt.entryPAL( mIpb, mOpb, nI, nO );
        
   outW = mOpb[0] / 1000;  // ns to us
   outR = mOpb[1] / 1000;
   outC = mOpb[2] / 1000;
   outD = mOpb[3];
   outE = mOpb[4];

   long totalBytes = API_BLOCK * fileSize * API_REPEATS;
   if ( outW > 0) { outW = totalBytes / outW; }
   if ( outR > 0) { outR = totalBytes / outR; }
   if ( outC > 0) { outC = totalBytes / outC; }
   // outD = totalBytes / outD;

   System.out.println("MBPS");
   System.out.println("Read  = " + outR );
   System.out.println("Write = " + outW );
   System.out.println("Copy  = " + outC );
   System.out.println("Delete status = " + outD );
   


   /*
   outE &= 0xFFF;
   System.out.println(outE);
   */

   }
}

class JNITESTLINUX32
{ 
  { System.loadLibrary("LINUX32JNI"); }
  public native int checkPAL();

  public native int entryPAL( long[] a, long[] b, long c, long d );
// public native int entryPAL(int a, int b, int c, int d);

}

class IOPB 
{

public static void transmitBytes
    ( byte[] bytearray, long[] longipb, int base, int length )
    {
    int n = length;
    int m = n*8;
    long x=0, y=0;
    int k=0;
    for (int i=0; i<n; i++) { longipb[base+i]=0; }
    for (int i=0; i<n; i++)
        {
        for(int j=0; j<8; j++)
           {
           x = bytearray[k] & 0xFF;
           k++;
           x = x << 56;
           y = y >>> 8;
           y = y + x;
           }
        longipb[base+i]=y;
        y=0;
        }
    }

public static byte[] receiveBytes
    ( long[] longopb, int base, int length )
    {
    int n = length;
    int m = n*8;
    long x=0, y=0;
    int k=0;
    byte[] bytearray = new byte[m];
    for (int i=0; i<m; i++) { bytearray[i]=0; }
    for (int i=0; i<n; i++)
        { 
        x = longopb[base+i];
        for (int j=0; j<8; j++)
            {
            y = x & 0xFF;
            x = x >>> 8;
            bytearray[k] = (byte)y;
            k++;
            }
        }
    return bytearray;
    }
    
public static void transmitString( String s, long[] ipb, int base, int length )
    {
    int sl = s.length();
    byte[] array = new byte[1024];
    for ( int i=0; i<1024; i++ ) { array[i]=0;}
    for ( int i=0; i<sl; i++  ) { array[i] = (byte) s.charAt(i); }
    IOPB.transmitBytes( array, ipb, base, length );
    }

public static String receiveString( long[] opb, int base, int length )
    {
    String s1=null;
    StringBuffer s2 = new StringBuffer();
    byte[] array = IOPB.receiveBytes(opb, base, length);
    for (int i=0; i<array.length; i++)
        {
        if ( array[i]==0 ) break;
        s2.append( (char)array[i] );
        }
    s1 = s2.toString();
    return s1;        
    }

}


