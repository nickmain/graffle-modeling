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
package org.epistem.util;

import org.epistem.io.IndentingPrintWriter;

/**
 * Writer for the Graphviz DOT format
 * 
 * @author nickmain
 */
public class GraphvizWriter {

	protected final IndentingPrintWriter out;

	/**
	 * @param out the output target
	 */
	public GraphvizWriter( IndentingPrintWriter out ) {
		this.out = out;
	}

	/**
	 * Start the digraph
	 */
	public void startDiGraph( String name ) {
		out.println( "digraph " + name + " {" );
		out.indent();
	}
	
	/**
	 * End the digraph
	 */
	public void endDiGraph() {
	    out.unindent();
		out.println( "}" );		
	}
	
	/**
	 * Start a cluster definition
	 * 
	 * @param name the cluster name
	 * @param label the cluster label
	 */
	public void startCluster( String name, String label ) {
        out.println( "subgraph \"cluster_" + name + "\" { label=\"" + label + "\";" );	    
        out.indent();
	}
	
	/**
     * Start a cluster definition with a fill color
     * 
     * @param name the cluster name
     * @param label the cluster label
     * @param color the fill color
     */
    public void startCluster( String name, String label, String color ) {
        out.println( "subgraph \"cluster_" + name + "\" { label=\"" + label + "\"; fillcolor=\"" + color + "\"; style=filled;" );      
        out.indent();
    }
    
	
	/**
	 * Declare a node label
	 * 
	 * @param name the node name
	 * @param label the node label
	 */
	public void declareNode( String name, String label, String color ) {
        out.println( "\"" + name + "\" [label=\"" + label + "\", fillcolor=\"" + color + "\", style=filled];" );        
	}
	
	/**
     * Declare an invisible node
     * 
     * @param name the node name
     */
    public void invisibleNode( String name ) {
        out.println( "\"" + name + "\" [width=.01,height=.01,label=\"\",style=invis];" );        
    }
    
	/**
	 * End a cluster definition
	 */
	public void endCluster() {
	    out.unindent();
	    out.println( "}" );
	}
	
	/**
     * Start a new ranking
     */
    public void startRank(  ) {
        out.println( "{rank=same;" );      
    }
    
    /**
     * End a ranking
     */
    public void endRank() {
        out.println( "}" );
    }
	
	/**
	 * Add an arc
	 * 
	 * @param from the from-node name
	 * @param to the to-node name
	 */
	public void arc( String from, String to ) {
		out.println( "\"" + from + "\" -> \"" + to + "\";" );		
	}

	/**
     * Add an arc that could be between clusters
     * 
     * @param from the from-node name
     * @param to the to-node name
     * @param tailCluster the cluster at the arc tail - may be null
     * @param headCluster the cluster at the arc head - may be null
     * 
     */
    public void arc( String from, String to, String tailCluster, String headCluster ) {
        arc( from, to, tailCluster, headCluster, null );
    }

    /**
     * Add an arc that could be between clusters
     * 
     * @param from the from-node name
     * @param to the to-node name
     * @param tailCluster the cluster at the arc tail - may be null
     * @param headCluster the cluster at the arc head - may be null
     * @param color the arc color - may be null
     */
    public void arc( String from, String to, String tailCluster, String headCluster, String color ) {
        out.print( "\"" + from + "\" -> \"" + to + "\"" );
        
        if( tailCluster != null || headCluster != null ) {
            out.print( " [" );
            
            if( tailCluster != null ) {
                out.print( "ltail=\"" );
                out.print( tailCluster );
                out.print( "\"" );
                
                if( headCluster != null ) {
                    out.print( ", " );
                }
            }

            if( headCluster != null ) {
                out.print( "lhead=\"" );
                out.print( headCluster );
                out.print( "\"" );
            }

            if( color != null ) {
                if( headCluster != null || tailCluster != null ) {
                    out.print( ", " );
                }
                
                out.print( "color=\"" );
                out.print( color );
                out.print( "\"" );
            }
            
            out.print( "]" );
        }
        
        out.println( ";" );
    }

	
	/**
     * Add an arc to a record cell
     * 
     * @param from the from-node name
     * @param toRecord the to-node name
     * @param cell the target cell
     */
    public void arcToRecord( String from, String toRecord, String cell ) {
        out.println( "\"" + from + "\" -> \"" + toRecord + "\":\"" + cell + "\";" );       
    }

    /**
     * Add between two records
     */
    public void recordArc( String from, String fromCell, String to, String toCell ) {
        out.println( "\"" + from + "\":\"" + fromCell + "\" -> \""  + to + "\":\"" + toCell + "\";" );       
    }

    
    /**
     * Set the record shape for records
     */
    public void useRecordShapes() {
        out.println( " rankdir=LR;\n  node [shape=record];\n  compound=true;\n  concentrate=true;" );        
    }
     
    /**
     * Start a record
     */
    public void startRecord( String name ) {
        out.print( "\"" + name + "\" [ label=\"" );    
    }
    
    /**
     * The first cell in a record
     */
    public void firstCell( String label ) {
        out.print( "<first> " + label );
    }
    
    /**
     * A subsequent cell in a record
     */
    public void cell( String name, String label ) {
        out.print( "|<" + name + "> " + label );
    }
    
    /**
     * End a record
     */
    public void endRecord( String fillColor ) {
        out.println( "\", fillcolor=\"" + fillColor + "\", style=filled];" );
    }
    
    /**
     * Add an arc with a label
     * 
     * @param from the from-node name
     * @param to the to-node name
     * @param label the arc label
     */
    public void arc( String from, String to, String label ) {
        out.println( "\"" + from + "\" -> \"" + to + "\" [label=\"" + label + "\"];" );        
    }
    
    /**
     * Close the underlying writer
     */
    public final void close() {
        out.flush();
        out.close();
    }
}
