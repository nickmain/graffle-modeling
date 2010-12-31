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
package org.epistem.diagram.model.util;

import java.io.File;
import java.io.FileWriter;

import org.epistem.diagram.model.*;
import org.epistem.graffle.OmniGraffleDoc;
import org.epistem.io.IndentingPrintWriter;
import org.epistem.util.GraphvizWriter;

/**
 * Diagram visitor that dumps out a Graphviz representation
 *
 * @author nickmain
 */
public class GraphvizDumper implements DiagramVisitor {

    private final GraphvizWriter gv;
    
    public GraphvizDumper( IndentingPrintWriter out ) {
        gv = new GraphvizWriter( out );
    }
    
    public void visitConnectorShape( ConnectorShape shape ) {
        gv.declareNode( "" + shape.hashCode(), "" + shape.text, "#cccccc" );

        if( shape.head != null ) gv.arc( "" + shape.hashCode(), "" + shape.head.hashCode(), "head" );
        if( shape.tail != null ) gv.arc( "" + shape.hashCode(), "" + shape.tail.hashCode(), "tail" );
    }

    public void visitDiagramEnd( Diagram diagram ) {
        gv.endDiGraph();
        gv.close();
    }

    public DiagramVisitor visitDiagramStart( Diagram diagram ) {
        gv.startDiGraph( "Diagram" );
        
        gv.declareNode( "diagram", diagram.file.getName(), "#ffffcc" );
        
        return this;
    }

    public void visitGroupEnd( Group group ) {
        for( Graphic g : group.children ) {
            gv.arc( "" + group.hashCode(), "" + g.hashCode(), "child" );
        }
    }

    public DiagramVisitor visitGroupStart( Group group ) {
        gv.declareNode( "" + group.hashCode(), "Group\n" + group.text, "#ffccff" );
        return this;
    }

    public void visitLineEnd( Line line ) {
        for( Graphic g : line.labels ) {
            gv.arc( "" + line.hashCode(), "" + g.hashCode(), "label" );
        }
    }

    public DiagramVisitor visitLineStart( Line line ) {
        gv.declareNode( "" + line.hashCode(), "Line", "#ccccff" );

        if( line.head != null ) gv.arc( "" + line.hashCode(), "" + line.head.hashCode(), "head" );
        if( line.tail != null ) gv.arc( "" + line.hashCode(), "" + line.tail.hashCode(), "tail" );
        
        return this;
    }

    public void visitPageEnd( Page page ) {
        for( Graphic g : page.rootGraphics ) {
            gv.arc( "" + page.hashCode(), "" + g.hashCode() );
        }
    }

    public DiagramVisitor visitPageStart( Page page ) {
        
        gv.declareNode( "" + page.hashCode(), page.title, "#ccffcc" );
        gv.arc( "diagram", "" + page.hashCode(), "page" );
        
        return this;
    }

    public void visitShape( Shape shape ) {
        gv.declareNode( "" + shape.hashCode(), "" + shape.text, "#ccffff" );
    }

    public void visitTableEnd( Table table ) {
        for( Graphic g : table.cells ) {
            gv.arc( "" + table.hashCode(), "" + g.hashCode(), "cell" );
        }
    }

    public DiagramVisitor visitTableStart( Table table ) {
        gv.declareNode( "" + table.hashCode(), "Table", "#cccc88" );
        return this;
    }

    public static void main( String[] args ) throws Exception {
        FileWriter out = new FileWriter( "target/test.dot" );
        OmniGraffleDoc doc = new OmniGraffleDoc( new File( "test-diagrams/test.graffle" ) );
        
        GraphvizDumper gv = new GraphvizDumper( new IndentingPrintWriter( out ) );
        Diagram diag = new Diagram( doc );
        diag.accept( gv );
    }
}
