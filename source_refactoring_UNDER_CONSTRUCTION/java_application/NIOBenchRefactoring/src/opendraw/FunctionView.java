/*
NIOBench. Mass storage and file I/O benchmark utility. 
(C)2020 IC Book Labs, the code is written by Manusov I.V.
Project second generation, refactoring started at 2019-2020.
-----------------------------------------------------------------------------
Package for support functions Y=F(X) drawings.
Function Y=F(X) drawing view.
*/

package opendraw;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.math.BigDecimal;
import javax.swing.JPanel;

public class FunctionView implements FunctionViewInterface
{
private final FunctionControllerInterface controller;
private final FunctionModelInterface model;
private final JPanel panel;
    
public FunctionView( FunctionControllerInterface x )
    {
    controller = x;
    model = controller.getModel();
    panel = new FunctionDrawPanel();
    }

@Override public JPanel getPanel()
    {
    return panel;
    }

private class FunctionDrawPanel extends JPanel
    {
    
    @Override public void paint( Graphics g )
        {

        Color backgroundColor = Color.WHITE;
        Color axisColor       = Color.BLACK;
        Color gridColor       = Color.LIGHT_GRAY;
        Color[] drawColors = new Color[] 
            { Color.BLUE , Color.RED , Color.MAGENTA };
        Font axisFont = new Font ( "Verdana", Font.PLAIN, 10 );
        Font textFont = new Font ( "Verdana", Font.BOLD + Font.ITALIC, 14 );

        Rectangle r = g.getClipBounds();
        g.setColor( backgroundColor );
        g.setFont( axisFont );
        g.fillRect( r.x , r.y ,  r.width , r.height );

        g.setColor( axisColor );
        int xleft   = r.x + 45;
        int xright  = r.width - 40;
        int ybottom = r.height - 40;
        int ytop    = 40;
        int xrightCorrected = xright;
        int ytopCorrected   = ytop;
        
        BigDecimal xmax = model.getXmax();
        BigDecimal xmin = model.getXmin();
        BigDecimal xsmall = model.getXsmallUnits();
        BigDecimal xbig = model.getXbigUnits();
        if ( xleft < xright )
            {  // X axis
            g.drawLine( xleft, ybottom, xright+20, ybottom );
            int dx = 
               ( ( xmax.subtract( xmin ) ).divideToIntegralValue( xsmall ) ).
               intValue();
            int px = ( xright - xleft ) / dx ;
            if ( px > 2 )
                {
                for( int i = xleft + px; i <= xright; i += px )
                    {  // X small points
                    g.drawLine( i, ybottom-1, i, ybottom+1 );
                    }
                }
            int rx = xbig.divideToIntegralValue( xsmall ).intValue();
            if ( (px*rx) > 2 )
                {
                BigDecimal xnums = xmin;
                String sx = xnums.toString();
                g.drawString( sx, xleft-4, ybottom+14 );
                for( int i = xleft+px*rx; i <= xright; i += (px*rx) )
                    {  // X big points
                    g.drawLine( i, ybottom-3, i, ybottom+3 );
                    xnums = xnums.add( xbig );
                    if ( xnums.compareTo( xmax ) == 0 )  xrightCorrected = i;
                    sx = xnums.toString();
                    FontMetrics fm = g.getFontMetrics();
                    int sw = fm.stringWidth( sx );
                    if ( sw < (px*rx) )  g.drawString( sx, i-4, ybottom+14 );
                    if ( (ybottom-3) > ytop )
                        {  // X grid vertical lines
                        Color c = g.getColor();
                        g.setColor( gridColor );
                        g.drawLine( i, ybottom-3, i, ytop );
                        g.setColor( c );
                        }
                    }
                }
            
            if ( xleft < (xright-5) )
                {  // X arrow
                g.fillRect( xright-2+20, ybottom-1, 2, 3 );
                g.fillRect( xright-4+20, ybottom-2, 2, 5 );
                g.fillRect( xright-6+20, ybottom-3, 2, 7 );
                }
            }
        
        BigDecimal ymax = model.getYmax();
        BigDecimal ymin = model.getYmin();
        BigDecimal ysmall = model.getYsmallUnits();
        BigDecimal ybig = model.getYbigUnits();
        if ( ytop < ybottom )
            {  // Y axis
            g.drawLine( xleft, ybottom, xleft, ytop-13 );
            int dy = 
               ( ( ymax.subtract( ymin ) ).divideToIntegralValue( ysmall ) ).
               intValue();
            int py = ( ybottom - ytop ) / dy;
            if ( py > 3 )
                {
                for( int i = ybottom-py; i >= ytop; i -= py )
                    {  // Y small points
                    g.drawLine( xleft-1, i, xleft+1, i );
                    }
                }
            int ry = ybig.divideToIntegralValue( ysmall ).intValue();
            if ( (py*ry) > 3 )
                {
                BigDecimal ynums = ymin;
                String sy;
                for( int i = ybottom - py*ry; i >= ytop; i -= (py*ry) )
                    {  // Y big points
                    g.drawLine( xleft-3, i, xleft+3, i );
                    ynums = ynums.add( ybig );
                    if ( ynums.compareTo( ymax ) == 0 )  ytopCorrected = i;
                    sy = ynums.toString();
                    FontMetrics fm = g.getFontMetrics();
                    int sw = fm.stringWidth( sy );
                    int sh = fm.getHeight();
                    if ( ( ( xleft-sw-2) > 1 ) && ( sh < (py*ry) ) )
                        g.drawString( sy, xleft-sw-4, i+4 );
                    if ( ( xleft-3 ) < xright )
                        {  // Y grid horizontal lines
                        Color c = g.getColor();
                        g.setColor( gridColor );
                        g.drawLine( xleft+3, i, xright, i );
                        g.setColor( c );
                        }
                    }
                }
            if ( ytop < (ybottom-6) )
                {  // Y arrow
                g.fillRect( xleft-1, ytop+2-13, 3, 2 );
                g.fillRect( xleft-2, ytop+4-13, 5, 2 );
                g.fillRect( xleft-3, ytop+6-13, 7, 2 );
                }
            }
        
        g.setFont( textFont );
        g.setColor( drawColors[0] );
        FontMetrics fm = g.getFontMetrics();
        String sx = model.getXname();
        int sw = fm.stringWidth( sx );
        g.drawString( sx, r.width-sw-7, r.height-7 );  // X axis units name
        
        String[] drawNames = model.getYnames();
        int drawShift = 10;
        int k = Integer.min ( drawColors.length, drawNames.length );
        
        for( int i=0; i<k; i++ )
            { // Y axis units name
            g.setColor( drawColors[i] );
            g.drawString( drawNames[i], drawShift, 20 );
            drawShift += fm.stringWidth( drawNames[i] ) + 10;
            }

        if ( ( xleft < xright ) && ( ytop < ybottom ) )
            {
            BigDecimal[][] f = model.getFunction();
            double px = ( xrightCorrected - xleft ) /
                ( xmax.doubleValue() - xmin.doubleValue() );
            double py = ( ybottom - ytopCorrected ) /
                ( ymax.doubleValue() - ymin.doubleValue() );
            int[] x1 = new int[k];
            int[] y1 = new int[k];
            
            for( int j=0; j<k; j++ )
                {
                // int n = model.getCurrentIndexes()[0];
                int n = model.getCurrentIndexes()[j];
                //
                g.setColor( drawColors[j] );
                for( int i=0; i<n; i++ )
                    {  // draw function Y = F(X)
                    double x = f[0][i].doubleValue() * px + xleft;
                    if ( f[j+1][i] != null )
                        {
                        double y = ybottom - f[j+1][i].doubleValue() * py;
                        int x2 = (int)x;
                        int y2 = (int)y;
                        if ( i > 0 )  g.drawLine( x1[j], y1[j], x2, y2 );
                        x1[j] = x2;
                        y1[j] = y2;
                        }
                    }
                }
            }
        }
    }
}
