/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Class with static helpers methods for conversion between 
IPB, OPB arrays of longs and arrays of bytes.
Note IPB = Input Parameters Block, OPB = Output Parameters Block.
*/

package niobenchrefactoring.resources;

public class IOPB 
{

public static String receiveStringFromOPB( long[] data )
    {
    StringBuilder sb = new StringBuilder( "" );
    for( int i=0; i<data.length; i++ )
        {
        long d = data[i];
        for( int j=0; j<8; j++ )
            {
            char c = (char)( d & 0xFF );
            if ( c == 0 )
                break;
            sb.append( c );
            d = d >> 8;
            }
        }
    return sb.toString();
    }

public static void receiveBytesFromOPB( long[] qwords, byte[] bytes )
    {
    int k = 0;
    for ( int i=0; i<qwords.length; i++ )
        {
        long q = qwords[i];
        for ( int j=0; j<8; j++ )
            {
            bytes[k] = (byte)( q & 0xFF );
            k++;
            q >>>= 8;
            }
        }
    }

public static void transmitStringToIPB( String s, long[] ipb, int base )
    {
    int i = base;
    int j = 0;
    byte b = 0;
    while ( i < ipb.length )
        {
        long data = 0;
        for( int k=0; k<8; k++ )
            {
            if ( j < s.length() )
                {
                b = (byte) s.charAt(j);
                }
            else if ( j == s.length() )
                {
                b = 0;
                }
            else
                {
                break;
                }
            j++;
            long a = b & 0xFFL;
            data |= a << ( k*8 );
            }
        ipb[i] = data;
        i++;
        if ( b == 0 )
            {
            break;
            }
        }
    }

/*
This irregular because OPB contains input data, refactoring required.
Or assignment old OPB = OPB + DTA (Data Transfer Area)
*/
public static void transmitBytesToOPB
            ( long[] qwords, byte[] bytes, int offset, int length )
    {
    int k = 0;
    for ( int i=0; i<length; i++ )
        {
        long q = 0;
        for( int j=0; j<8; j++ )
            {
            long lb = (long)( ( (long) bytes[k] ) & 0xFFL ) ;
            lb = lb << ( j * 8 );
            q = q | lb;
            k++;
            }
        qwords[i + offset] = q;
        }
    }
}
