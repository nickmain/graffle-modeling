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
package org.epistem.diagram.model;

import java.awt.geom.Point2D;
import java.util.*;

import org.epistem.graffle.OGGraphic;

/**
 * A Line
 *
 * @author nickmain
 */
public class Line extends Graphic implements GraphicContainer, Connector {

    TreeMap<Double, Shape> labelMap = new TreeMap<Double, Shape>();

    public Graphic head;
    public Graphic tail;
    public final String headArrow;
    public final String tailArrow;
    public final List<Point2D> points;
    
    public HashSet<Line> lineGroup;
    
    /** Ordered, from tail to head */
    public Collection<Shape> labels = labelMap.values();            

    /** @see org.epistem.diagram.model.Connector#getHead() */
    public Graphic getHead() {
        return head;
    }
    
    /** @see org.epistem.diagram.model.Connector#getTail() */
    public Graphic getTail() {
        return tail;
    }

    /** @see java.lang.Iterable#iterator() */
    public Iterator<Graphic> iterator() {
        return new ArrayList<Graphic>( labels ).iterator();
    }
    
    @Override
    public boolean isSolid() {
        return isSolid;
    }

    /**
     * Accept a visitor
     */
    public void accept( DiagramVisitor visitor ) {
        DiagramVisitor labelVisitor = visitor.visitLineStart( this );
        
        if( labelVisitor != null ) {
            for( Graphic g : labels ) g.accept( labelVisitor );
        }
        
        visitor.visitLineEnd( this );
    }
    
    Line( OGGraphic ogg, GraphicContainer parent, Page page ) {
        super( ogg, parent, page );
        
        headArrow = ogg.headArrow();
        tailArrow = ogg.tailArrow();
        points    = ogg.points();
    }

    /** @see org.epistem.diagram.model.Graphic#init() */
    @Override
    void init() {
        head = page.graphics.get( ogg.headId() );
        tail = page.graphics.get( ogg.tailId() );
        
        initLineGroup( head );
        initLineGroup( tail );
        
        if( head != null ) head.incoming.add( this );
        if( tail != null ) tail.outgoing.add( this );
    }
    
    private void initLineGroup( Graphic target ) {
        if( target == null ) return;
        if( ! (target instanceof Line )) return;
        
        Line line = (Line) target;
        
        if( lineGroup == null ) {
            lineGroup = new HashSet<Line>();
            lineGroup.add( this );
        }
        
        lineGroup.add( line );
        
        if( line.lineGroup != null ) {
           if( line.lineGroup != lineGroup ) {
               lineGroup.addAll( line.lineGroup );
               line.lineGroup = lineGroup;
           }
        }
        else {
            line.lineGroup = lineGroup;
        }
    }
    
    @Override
    public String toString() {
        return "Line";
    }
}
