/*--------------------------------------------------------------------------------
  Copyright (c) 2011, David N. Main
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
package org.epistem.diagram.literate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.apache.tools.ant.BuildException;
import org.epistem.diagram.model.DiagramVisitor;
import org.epistem.diagram.model.Page;
import org.epistem.diagram.model.Shape;

/**
 * Diagram visitor that extracts source files from a diagram to implement
 * a form of Literate Programming
 *
 * @author nickmain
 */
public class SourceFileExtractor extends DiagramVisitor.Impl {

    private static final String CODE = "code";
    
    private final Map<String,Collection<String>> fileSrcs = new HashMap<String, Collection<String>>();
    private final Map<String, List<Shape>> pageShapes = new HashMap<String, List<Shape>>();

    /**
     * After the diagram visit, get the extracted source
     * @return source, keyed by file name
     */
    public Map<String, String> getFileSources() {
        Map<String, String> srcs = new HashMap<String, String>();
        
        for( String fileName : fileSrcs.keySet() ) {
            StringBuilder buff = new StringBuilder();

            for( String src : fileSrcs.get( fileName ) ) {
                buff.append( src );
                buff.append( "\n" );
            }
            
            srcs.put( fileName, buff.toString() );
        }
        
        return srcs;
    }
    
    /**
     * After the diagram visit, write out the source files
     * 
     * @param baseDir the base dir for the files
     */
    public void writeFiles( File baseDir ) {
        log( "emitting source files:" );
        
        for( String fileName : fileSrcs.keySet() ) {
            File srcFile = new File( baseDir, fileName );
            
            try {
                log( "  - writing " + srcFile.getCanonicalPath() );
            } 
            catch( IOException e ) { e.printStackTrace(); }
            
            try {
                FileWriter writer = new FileWriter( srcFile );
                
                for( String src : fileSrcs.get( fileName ) ) {
                    writer.write( src );
                    writer.write( "\n" );
                }

                writer.close();
            } 
            catch( IOException e ) {
                throw new BuildException( e );
            }                    
        }                        
    }
    
    /**
     * Override to emit log messages
     */
    public void log( String message ) {
        //nothing
    }

    @Override
    public void visitPageEnd( Page page ) {

        //sort shapes according to vertical position
        for( String fileName : pageShapes.keySet() ) {
            List<Shape> shapes = pageShapes.get( fileName );
            
            Collections.sort( shapes, new Comparator<Shape>() {
                @Override
                public int compare( Shape a, Shape b ) {
                    double ay = a.bounds.getY();
                    double by = b.bounds.getY();
                    
                    if( ay == by ) {
                        double ax = a.bounds.getX();
                        double bx = b.bounds.getX();
                        
                        if( ax == bx ) return 0;                                
                        if( ax <  bx ) return -1;
                        return 1;                                
                    }
                    
                    if( ay < by ) return -1;
                    return 1;
                }                        
            });
            
            
            //append source to global map
            Collection<String> fileSrc = fileSrcs.get( fileName );
            if( fileSrc == null ) fileSrcs.put( fileName, fileSrc = new ArrayList<String>() );
            
            for( Shape s : shapes ) {
                fileSrc.add( s.text );
            }
        }
     }

    @Override
    public DiagramVisitor visitPageStart( Page page ) {

        log( "- page: " + page.title );
        
        pageShapes.clear();
        
        return super.visitPageStart( page );
    }

    @Override
    public void visitShape( Shape shape ) {
        
        //gather shapes tagged as code - keyed by filename based on layer name
        if( CODE.equalsIgnoreCase( shape.metadata.notes ) ) {
            String fileName = shape.layer.name;
            List<Shape> shapes = pageShapes.get( fileName );                    
            if( shapes == null ) pageShapes.put( fileName, shapes = new ArrayList<Shape>() );
            
            shapes.add( shape );
        }                
    }
}
