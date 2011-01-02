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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.epistem.graffle.OGGraphic;
import org.epistem.graffle.OGLayer;

/**
 * Base graphic
 *
 * @author nickmain
 */
public abstract class Graphic {

    public final Map<String,Object> userData = new HashMap<String, Object>();
    public final Metadata metadata;
    public final boolean  isSolid;
    public final Page     page;
    public final Collection<Connector> incoming = new HashSet<Connector>();
    public final Collection<Connector> outgoing = new HashSet<Connector>();
    public GraphicContainer parent;
    public final double x, y;
    public final Layer layer;
    
    
    protected OGGraphic ogg;
    
    abstract void init();
    
    public abstract void accept( DiagramVisitor visitor );
    
    Graphic( OGGraphic ogg, GraphicContainer parent, Page page ) {

        this.ogg = ogg;
        this.page = page;
        this.parent = parent;
        metadata = new Metadata( ogg.notes(), ogg.userProperties() );
        isSolid = ogg.strokePattern() == 0;
        
        page.graphics.put( ogg.id(), this );
        
        Rectangle2D bounds = ogg.bounds();
        x = bounds.getCenterX();
        y = bounds.getCenterY();
        
        OGLayer ogLayer = ogg.layer();
        if( ogLayer != null ) {
            layer = page.ogLayers.get( ogLayer );             
            layer.graphics.add( this );
        }
        else layer = null;        
    }
    
    static Graphic make( OGGraphic ogg, GraphicContainer parent, Page page ) {
        switch( ogg.graphicClass() ) {
            case Group:        return new Group( ogg, parent, page );
            case TableGroup:   return new Table( ogg, parent, page );
            case LineGraphic:  return new Line ( ogg, parent, page );
            case ShapedGraphic:     
                if( ogg.tailId() != 0 || ogg.headId() != 0 ) return new ConnectorShape( ogg, parent, page );
                return new Shape( ogg, parent, page );
                
            default: throw new RuntimeException( "UNREACHABLE CODE" );
        }
    }
    
    @Override
    abstract public String toString();
    
    /**
     * Get a string representing the location of the graphic
     */
    public String toLocationString() {
        return  "'" + page.title + "'(" + x + "," + y + ")";
    }
}
