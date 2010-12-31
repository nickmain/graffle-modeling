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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.epistem.graffle.OGGraphic;

/**
 * A table group
 *
 * @author nickmain
 */
public class Table extends Graphic implements GraphicContainer {

    public final Collection<Shape> cells = new HashSet<Shape>();
    
    /** Table[row][column] */
    public final Shape[][] table;
    
    /** @see java.lang.Iterable#iterator() */
    public Iterator<Graphic> iterator() {
        return new HashSet<Graphic>( cells ).iterator();
    }

    /** Get the row count */
    public int rowCount() { return table.length; }

    /** Get the column count */
    public int colCount() { return table[0].length; }
    
    /**
     * Accept a visitor
     */
    public void accept( DiagramVisitor visitor ) {
        DiagramVisitor cellVisitor = visitor.visitTableStart( this );
        
        if( cellVisitor != null ) {
            for( Graphic g : cells ) g.accept( cellVisitor );
        }
        
        visitor.visitTableEnd( this );
    }
    
    Table( OGGraphic ogg, GraphicContainer parent, Page page ) {
        super( ogg, parent, page );
        
        OGGraphic[][] oggTable = ogg.table();
        int colCount = oggTable.length;
        int rowCount = oggTable[0].length;
        
        table = new Shape[ rowCount ][ colCount ];
        
        for( int row = 0; row < rowCount; row++ ) {
            for( int col = 0; col < colCount; col++ ) {
                Shape s = new Shape( oggTable[ col ][ row ], this, page );
                table[ row ][ col ] = s;
                cells.add( s );
            }
        }
    }

    /** @see org.epistem.diagram.model.Graphic#init() */
    @Override
    void init() {
        //nothing
    }
    
    @Override
    public String toString() {
        return "Table";
    }
}
