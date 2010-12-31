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

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.text.DefaultStyledDocument;

import org.epistem.graffle.OGGraphic;

/**
 * A shape
 *
 * @author nickmain
 */
public class Shape extends Graphic {
    
    public final String text;
    public final DefaultStyledDocument richText;
    public final Rectangle2D bounds;
    public final Collection<Shape> containedShapes    = new HashSet<Shape>();
    public final Collection<Shape> intersectingShapes = new HashSet<Shape>();
    public final Collection<Shape> containingShapes   = new HashSet<Shape>();
    
    /**
     * Accept a visitor
     */
    public void accept( DiagramVisitor visitor ) {
        visitor.visitShape( this );
    }
    
    Shape( OGGraphic ogg, GraphicContainer parent, Page page ) {
        super( ogg, parent, page );
        
        OGGraphic g = ogg;
        
        if( ogg.isSubgraph() ) {
            List<OGGraphic> kids = ogg.graphics();
            g = kids.get( kids.size() - 1 );
        }
        
        this.text     = g.text();
        this.richText = g.styledText();
        this.bounds   = g.bounds();
    }

    /** @see org.epistem.diagram.model.Graphic#init() */
    @Override
    void init() {
        if( ogg.labelLineId() != 0 ) {
            Line line = (Line) page.graphics.get( ogg.labelLineId() );
            line.labelMap.put( ogg.labelPosition(), this );
            
            parent = line;            
            page.rootGraphics.remove( this );
        }        
    }
    
    @Override
    public String toString() {
        return "Shape '" + text + "'";
    }
}
