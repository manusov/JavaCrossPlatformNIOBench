//---------- NIOBench, file I/O Benchmark utility. (C)2018 IC Book Labs --------
// Strings with getters for product and vendor information.
// This module added at v0.46, centralize vendor info.

// Edit version number at this file, one time.
// Note 2 console messages in the debug version:
// at start and at native benchmark.
// Edit native DLL for Win64, flush added and removed by comments.

package niobench;

public class About {

private final static String VERSION_NAME = "v0.49";    //  DEBUG 4";
private final static String VENDOR_NAME  = "(C)2018 IC Book Labs";
private final static String SHORT_NAME   = "NIOBench " + VERSION_NAME;
private final static String LONG_NAME    = "Java " + SHORT_NAME;
private final static String WEB_SITE     = "http://icbook.com.ua";
private final static String VENDOR_ICON  = "/niobench/resources/icbook.jpg";

public static String getVersionName() { return VERSION_NAME; }
public static String getVendorName()  { return VENDOR_NAME;  }
public static String getShortName()   { return SHORT_NAME;   }
public static String getLongName()    { return LONG_NAME;    }
public static String getWebSite()     { return WEB_SITE;     }
public static String getVendorIcon()  { return VENDOR_ICON;  }
    
}
