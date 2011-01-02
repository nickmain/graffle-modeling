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
import org.epistem.diagram.model.Diagram;
import org.epistem.diagram.owl.OWLEmitter;
import org.epistem.graffle.OmniGraffleDoc;

/**
 * ANT task to generate an ontology from an Omnigraffle diagram
 *
 * @author nickmain
 */
public class OWLEmitterAntTask extends Task {

    private File diagram;
    private File ontology;
    /**
     * @param diagram the diagram to set
     */
    public void setDiagram( File diagram ) {
        this.diagram = diagram;
    }
    /**
     * @param ontology the ontology to set
     */
    public void setOntology( File ontology ) {
        this.ontology = ontology;
    }
        
    /** @see org.apache.tools.ant.Task#execute() */
    @Override
    public void execute() throws BuildException {
        if( diagram  == null ) throw new BuildException( "Missing diagram" );
        if( ontology == null ) throw new BuildException( "Missing ontology" );

        try {
            log( "Loading Omnigraffle diagram from " + diagram.getName() + "  ..." );
            OWLEmitter emitter = new OWLEmitter( new Diagram( new OmniGraffleDoc( diagram )));
            
            log( "Generating OWL2 ontology ..." );
            emitter.processDiagram();
            
            log( "Saving ontology to " + ontology.getName() + " ..." );
            emitter.saveOntology( ontology );
            
            log( "... Done." );
        }
        catch( Exception e ) {
            throw new BuildException( e );
        }
    }
}
