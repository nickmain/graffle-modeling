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

import java.util.*;

import org.epistem.graffle.OGGraphic;
import org.epistem.graffle.OGLayer;
import org.epistem.graffle.OGSheet;

/**
 * A page within a document
 *
 * @author nickmain
 */
public class Page implements GraphicContainer {

    public final Map<String,Object> userData = new HashMap<String, Object>();
    public final String title;
    public final Collection<Graphic> rootGraphics = new HashSet<Graphic>();
    public final Metadata metadata;
    public final Diagram  diagram;
    public final Collection<Layer> layers = new HashSet<Layer>();
    
    /*pkg*/ final Map<OGLayer, Layer> ogLayers = new HashMap<OGLayer, Layer>();
    
    /** @see java.lang.Iterable#iterator() */
    public Iterator<Graphic> iterator() {
        return rootGraphics.iterator();
    }
    
    /**
     * Accept a visitor
     */
    public void accept( DiagramVisitor visitor ) {
        DiagramVisitor rootVisitor = visitor.visitPageStart( this );
        
        if( rootVisitor != null ) {
            for( Graphic g : rootGraphics ) g.accept( rootVisitor );
        }
        
        visitor.visitPageEnd( this );
    }
    
    Map<Integer, Graphic> graphics;
    
    Page( OGSheet sheet, Diagram diagram ) {
        
        title = sheet.title();
        this.diagram = diagram;
        metadata = new Metadata( sheet.notes(), sheet.userProperties() );
        
        for( OGLayer ogLayer : sheet.layers ) {
            Layer layer = new Layer( ogLayer.name(), ogLayer.visible() );
            ogLayers.put( ogLayer, layer );
        }
        
        graphics = new HashMap<Integer, Graphic>();
        for( OGGraphic g : sheet.graphics()) {
            rootGraphics.add( Graphic.make( g, this, this ) );            
        }
        
        for( Graphic g : graphics.values() ) {
            g.init();
            g.ogg = null;
        }
        
        graphics = null;
        
        for( Graphic g : rootGraphics ) {
            if( ! (g instanceof Shape )) continue;
            Shape s = (Shape) g;
            
            for( Graphic g2 : rootGraphics ) {
                if( g == g2 ) continue;                
                if( ! (g2 instanceof Shape )) continue;
                Shape s2 = (Shape) g2;
                
                if( s.bounds.intersects( s2.bounds ) ) {
                    s.intersectingShapes.add( s2 );
                    s2.intersectingShapes.add( s );
                }
                
                if( s.bounds.contains( s2.bounds ) ) {
                    s.containedShapes.add( s2 );
                    s2.containingShapes.add( s );
                }

                if( s2.bounds.contains( s.bounds ) ) {
                    s2.containedShapes.add( s );
                    s.containingShapes.add( s2 );
                }
            }
        }        
    }       
}
