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

import org.epistem.graffle.OGGraphic;

/**
 * A shape that can connect
 *
 * @author nickmain
 */
public class ConnectorShape extends Shape implements Connector {
    
    public Graphic head;
    public Graphic tail;
    
    /** @see org.epistem.diagram.model.Connector#getHead() */
    public Graphic getHead() {
        return head;
    }
    
    /** @see org.epistem.diagram.model.Connector#getTail() */
    public Graphic getTail() {
        return tail;
    }
    
    /**
     * Accept a visitor
     */
    public void accept( DiagramVisitor visitor ) {
       visitor.visitConnectorShape( this );
    }
    
    @Override
    public boolean isSolid() {
        return isSolid;
    }
    
    ConnectorShape( OGGraphic ogg, GraphicContainer parent, Page page ) {
        super( ogg, parent, page );
    }
    
    /** @see org.epistem.diagram.model.Graphic#init() */
    @Override
    void init() {
        super.init();
        
        head = page.graphics.get( ogg.headId() );
        tail = page.graphics.get( ogg.tailId() );
        
        if( head != null ) head.incoming.add( this );
        if( tail != null ) tail.outgoing.add( this );
    }

    @Override
    public String toString() {
        return "Connector '" + text + "'";
    }
}
