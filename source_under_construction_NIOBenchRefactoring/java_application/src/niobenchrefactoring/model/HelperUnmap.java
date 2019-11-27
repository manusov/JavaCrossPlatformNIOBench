/*
Helper for Java version-specific operation: memory mapped buffer unmap.
*/

package niobenchrefactoring.model;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

class HelperUnmap 
{
private final boolean version9plus;

HelperUnmap()
    {
    /*
    Detect Java version for select buffer unmap handler
    */
    String version = System.getProperty( "java.version" );
    if( version.startsWith( "1." ) )
        {
        version = version.substring( 2, 3 );
        }
    else 
        {
        int dot = version.indexOf( "." );
        if( dot != -1 ) 
            {
            version = version.substring( 0, dot ); 
            }
        }
    int n = Integer.parseInt( version );
    version9plus = ( n >= 9 );
    }

/*
Helper for java-version specfic unmap memory-mapped buffer
*/
StatusEntry unmap( MappedByteBuffer buffer )
    {
    boolean statusFlag = false;
    String statusString = "N/A";
    /*
    Handler for buffer unmap under Java 8
    */
    if ( ( buffer != null )&&( buffer.isDirect() )&&( !version9plus ) )
        {
        try {
            Method cleaner = buffer.getClass().getMethod("cleaner");
            cleaner.setAccessible( true );
            Method clean = Class.forName( "sun.misc.Cleaner" ).
                getMethod( "clean" );
            clean.setAccessible( true );
            clean.invoke( cleaner.invoke( buffer ) );
            statusFlag = true;
            }
        catch ( ClassNotFoundException | IllegalAccessException |
                IllegalArgumentException | NoSuchMethodException |
                SecurityException | InvocationTargetException e )
            {
            statusFlag = false;
            statusString = "Unmap failed (Java 8): " + e.getMessage();
            }
        }
    /*
    Handler for buffer unmap under Java 9+
    */
    else if ( ( buffer != null )&&( buffer.isDirect() )&&( version9plus ) )
        {
        try
            {
            Class<?> unsafeClass = Class.forName( "sun.misc.Unsafe" );
            Field unsafeField = unsafeClass.getDeclaredField( "theUnsafe" );
            unsafeField.setAccessible( true );
            Object unsafe = unsafeField.get( null );
            Method invokeCleaner = unsafeClass.
            getMethod( "invokeCleaner", ByteBuffer.class );
            invokeCleaner.invoke( unsafe, buffer );
            statusFlag = true;
            }
        catch ( ClassNotFoundException | IllegalAccessException |
                IllegalArgumentException | NoSuchFieldException |
                NoSuchMethodException | SecurityException |
                InvocationTargetException e )
            {
            statusFlag = false;
            statusString = "Unmap failed (Java 9+): " + e.getMessage();
            }
        }
    /*
    Return status
    */
    return new StatusEntry( statusFlag, statusString );
    }
}
