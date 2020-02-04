/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
IO scenario class for Archives benchmark, include phases: 
files prepare, pack files into archive, unpack files from archive.
Use java built-in zip support.
*/

package niobenchrefactoring.model;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class IOscenarioArchives extends IOscenarioChannel // IOscenario
{
private final static String  DEFAULT_PATH_ZIP     = "C:\\TEMP\\";
private final static String  DEFAULT_PREFIX_ZIP   = "arc";
private final static String  DEFAULT_POSTFIX_ZIP  = ".zip";

final String pathZip;
final String prefixZip;
final String postfixZip;
final String[] namesZip;
final Path[] pathsZip;
final ZipInputStream[] zis;
final ZipOutputStream[] zos;

/*
Default constructor
*/
/*
public IOscenarioArchives()
    {
    super();
    pathZip    = DEFAULT_PATH_ZIP;
    prefixZip  = DEFAULT_PREFIX_ZIP;
    postfixZip = DEFAULT_POSTFIX_ZIP;
    namesZip = new String[fileCount];
    pathsZip = new Path[fileCount];
    zis = new ZipInputStream[fileCount];
    zos = new ZipOutputStream[fileCount];
    zipInitHelper();
    }
*/

/*
Constructor with parameters
*/
public IOscenarioArchives
    ( String pathSrc, String prefixSrc, String postfixSrc,
      String pathZip, String prefixZip, String postfixZip,
      String pathDst, String prefixDst, String postfixDst,
      int fileCount, int fileSize, int blockSize, int threadCount,
      boolean readSync, boolean writeSync, boolean copySync, 
      boolean dataSparse, boolean fastCopy,
      int readWriteMode, int addressMode, int dataMode,
      int readDelay, int writeDelay, int copyDelay,
      byte[] dataBlock )
    {
    super( pathSrc, prefixSrc, postfixSrc, pathDst, prefixDst, postfixDst,
           fileCount, fileSize, blockSize, threadCount,
           readSync, writeSync, copySync, dataSparse, fastCopy,
           readWriteMode, addressMode, dataMode,
           readDelay, writeDelay, copyDelay, dataBlock );
    this.pathZip    = ( pathZip == null    ) ? DEFAULT_PATH_ZIP    : pathZip;
    this.prefixZip  = ( prefixZip == null  ) ? DEFAULT_PREFIX_ZIP  : prefixZip;
    this.postfixZip = ( postfixZip == null ) ? DEFAULT_POSTFIX_ZIP : postfixZip;
    namesZip = new String[fileCount];
    pathsZip = new Path[fileCount];
    zis = new ZipInputStream[fileCount];
    zos = new ZipOutputStream[fileCount];
    zipInitHelper();
    }

/*
Helper for class constructor    
*/    
private void zipInitHelper()
    {
    for( int i=0; i<fileCount; i++ )
        {
        namesZip[i] = pathZip + prefixZip + i + postfixZip;
        try 
            {
            pathsZip[i] = fileSystem.getPath( namesZip[i] );
            zos[i] = new ZipOutputStream( new FileOutputStream( namesZip[i] ) );
            zis[i] = new ZipInputStream( new FileInputStream( namesZip[i] ) );
            }
        catch ( FileNotFoundException e )
            {
            lastError = new StatusEntry( false, e.getMessage() );
            }
        }
    }

/*
Run performance scenario    
*/
@Override public void run()
    {
    iotWrite = new IOtaskChannelWriteMT( this );    
    iotCopy  = new IOtaskArchivePack( this );
    iotRead  = new IOtaskArchiveUnpack( this );
/*    
    if ( errorCheck() )
        {
        threadHelper( iotWrite );
        }
    
    if ( errorCheck() )
        {
        threadHelper( iotCopy );
        }
    
    if ( errorCheck() )
        {
        threadHelper( iotRead );
        }
*/

    if ( ( ! interrupt ) && ( ! isInterrupted() ) && errorCheck() )
        {
        // preDelay( writeDelay, WRITE_DELAY_NAME );
        threadHelper( iotWrite );
        }
    
    if ( ( ! interrupt ) && ( ! isInterrupted() ) && errorCheck() )
        {
        // preDelay( copyDelay, COPY_DELAY_NAME );
        threadHelper( iotCopy );
        }
    
    if ( ( ! interrupt ) && ( ! isInterrupted() ) && errorCheck() )
        {
        // preDelay( readDelay, READ_DELAY_NAME );
        threadHelper( iotRead );
        }

    setSync( 0, lastError, DELETE_ID, DELETE_NAME );
    delete( pathsSrc, channelsSrc );
    delete( pathsDst, channelsDst );
    deleteZip( pathsZip, zis, zos );
    }

/*
Helper for delete archive files
*/
void deleteZip( Path[] path, ZipInputStream[] zis, ZipOutputStream[] zos )
    {
    if ( ( path == null ) || ( path.length == 0 ) ||
         ( zis == null  ) || ( zis.length == 0)   ||
         ( zos == null  ) || ( zos.length == 0 )  )
        {
        lastError = new StatusEntry( false, "Delete context bad" );
        }
    else
        {
        for( int i=0; i<path.length; i++ )
            {
            try {
                if ( ( path[i] != null) ||
                     ( zis[i] != null ) || ( zos[i] != null ) )
                    {
                    zis[i].close();
                    zos[i].close();
                    Files.delete( path[i] );
                    }
                }
            catch ( IOException e )
                {
                lastError =
                    new StatusEntry( false, "Delete error: " + e.getMessage() );
                }
            }
        }
    }

}
