/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Class with static helpers methods for conversion between 
IPB, OPB arrays of longs and arrays of bytes.
Note IPB = Input Parameters Block, OPB = Output Parameters Block.
*/

package niobenchrefactoring.resources;

public class IOPB 
{
/*
Read string as sequence of chars from array of longs.
Each long unpack to 8 bytes, each byte zero-extend to 16-bit word.
Method used for read native I/O library name.
data   = source array of long numbers, contain chars as ASCII bytes
return = string as sequence of 16-bit chars
*/
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

/*
Read sequence of bytes from array of longs. Each long unpack to 8 bytes.
Method used for read data from random numbers generator
during native I/O benchmarks.
qwords = source array of long numbers
bytes  = destination array of bytes
*/
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

/*
Copy string as sequence of ASCII bytes to array of longs.
Each 16-bit char converted to 8-bit ASCII char, as byte,
each 8-byte group pack to long.
Method used for write file names to IPB during native I/O benchmarks.
s    = source string
ipb  = destination array
base = offset in the destination array, units = long numbers
*/
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
Note about DTA.
DTA base + OPB base + OPB size.
DTA = Data Transfer Area,
OPB = Output Parameters Block.
*/
/*
Copy source bytes array to destination long array, for integer number of longs.
Each 8-byte group pack to long.
Method used for write data to files during native I/O benchmarks.
dstQwords = destination array
srcBytes  = source array
dstOffset = offset in the destination array, units = long numbers
length    = copy size, units = long numbers
*/
public static void transmitBytesToDTA
            ( long[] dstQwords, byte[] srcBytes, int dstOffset, int length )
    {
    int k = 0;
    for ( int i=0; i<length; i++ )
        {
        long q = 0;
        for( int j=0; j<8; j++ )
            {
            long lb = (long)( ( (long) srcBytes[k] ) & 0xFFL ) ;
            lb = lb << ( j * 8 );
            q = q | lb;
            k++;
            }
        dstQwords[i + dstOffset] = q;
        }
    }
}
