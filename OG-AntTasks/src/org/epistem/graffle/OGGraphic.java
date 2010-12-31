/*--------------------------------------------------------------------------------
  Copyright (c) 2010, David N. Main
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
--------------------------------------------------------------------------------*/
package org.epistem.graffle;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.StringReader;
import java.util.*;

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.rtf.RTFEditorKit;

import static org.epistem.graffle.OGUtils.*;

/**
 * A graphic
 *
 * @author nickmain
 */
@SuppressWarnings("unchecked")
public final class OGGraphic {

    public static enum GraphicClass {
        LineGraphic,
        ShapedGraphic,
        Group,
        TableGroup
    }
    
    private final Map<String, Object> dict;
    public  final OGSheet sheet;
    public  final OGGraphic parent;
    private OGLayer layer;    
    
    OGGraphic( OGSheet sheet, OGGraphic parent, Map<String, Object> dict ) {
        this.dict = dict;
        this.sheet = sheet;
        this.parent = parent;
    }

    /**
     * Whether a group is a subgraph
     */
    public boolean isSubgraph() {
        Boolean b = (Boolean) dict.get( "isSubgraph" );
        if( b == null ) return false;
        return b;
    }
    
    /** Get the graphic type */
    public GraphicClass graphicClass() {
        return GraphicClass.valueOf( (String ) dict.get( "Class" ) );
    }
    
    /**
     * Get the layer this graphic is on
     */
    public OGLayer layer() {
        if( layer == null ) {
            Integer layerIndex = (Integer) dict.get( "Layer" );
            if( layerIndex == null ) return null;
            layer = sheet.layers[ layerIndex ];
        }
        return layer;
    }
    
    /**
     * Get the stroke style (zero is solid)
     */
    public int strokePattern() {
        Map<String,Object> style = (Map<String,Object>) dict.get( "Style" );
        if( style == null ) return 0;
        Map<String,Object> stroke = (Map<String,Object>) style.get( "stroke" );
        if( stroke == null ) return 0;

        Integer pattern = (Integer) stroke.get( "Pattern" );
        if( pattern == null ) return 0;
        return pattern;
    }
    
    /**
     * Get the graphic's id
     */
    public int id() {
        return (Integer) dict.get( "ID" );
    }
    
    /**
     * Get the notes, if any
     */
    public String notes() {
        String s = unRTF( (String) dict.get( "Notes" ));
        if( s == null ) return null;
        if( s.endsWith( "\n" ) ) s = s.substring( 0, s.length() - 1 );
        return s;
    }
    
    /**
     * Get the rows of a table
     * @return
     */
    public List<List<OGGraphic>> tableRows() {
        List<List<OGGraphic>> rows = new ArrayList<List<OGGraphic>>();
        if( graphicClass() != GraphicClass.TableGroup ) return rows;
        
        SortedMap<Double, SortedMap<Double, OGGraphic>> rowMap = 
            new TreeMap<Double, SortedMap<Double,OGGraphic>>();
        
        for( OGGraphic g : graphics() ) {
            Rectangle2D rect = g.bounds();
            
            SortedMap<Double,OGGraphic> row = rowMap.get( rect.getY() );
            if( row == null ) {
                row = new TreeMap<Double, OGGraphic>();
                rowMap.put( rect.getY(), row );
            }
            
            row.put( rect.getX(), g );
        }
        
        for( SortedMap<Double,OGGraphic> row : rowMap.values() ) {
            List<OGGraphic> rowList = new ArrayList<OGGraphic>();
            rows.add( rowList );
            
            for( OGGraphic g : row.values() ) {
                rowList.add( g );
            }
        }
        
        return rows;
    }
    
    /**
     * Get the cells of a table in an [x][y] array
     */
    public OGGraphic[][] table() {
        List<List<OGGraphic>> tableRows = tableRows();
        if( tableRows.isEmpty() ) return new OGGraphic[0][];
        
        int rowCount = tableRows.size();
        int rowSize  = tableRows.get(0).size();
        OGGraphic[][] table = new OGGraphic[ rowSize ][ rowCount ];
        for( int y = 0; y < rowCount; y++ ) {
            List<OGGraphic> row = tableRows.get( y );
            
            for( int x = 0; x < rowSize; x++ ) {
                OGGraphic cell = row.get( x );
                table[x][y] = cell;
            }
        }       
        
        return table;
    }
    
    /**
     * Get the user defined properties
     */
    public Map<String,String> userProperties() {
        return (Map<String,String>) dict.get( "UserInfo" );
    }
    
    /**
     * Get the shape of a ShapedGraphic
     */
    public String shape() {
        return (String) dict.get( "Shape" );
    }
    
    /**
     * The id of the attached image
     * @return null if this shape does not have an image
     */
    public Integer imageId() {
        return (Integer) dict.get( "ImageID" );
    }
    
    /**
     * Get the bounds of a shape graphic
     */
    public Rectangle2D bounds() {
        String bounds = (String) dict.get( "Bounds" );
        if( bounds == null ) bounds = "{{0,0}, {0,0}}";
        
        StringTokenizer tok = new StringTokenizer( bounds, " {}," );
        
        return new Rectangle2D.Double( 
                       Double.parseDouble( tok.nextToken() ),
                       Double.parseDouble( tok.nextToken() ),                
                       Double.parseDouble( tok.nextToken() ),                
                       Double.parseDouble( tok.nextToken() ));
    }
    
    /**
     * Get the graphics within a group graphic
     */
    public List<OGGraphic> graphics() {
        List<OGGraphic> oggraphics = new ArrayList<OGGraphic>();
        
        List<Object> graphics = (List<Object>) dict.get( "Graphics" );
        for( Object dict : graphics ) {
            oggraphics.add( new OGGraphic( sheet, this, (Map<String,Object>) dict ) );
        }
        
        return oggraphics;
    }
    
    /**
     * Get the points for a line graphic
     */
    public List<Point2D> points() {
        List<Point2D> points = new ArrayList<Point2D>();
        
        List<Object> pp = (List<Object>) dict.get( "Points" );
        for( Object p : pp ) {
            StringTokenizer tok = new StringTokenizer( (String) p, " {}," );
            points.add( new Point2D.Double( 
                                Double.parseDouble( tok.nextToken() ),
                                Double.parseDouble( tok.nextToken() ) ));
        }
        
        return points;
    }
    
    /**
     * Get the head arrow type for a line graphic
     */
    public String headArrow() {
        Map<String,Object> style = (Map<String,Object>) dict.get( "Style" );
        if( style == null ) return "0";
        Map<String,Object> stroke = (Map<String,Object>) style.get( "stroke" );
        if( stroke == null ) return "0";
        
        return (String) stroke.get( "HeadArrow" );
    }

    /**
     * Get the tail arrow type for a line graphic
     */
    public String tailArrow() {
        Map<String,Object> style = (Map<String,Object>) dict.get( "Style" );
        if( style == null ) return "0";
        Map<String,Object> stroke = (Map<String,Object>) style.get( "stroke" );
        if( stroke == null ) return "0";
        
        return (String) stroke.get( "TailArrow" );
    }
    
    /**
     * The id of the shape that the line head is connected to.
     * @return zero if none
     */
    public int headId() {
        Map<String,Object> head = (Map<String,Object>) dict.get( "Head" );
        if( head == null ) return 0;
        
        return (Integer) head.get( "ID" );        
    }
    
    /**
     * The id of the shape that the line tail is connected to.
     * @return zero if none
     */
    public int tailId() {
        Map<String,Object> tail = (Map<String,Object>) dict.get( "Tail" );
        if( tail == null ) return 0;
        
        return (Integer) tail.get( "ID" );        
    }
    
    /**
     * The id of the line that this shape is a label for
     * @return zero if none
     */
    public int labelLineId() {
        Map<String,Object> line = (Map<String,Object>) dict.get( "Line" );
        if( line == null ) return 0;
        
        return (Integer) line.get( "ID" );                
    }
    
    /**
     * The position on the target line if this shape is a label
     * @return zero if none
     */
    public double labelPosition() {
        Map<String,Object> line = (Map<String,Object>) dict.get( "Line" );
        if( line == null ) return 0;
        
        return (Double) line.get( "Position" );
    }
    
    /**
     * Get the text of the shape
     */
    public String text() {
        Map<String,Object> text = (Map<String,Object>) dict.get( "Text" );
        if( text == null ) return null;
        
        String s = (String) text.get( "Text" );
        s = unRTF( s );
        if( s == null ) return null;
        if( s.endsWith( "\n" ) ) s = s.substring( 0, s.length() - 1 );
        return s;
    }
    
    /**
     * Get the styled text
     */
    public DefaultStyledDocument styledText() {
        Map<String,Object> text = (Map<String,Object>) dict.get( "Text" );
        if( text == null ) return null;

        String s = (String) text.get( "Text" );
        if( s == null ) return null;

        DefaultStyledDocument doc = new DefaultStyledDocument();
        RTFEditorKit kit = new RTFEditorKit();
        
        try {
            kit.read( new StringReader( s ), doc, 0 );
        }
        catch( Exception ex ) {
            throw new RuntimeException( ex );
        }
        
        return doc;
    }
}
