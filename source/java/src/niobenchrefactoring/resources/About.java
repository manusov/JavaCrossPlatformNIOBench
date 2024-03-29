/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2021 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Vendor, Product and Version description strings, web site link, 
pictogram resource reference.
*/

package niobenchrefactoring.resources;

public class About 
{
private final static String VERSION_NAME = "v0.03.06";
private final static String VENDOR_NAME  = "(C)2022 IC Book Labs";
private final static String SHORT_NAME   = "NIOBench " + VERSION_NAME;
private final static String LONG_NAME    = "Java " + SHORT_NAME;
private final static String WEB_SITE     = "https://github.com/manusov";
private final static String VENDOR_ICON  = 
                                "/niobenchrefactoring/resources/icbook.jpg";
public static String getVersionName() { return VERSION_NAME; }
public static String getVendorName()  { return VENDOR_NAME;  }
public static String getShortName()   { return SHORT_NAME;   }
public static String getLongName()    { return LONG_NAME;    }
public static String getWebSite()     { return WEB_SITE;     }
public static String getVendorIcon()  { return VENDOR_ICON;  }
}
