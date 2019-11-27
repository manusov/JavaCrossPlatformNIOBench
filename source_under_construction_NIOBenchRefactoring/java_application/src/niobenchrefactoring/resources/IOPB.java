/* 


Class with static helpers methods for conversion between 
IPB, OPB arrays of longs and arrays of bytes.
Note IPB = Input Parameters Block, OPB = Output Parameters Block.

*/

package niobenchrefactoring.resources;

public class IOPB 
{

public static String extractStringFromOPB( long[] data )
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
    
}
