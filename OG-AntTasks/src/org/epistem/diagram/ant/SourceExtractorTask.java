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
package org.epistem.diagram.ant;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.epistem.diagram.literate.SourceFileExtractor;
import org.epistem.diagram.model.Diagram;
import org.epistem.graffle.OmniGraffleDoc;

/**
 * Ant task to extract source code from a diagram and write to target files.
 *
 * @author nickmain
 */
public class SourceExtractorTask extends Task {
   
    private File ogFile;
    private File baseDir;
    
    /**
     * Set the diagram to read
     */
    public void setDiagram( File file ) {
        this.ogFile = file;
    }

    /**
     * Set the optional base dir for the generated source files
     */
    public void setDir( File dir ) {
        baseDir = dir;
    }
    
    @Override
    public void execute() throws BuildException {

        if( ogFile == null ) throw new BuildException( "diagram file is required" );
        baseDir = ogFile.getParentFile();
        
        OmniGraffleDoc doc;
        try {
            doc = new OmniGraffleDoc( ogFile );
        }
        catch( Exception e ) {
            throw new BuildException( e );
        }
        
        
        Diagram diagram;
        try {
            diagram = new Diagram( doc );    
        }
        catch( Exception e ) {
            e.printStackTrace();
            throw new BuildException( e );
        }
      
        log( "Processing diagram " + ogFile + " ..." );

        SourceFileExtractor extractor = new SourceFileExtractor() {
            @Override public void log( String message ) { SourceExtractorTask.this.log( "  " + message ); }           
        };
        
        diagram.accept( extractor );
        
        extractor.writeFiles( baseDir );

        log( "... done" );
    }
}

