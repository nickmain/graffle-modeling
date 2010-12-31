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

/**
 * Diagram visitor
 *
 * @author nickmain
 */
public interface DiagramVisitor {

    /**
     * Start a diagram visit
     * 
     * @return visitor for the pages - null to skip
     */
    public DiagramVisitor visitDiagramStart( Diagram diagram );

    /**
     * End of diagram visit
     */
    public void visitDiagramEnd( Diagram diagram );

    /**
     * Start of a page visit
     * 
     * @return visitor for the root graphics - null to skip
     */
    public DiagramVisitor visitPageStart( Page page );
    
    /**
     * End of page visit
     */
    public void visitPageEnd( Page page );
    
    /**
     * Start of a group visit
     * 
     * @return visitor for the children - null to skip
     */
    public DiagramVisitor visitGroupStart( Group group );
    
    /**
     * End of group visit
     */
    public void visitGroupEnd( Group group );
    
    /**
     * Visit a shape
     */
    public void visitShape( Shape shape );
    
    /**
     * Visit a connector shape
     */
    public void visitConnectorShape( ConnectorShape shape );
    
    /**
     * Start a line visit
     * 
     * @return visitor for the labels - null to skip
     */
    public DiagramVisitor visitLineStart( Line line );
    
    /**
     * End of a line visit
     */
    public void visitLineEnd( Line line );
    
    /**
     * Start of a table visit
     * 
     * @return visitor for the cells - null to skip
     */
    public DiagramVisitor visitTableStart( Table table );

    /**
     * End of a table visit
     */
    public void visitTableEnd( Table table );
    
    /**
     * Convenience implementation that visits all children and labels
     */
    public static class Impl implements DiagramVisitor {
        public void visitConnectorShape( ConnectorShape shape ) {}
        public void visitDiagramEnd( Diagram diagram ) {}
        public DiagramVisitor visitDiagramStart( Diagram diagram ) { return this; }
        public void visitGroupEnd( Group group ) {}
        public DiagramVisitor visitGroupStart( Group group ) { return this; }
        public void visitLineEnd( Line line ) {}
        public DiagramVisitor visitLineStart( Line line ) { return this; }
        public void visitPageEnd( Page page ) {}
        public DiagramVisitor visitPageStart( Page page ) { return this; }
        public void visitShape( Shape shape ) {}
        public void visitTableEnd( Table table ) {}
        public DiagramVisitor visitTableStart( Table table ) { return this; }        
    }
    
    /**
     * Convenience implementation that does not visit children or labels but
     * does visit all pages
     */
    public static class ShallowImpl implements DiagramVisitor {
        public void visitConnectorShape( ConnectorShape shape ) {}
        public void visitDiagramEnd( Diagram diagram ) {}
        public DiagramVisitor visitDiagramStart( Diagram diagram ) { return this; }
        public void visitGroupEnd( Group group ) {}
        public DiagramVisitor visitGroupStart( Group group ) { return null; }
        public void visitLineEnd( Line line ) {}
        public DiagramVisitor visitLineStart( Line line ) { return null; }
        public void visitPageEnd( Page page ) {}
        public DiagramVisitor visitPageStart( Page page ) { return this; }
        public void visitShape( Shape shape ) {}
        public void visitTableEnd( Table table ) {}
        public DiagramVisitor visitTableStart( Table table ) { return null; }        
    }
}
