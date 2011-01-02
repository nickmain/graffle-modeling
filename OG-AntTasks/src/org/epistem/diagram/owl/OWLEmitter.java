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
package org.epistem.diagram.owl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import org.epistem.diagram.model.*;
import org.epistem.graffle.OmniGraffleDoc;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.DefaultOntologyFormat;
import org.semanticweb.owlapi.io.OWLOntologyDocumentTarget;
import org.semanticweb.owlapi.io.StreamDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLFacet;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;

/**
 * Generates OWL from a diagram
 *
 * @author nickmain
 */
public class OWLEmitter {

    private final Diagram diagram;
    
    private final Map<String,String> namespaces = new HashMap<String, String>();
    private final String defaultNamespace;
    
    private final OWLOntologyManager manager;
    private final OWLDataFactory     factory;
    private final OWLOntology        ontology;
    
    //graphics by note
    private final Map<GraphicNote,Collection<Graphic>> graphics = new HashMap<GraphicNote,Collection<Graphic>>();
    
    /**
     * Load an ontology from a OG file using the given manager
     * @param file the OG file
     * @param manager the manager to use
     * @return the loaded ontology
     */
    public static OWLOntology loadOntology( File file, OWLOntologyManager manager ) {
        Diagram diagram;
        try {
            diagram = new Diagram( new OmniGraffleDoc( file ) );
            OWLEmitter emitter = new OWLEmitter( diagram, manager );
            emitter.processDiagram();
            return emitter.getOntology();
        }
        catch( Exception e ) {
            throw new RuntimeException( e );
        }
    }
    
    private OWLEmitter( Diagram diagram, OWLOntologyManager manager ) {
        this.manager = manager;
        this.diagram = diagram;
        factory = manager.getOWLDataFactory();
        
        addCommonNamespaces();
        defaultNamespace = findNamespaceDeclarations();
        
        try {
            ontology = manager.createOntology( IRI.create( defaultNamespace ));
        } 
        catch( OWLOntologyCreationException e ) {
            throw new RuntimeException( e );
        }
    }
    
    /**
     * @param diagram the omnigraffle diagram to read
     */
    public OWLEmitter( Diagram diagram ) {
        this( diagram, OWLManager.createOWLOntologyManager() );
    }
    
    /**
     * Get the ontology being built
     */
    public OWLOntology getOntology() {
        return ontology;
    }
    
    /**
     * Save the ontology
     */
    public void saveOntology( OWLOntologyFormat format, OWLOntologyDocumentTarget target ) {
        try {
            if( format instanceof PrefixOWLOntologyFormat ) {
                PrefixOWLOntologyFormat pof = (PrefixOWLOntologyFormat) format;
                
                for( String prefix : namespaces.keySet() ) {
                    pof.setPrefix( prefix, namespaces.get( prefix ) );
                }
            }
            
            manager.saveOntology( ontology, format, target );
        }
        catch( OWLOntologyStorageException e ) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Save the ontology as RDF in the given file
     */
    public void saveOntology( File file ) {
        FileOutputStream out;
        try {
            out = new FileOutputStream( file );
            saveOntology( new DefaultOntologyFormat(), new StreamDocumentTarget( out ) );
            out.close();
        }
        catch( IOException e ) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Process the diagram and build the ontology
     */
    public void processDiagram() {
        graphics.clear();
        diagram.accept( noteVisitor );
        process();
    }
    
    /** Process just a single page */
    /*pkg*/ void processPage( Page page ) {
        //put the page graphics (only) in the map
        graphics.clear();        
        page.accept( noteVisitor );

        process();
    }

    /**
     * Process the gathered graphics
     */
    private void process() {
        processImports();
        processClassAxioms();
        processIndividuals();
        processObjectProperties();
        processDataProperties();
        processDatatypes();
        processRules();
        processAnnotations();
    }

    /** Add SWRL rules */
    private void processRules() {
        for( Connector c: connectors( GraphicNote.rule ) ) {
            String body = shape( c.getTail() ).text;
            String head = shape( c.getHead() ).text;
            
            SWRLRule rule = factory.getSWRLRule( 
                                        parseAtoms( body, c.getTail() ), 
                                        parseAtoms( head, c.getHead() ) );
            addAxiom( rule );
        }

    }

    //parse a set of atoms
    private Set<SWRLAtom> parseAtoms( String part, Graphic g ) {
        Set<SWRLAtom> atoms = new HashSet<SWRLAtom>();
        String[] atomStrings = part.split( "\\n" );  
        
        for( String atomString : atomStrings ) {        
            String[] parts = atomString.split( "\\(" );
            if( parts.length != 2 ) throw error( "Rule atom must include an open paren: " + atomString, g );
            
            String pred = parts[0].trim();
            String args = parts[1].trim();
            
            if( ! args.endsWith( ")" ) ) throw error( "Rule atom must include a close paren: " + atomString, g );
            args = args.substring( 0, args.length() - 1 );
            String[] argStrings = args.split( "," );
            
            //gather args
            List<SWRLArgument> swrlArgs = new ArrayList<SWRLArgument>();
            for( String argString : argStrings ) {
                argString = argString.trim();
                if( argString.startsWith( "?" ) ) {
                    swrlArgs.add( factory.getSWRLVariable( IRI.create( defaultNamespace + "#swrlVar_" + argString.substring( 1 ) ) ) );
                }
                else if( Character.isJavaIdentifierStart( argString.charAt( 0 ) )) {
                    swrlArgs.add( factory.getSWRLIndividualArgument( 
                                      factory.getOWLNamedIndividual( 
                                          IRI.create( uriFromString( argString ) ))));
                }
                else if( Character.isDigit( argString.charAt( 0 ) ) ) {
                    swrlArgs.add( factory.getSWRLLiteralArgument( 
                                      factory.getOWLLiteral( Integer.parseInt( argString ) )));
                }
                else if( argString.startsWith( "\"" ) ) {
                    int closingQuotes = argString.lastIndexOf( "\"" );
                    if( closingQuotes == 0 ) throw error( "No closing quotes in string", g );
                    String string = argString.substring( 1, closingQuotes );
                    
                    int carets = argString.lastIndexOf( "^^" );
                    if( carets > closingQuotes ) {
                        String datatype = argString.substring( carets + 2 ).trim();
                        swrlArgs.add( factory.getSWRLLiteralArgument( 
                                factory.getOWLLiteral( string, 
                                    factory.getOWLDatatype( IRI.create( uriFromString( datatype ) ) ) )));                        
                    }
                    else {
                        swrlArgs.add( factory.getSWRLLiteralArgument( factory.getOWLLiteral( string )));
                    }                    
                }
                else throw error( "invalid arg: " + argString, g );
            }
            
            if( "SameAs".equals( pred ) ) {                
                atoms.add( factory.getSWRLSameIndividualAtom( 
                                       (SWRLIArgument) swrlArgs.get( 0 ), 
                                       (SWRLIArgument) swrlArgs.get( 1 ) ) );
            }
            else if( "DifferentFrom".equals( pred ) ) {
                atoms.add( factory.getSWRLDifferentIndividualsAtom(  
                                       (SWRLIArgument) swrlArgs.get( 0 ), 
                                       (SWRLIArgument) swrlArgs.get( 1 ) ) );                
            }
            else if( pred.startsWith( "swrlb:" ) ) {
                List<SWRLDArgument> dargs = new ArrayList<SWRLDArgument>();                
                for( SWRLArgument a : swrlArgs ) dargs.add( (SWRLDArgument) a );
                
                atoms.add( factory.getSWRLBuiltInAtom( IRI.create( uriFromString( pred )), dargs ));
            }
            else {
                Set<OWLEntity> ents = ontology.getEntitiesInSignature( IRI.create( uriFromString( pred )), true );
                
                for( OWLEntity ent : ents ) {
                    if( ent instanceof OWLClass ) {
                        atoms.add( factory.getSWRLClassAtom(
                                       (OWLClass) ent,
                                       (SWRLIArgument) swrlArgs.get( 0 )));
                    }
                    else if( ent instanceof OWLDataProperty ) {
                        atoms.add( factory.getSWRLDataPropertyAtom( 
                                       (OWLDataProperty) ent,
                                       (SWRLIArgument) swrlArgs.get( 0 ),
                                       (SWRLDArgument) swrlArgs.get( 1 )));
                    }
                    else if( ent instanceof OWLDatatype ) {
                        atoms.add( factory.getSWRLDataRangeAtom(  
                                       (OWLDatatype) ent,
                                       (SWRLDArgument) swrlArgs.get( 0 )));                        
                    }
                    else if( ent instanceof OWLObjectProperty ) {
                        atoms.add( factory.getSWRLObjectPropertyAtom( 
                                       (OWLObjectProperty) ent,
                                       (SWRLIArgument) swrlArgs.get( 0 ),
                                       (SWRLIArgument) swrlArgs.get( 1 )));
                    }
                }
            }            
        }
        
        return atoms;
    }
    
    private void processAnnotations() {
        for( Graphic g : graphics( GraphicNote.annotation ) ) {
            if( g instanceof Table ) {
                Table t = (Table) g;
                
                if( g.outgoing.isEmpty() ) {
                    annotate( ontology.getOntologyID().getOntologyIRI(), t );
                }
                else {
                    for( Connector conn : g.outgoing ) {
                        IRI iri = IRI.create( uriFromShape( shape( conn.getHead() ) ) );
                        annotate( iri, t );
                    }
                }
            }
            else if( g instanceof Shape ) {
                for( Connector conn : g.outgoing ) {
                    IRI iri = IRI.create( uriFromShape( shape( conn.getHead() ) ) );
                    
                    OWLAnnotationProperty prop = factory.getOWLAnnotationProperty( IRI.create( "http://www.w3.org/2000/01/rdf-schema#comment" ) );
                    OWLLiteral            val  = factory.getOWLLiteral( shape(g).text );
                    OWLAnnotation         anno = factory.getOWLAnnotation( prop, val );
                    
                    addAxiom( factory.getOWLAnnotationAssertionAxiom( iri, anno ) );
                }                
            }
            else throw error( "Only table and shape annotations supported", g );            
        }
    }
    
    private void annotate( IRI subject, Table t ) {
        if( t.colCount() != 2 ) throw error( "Annotation table must have 2 columns", t );
        
        for( Shape[] row : t.table ) {
            IRI iri = IRI.create( uriFromShape( row[0] ) );
            String value = row[1].text;
            
            OWLAnnotationProperty prop = factory.getOWLAnnotationProperty( iri );
            OWLLiteral            val  = factory.getOWLLiteral( value );
            OWLAnnotation         anno = factory.getOWLAnnotation( prop, val );
            
            addAxiom( factory.getOWLAnnotationAssertionAxiom( subject, anno ) );
        }        
    }
    
    /** Add imports */
    private void processImports() {
        for( Connector c : connectors( GraphicNote.ontology_import ) ) {
            String importName = shape( c.getHead() ).text.trim();
            
            if( importName.contains( ".graffle" ) ) {
                File dir = diagram.file.getParentFile();
                OWLOntology importOntology = loadOntology( new File( dir, importName ), manager );
                OWLImportsDeclaration imp = factory.getOWLImportsDeclaration( importOntology.getOntologyID().getOntologyIRI() );                
                manager.applyChange( new AddImport( ontology, imp ) );
            }
            else throw error( "Only graffle imports are supported", c.getHead() );            
        }        
    }
    
    /** Find and declare datatypes */
    private void processDatatypes() {
        for( Connector c : connectors( GraphicNote.datatype_definition ) ) {
            OWLDatatype  datatype = factory.getOWLDatatype( IRI.create( uriFromShape( shape( c.getTail() ))));
            OWLDataRange range    = dataRangeFor( c.getHead() );
            
            addAxiom( factory.getOWLDeclarationAxiom( datatype ) );
            addAxiom( factory.getOWLDatatypeDefinitionAxiom( datatype, range ) );
        }        
    }
    
    /** Find and add object property axioms */
    private void processObjectProperties() {
        for( Shape s : shapes( GraphicNote.object_property ) ) {
            OWLObjectProperty prop = factory.getOWLObjectProperty( IRI.create( uriFromShape( s ) ) );
            if( ! prop.getIRI().toString().startsWith( defaultNamespace ) ) continue;

            addAxiom( factory.getOWLDeclarationAxiom( prop ) );
        }
        
        for( Shape s : shapes( GraphicNote.functional  )) addAxiom( factory.getOWLFunctionalObjectPropertyAxiom( objPropTarget( s ) ) );
        for( Shape s : shapes( GraphicNote.reflexive   )) addAxiom( factory.getOWLReflexiveObjectPropertyAxiom( objPropTarget( s ) ) );
        for( Shape s : shapes( GraphicNote.irreflexive )) addAxiom( factory.getOWLIrreflexiveObjectPropertyAxiom( objPropTarget( s ) ) );
        for( Shape s : shapes( GraphicNote.symmetric   )) addAxiom( factory.getOWLSymmetricObjectPropertyAxiom( objPropTarget( s ) ) );
        for( Shape s : shapes( GraphicNote.transitive  )) addAxiom( factory.getOWLTransitiveObjectPropertyAxiom( objPropTarget( s ) ) );
        for( Shape s : shapes( GraphicNote.asymmetric  )) addAxiom( factory.getOWLAsymmetricObjectPropertyAxiom( objPropTarget( s ) ) );
        for( Shape s : shapes( GraphicNote.inverse_functional )) addAxiom( factory.getOWLInverseFunctionalObjectPropertyAxiom( objPropTarget( s ) ) );
       
        for( Connector c : connectors( GraphicNote.inverse_properties )) {
            addAxiom( factory.getOWLInverseObjectPropertiesAxiom( 
                                  objPropExprFor( c.getTail() ), 
                                  objPropExprFor( c.getHead() ) ));
        }
        
        for( Connector c : connectors( GraphicNote.equivalent_object_properties )) {
            addAxiom( factory.getOWLEquivalentObjectPropertiesAxiom( 
                                  objPropExprFor( c.getTail() ), 
                                  objPropExprFor( c.getHead() ) ));
        }

        for( Connector c : connectors( GraphicNote.disjoint_object_properties )) {
            addAxiom( factory.getOWLDisjointObjectPropertiesAxiom( 
                                  objPropExprFor( c.getTail() ), 
                                  objPropExprFor( c.getHead() ) ));
        }
        
        for( Shape s : shapes( GraphicNote.pairwise_disjoint_object_properties ) ) {
            addAxiom( factory.getOWLDisjointObjectPropertiesAxiom( objPropTargets( s ) ) );                
        }
        
        for( Connector c : connectors( GraphicNote.sub_object_property_of )) {
            addAxiom( factory.getOWLSubObjectPropertyOfAxiom(  
                                  objPropExprFor( c.getTail() ), 
                                  objPropExprFor( c.getHead() ) ));
        }
        
        for( Connector c : connectors( GraphicNote.object_property_domain )) {
            addAxiom( factory.getOWLObjectPropertyDomainAxiom(   
                                  objPropExprFor( c.getTail() ), 
                                  classExpressionFor( c.getHead() ) ));
        }

        for( Connector c : connectors( GraphicNote.object_property_range )) {
            addAxiom( factory.getOWLObjectPropertyRangeAxiom(   
                                  objPropExprFor( c.getTail() ), 
                                  classExpressionFor( c.getHead() ) ));
        }

        for( Connector c : connectors( GraphicNote.sub_object_property_chain )) {
            
            List<OWLObjectPropertyExpression> props = new ArrayList<OWLObjectPropertyExpression>();
            
            props.add( objPropExprFor( c.getTail() ) );
            for( Shape s : ((Line) c).labels ) {
                props.add( objPropExprFor( s ) );                
            }
            
            addAxiom( factory.getOWLSubPropertyChainOfAxiom(    
                                  props, 
                                  objPropExprFor( c.getHead() ) ));
        }        
    }
    
    /** Find and process data properties */
    private void processDataProperties() {
        for( Shape s : shapes( GraphicNote.data_property ) ) {
            OWLDataProperty prop = factory.getOWLDataProperty( IRI.create( uriFromShape( s ) ) );
            if( ! prop.getIRI().toString().startsWith( defaultNamespace ) ) continue;

            addAxiom( factory.getOWLDeclarationAxiom( prop ) );
        }

        for( Shape s : shapes( GraphicNote.functional_data_property  )) {
            addAxiom( factory.getOWLFunctionalDataPropertyAxiom( dataPropTarget( s ) ) );
        }
        
        for( Connector c : connectors( GraphicNote.equivalent_data_properties )) {
            addAxiom( factory.getOWLEquivalentDataPropertiesAxiom( 
                                  dataPropExprFor( c.getTail() ), 
                                  dataPropExprFor( c.getHead() ) ));
        }
        
        for( Connector c : connectors( GraphicNote.data_property_domain )) {
            addAxiom( factory.getOWLDataPropertyDomainAxiom(   
                                  dataPropExprFor( c.getTail() ), 
                                  classExpressionFor( c.getHead() ) ));
        }

        for( Connector c : connectors( GraphicNote.data_property_range )) {
            addAxiom( factory.getOWLDataPropertyRangeAxiom(   
                                  dataPropExprFor( c.getTail() ), 
                                  dataRangeFor( c.getHead() ) ));
        }
        
        for( Connector c : connectors( GraphicNote.disjoint_data_properties )) {
            addAxiom( factory.getOWLDisjointDataPropertiesAxiom( 
                                  dataPropExprFor( c.getTail() ), 
                                  dataPropExprFor( c.getHead() ) ));
        }
        
        for( Shape s : shapes( GraphicNote.pairwise_disjoint_data_properties ) ) {
            addAxiom( factory.getOWLDisjointDataPropertiesAxiom( dataPropTargets( s ) ) );                
        }
        
        for( Connector c : connectors( GraphicNote.sub_data_property_of )) {
            addAxiom( factory.getOWLSubDataPropertyOfAxiom(  
                                  dataPropExprFor( c.getTail() ), 
                                  dataPropExprFor( c.getHead() ) ));
        }
        
    }
    
    /** Find and add individual axioms */
    private void processIndividuals() {
        for( Shape s : shapes( GraphicNote.individual ) ) {
            OWLNamedIndividual ind = individualFor( s );
            if( ! ind.getIRI().toString().startsWith( defaultNamespace ) ) continue;
            
            addAxiom( factory.getOWLDeclarationAxiom( ind  ) );
        }

        for( Connector conn : connectors( GraphicNote.same_individual ) ) {
            addAxiom( factory.getOWLSameIndividualAxiom( 
                                  individualFor( conn.getTail() ), 
                                  individualFor( conn.getHead() )) );
        }

        for( Connector conn : connectors( GraphicNote.different_individuals ) ) {
            addAxiom( factory.getOWLDifferentIndividualsAxiom( 
                                  individualFor( conn.getTail() ), 
                                  individualFor( conn.getHead() )) );
        }

        for( Connector conn : connectors( GraphicNote.class_assertion ) ) {
            addAxiom( factory.getOWLClassAssertionAxiom( 
                                  classExpressionFor( conn.getHead() ), 
                                  individualFor( conn.getTail() )) );
        }

        for( Connector conn : connectors( GraphicNote.prop_assertion ) ) {
            OWLPropertyExpression<?,?> propEx = propExprForConnector( conn );
            
            if( propEx instanceof OWLObjectPropertyExpression ){
                addAxiom( factory.getOWLObjectPropertyAssertionAxiom( 
                                      (OWLObjectPropertyExpression) propEx, 
                                      individualFor( conn.getTail() ), 
                                      individualFor( conn.getHead() ) ) );
            }
            else {
                addAxiom( factory.getOWLDataPropertyAssertionAxiom( 
                                      (OWLDataPropertyExpression) propEx, 
                                      individualFor( conn.getTail() ), 
                                      literalFor( conn.getHead() ) ) );
            }
        }

        for( Connector conn : connectors( GraphicNote.neg_prop_assertion ) ) {
            OWLPropertyExpression<?,?> propEx = propExprForConnector( conn );
            
            if( propEx instanceof OWLObjectPropertyExpression ){
                addAxiom( factory.getOWLNegativeObjectPropertyAssertionAxiom(
                                      (OWLObjectPropertyExpression) propEx, 
                                      individualFor( conn.getTail() ), 
                                      individualFor( conn.getHead() ) ) );
            }
            else {
                addAxiom( factory.getOWLNegativeDataPropertyAssertionAxiom( 
                                      (OWLDataPropertyExpression) propEx, 
                                      individualFor( conn.getTail() ), 
                                      literalFor( conn.getHead() ) ) );
            }
        }
        
        //TODO
    }
    
    private OWLLiteral literalFor( Graphic g ) {
        if( g instanceof Shape ) {
            return factory.getOWLLiteral( shape(g).text );
        }
        
        if( g instanceof Table ) {
            Table t = (Table) g;
                        
            Collection<Shape> cells = t.cells;
            if( cells.size() != 2 ) throw error( "Literal table must have 2 cells", g );
            
            Shape[][] table = t.table;
            if( table.length != 2 ) throw error( "Literal table must have 2 rows", g );
            
            Shape type  = table[0][0];
            Shape value = table[1][0];
            
            return factory.getOWLLiteral( 
                               value.text.trim(), 
                               factory.getOWLDatatype( IRI.create( uriFromShape( type ) )));
        }
        
        throw error( "Cannot make Literal from graphic", g );
    }
    
    private OWLPropertyExpression<?,?> propExprForConnector( Connector c ) {
        if( c instanceof Line ) {
            Line line = (Line) c;

            OWLPropertyExpression<?,?> propEx = null;
            
            for( Shape label : line.labels ) {
                OWLPropertyExpression<?,?> labelPropEx = maybePropExprFor( label );
                
                if( labelPropEx == null ) continue;
                
                if( propEx != null ) throw error( "More than one property expression label on a line", line );
                propEx = labelPropEx;
            }

            if( propEx == null ) throw error( "Prop-assertion line must have a prop label", line );
            return propEx;
        }
        
        throw error( "Cannot get prop expression from connector", (Graphic) c );
    }
    
    private OWLNamedIndividual individualFor( Graphic g ) {
        switch( noteFrom( g ) ) {
            case individual: return factory.getOWLNamedIndividual( IRI.create( uriFromShape( shape(g) ) ) );            
        }
        
        throw error( "Could not create Individual from graphic", g );
    }
    
    /** Find and add class axioms */
    private void processClassAxioms() {
        processSubclassOf();
        processEquivalentClasses();
        processDisjointClasses();
        processDisjointUnions();
        processHasKeys();
    }

    private void processHasKeys() {
        for( Shape s : shapes( GraphicNote.has_key ) ) {
            
            Collection<Connector> in = s.incoming;
            if( in.size() != 1 ) throw error( "HasKey can only have one incoming connector", s );
            Connector c = in.iterator().next();
            
            OWLClassExpression expr = classExpressionFor( c.getTail() );
            
            Set<OWLPropertyExpression<?,?>> props = new HashSet<OWLPropertyExpression<?,?>>();
            for( Connector conn : s.outgoing ) {
                props.add( propExprFor( conn.getHead() ));                
            }
            
            if( props.isEmpty() ) throw error( "HasKey must have one or more target props", s );
            
            addAxiom( factory.getOWLHasKeyAxiom( expr, props ) );                
        }                
    }
    
    private void processDisjointUnions() {
        for( Shape s : shapes( GraphicNote.disjoint_union ) ) {
            
            Collection<Connector> in = s.incoming;
            if( in.size() != 1 ) throw error( "Disjoint union can only have one incoming connector", s );
            Connector c = in.iterator().next();
            
            OWLClassExpression expr = classExpressionFor( c.getTail() );
            if( !( expr instanceof OWLClass ) ) throw error( "Disjoint union requires a class", c.getTail() );
            
            addAxiom( factory.getOWLDisjointUnionAxiom( (OWLClass) expr, classTargets( s ) ) );                
        }        
    }
    
    private void processDisjointClasses() {
        for( Connector conn : connectors( GraphicNote.disjoint_classes ) ) {
            addAxiom( factory.getOWLDisjointClassesAxiom( 
                          classExpressionFor( conn.getTail() ), 
                          classExpressionFor( conn.getHead() )) );
        }
        
        for( Shape s : shapes( GraphicNote.pairwise_disjoint ) ) {
            addAxiom( factory.getOWLDisjointClassesAxiom( classTargets( s ) ) );                
        }
    }

    private void processEquivalentClasses() {
        for( Connector conn : connectors( GraphicNote.equivalent_classes ) ) {
            addAxiom( factory.getOWLEquivalentClassesAxiom( 
                          classExpressionFor( conn.getTail() ), 
                          classExpressionFor( conn.getHead() )) );
        }
    }
    
    private void processSubclassOf() {
        for( Connector conn : connectors( GraphicNote.subclass_of ) ) {
            addAxiom( factory.getOWLSubClassOfAxiom( 
                          classExpressionFor( conn.getTail() ), 
                          classExpressionFor( conn.getHead() )) );
        }
    }

    //maybe get the property expression for a graphic
    private OWLPropertyExpression<?,?> maybePropExprFor( Graphic g ) {
        if( g == null || g.metadata.notes == null || g.metadata.notes.trim().length() == 0 ) return null;
        
        switch( noteFrom( g ) ) {
            case object_property: 
                return factory.getOWLObjectProperty( IRI.create( uriFromShape( shape(g) ) ) );
            
            case inverse_object_property: 
                return factory.getOWLObjectInverseOf( 
                           factory.getOWLObjectProperty( IRI.create( uriFromShape( shape(g) ) ) ) );
                
            case data_property:
                return factory.getOWLDataProperty( IRI.create( uriFromShape( shape(g) ) ) );
        }
        
        return null;
    }

    //get the obj prop expressions targetted by a shape
    private Set<OWLObjectPropertyExpression> objPropTargets( Shape s ) {
        Set<OWLObjectPropertyExpression> targets = new HashSet<OWLObjectPropertyExpression>();
        
        for( Connector conn : s.outgoing ) {
            Graphic g = conn.getHead();
            if( g == null ) throw error( "Must have a head target", (Graphic) conn );
            
            OWLPropertyExpression<?,?> propEx = propExprFor( g );
            if( !(propEx instanceof OWLObjectPropertyExpression )) throw error( "Not an object property expression", g );
            
            targets.add( (OWLObjectPropertyExpression) propEx );
        }

        if( targets.size() < 2 ) throw error( "Must have 2 or more outgoing connections to object props", s );
        return targets;
    }
    
    //get the data prop expressions targetted by a shape
    private Set<OWLDataPropertyExpression> dataPropTargets( Shape s ) {
        Set<OWLDataPropertyExpression> targets = new HashSet<OWLDataPropertyExpression>();
        
        for( Connector conn : s.outgoing ) {
            Graphic g = conn.getHead();
            if( g == null ) throw error( "Must have a head target", (Graphic) conn );
            
            OWLPropertyExpression<?,?> propEx = propExprFor( g );
            if( !(propEx instanceof OWLDataPropertyExpression )) throw error( "Not a data property expression", g );
            
            targets.add( (OWLDataPropertyExpression) propEx );
        }

        if( targets.size() < 2 ) throw error( "Must have 2 or more outgoing connections to data props", s );
        return targets;
    }
    
    //get the obj prop expression targetted by a shape
    private OWLObjectPropertyExpression objPropTarget( Shape s ) {
        if( s.outgoing.size() != 1 ) throw error( "Must have one outgoing connector", s );
        
        Connector conn = s.outgoing.iterator().next();
        Graphic g = conn.getHead();
        if( g == null ) throw error( "Must have a head target", (Graphic) conn );
        
        OWLPropertyExpression<?,?> propEx = propExprFor( g );
        if( propEx instanceof OWLObjectPropertyExpression ) return (OWLObjectPropertyExpression) propEx;
        throw error( "Not an object property expression", g );
    }
    
    //get the data prop expression targetted by a shape
    private OWLDataPropertyExpression dataPropTarget( Shape s ) {
        if( s.outgoing.size() != 1 ) throw error( "Must have one outgoing connector", s );
        
        Connector conn = s.outgoing.iterator().next();
        Graphic g = conn.getHead();
        if( g == null ) throw error( "Must have a head target", (Graphic) conn );
        
        OWLPropertyExpression<?,?> propEx = propExprFor( g );
        if( propEx instanceof OWLDataPropertyExpression ) return (OWLDataPropertyExpression) propEx;
        throw error( "Not a data property expression", g );
    }
    
    //definitely get the object property expression for a graphic
    private OWLObjectPropertyExpression objPropExprFor( Graphic g ) {
        OWLPropertyExpression<?,?> propEx = maybePropExprFor( g );
        if( propEx != null && propEx instanceof OWLObjectPropertyExpression ) {
            return (OWLObjectPropertyExpression) propEx;
        }
        
        throw error( "Could not create object property expression for graphic", g );
    }

    //definitely get the data property expression for a graphic
    private OWLDataPropertyExpression dataPropExprFor( Graphic g ) {
        OWLPropertyExpression<?,?> propEx = maybePropExprFor( g );
        if( propEx != null && propEx instanceof OWLDataPropertyExpression ) {
            return (OWLDataPropertyExpression) propEx;
        }
        
        throw error( "Could not create data property expression for graphic", g );
    }
    
    //definitely get the property expression for a graphic
    private OWLPropertyExpression<?,?> propExprFor( Graphic g ) {
        OWLPropertyExpression<?,?> propEx = maybePropExprFor( g );
        if( propEx != null ) return propEx;
        
        throw error( "Could not create property expression for graphic", g );
    }
    
    /**
     * Get the data range targetted by a graphic
     */
    private OWLDataRange dataRangeTargetFor( Graphic g ) {
        if( g == null || g.outgoing.size() != 1 ) throw error( "Must target 1 datatype", g );
        
        Graphic head = g.outgoing.iterator().next().getHead();
        if( head == null ) throw error( "Must target a datatype", g );
        
        return dataRangeFor( head );
    }

    /**
     * Get the literals targetted by a graphic
     */
    private Set<OWLLiteral> literalTargetsFor( Graphic g ) {
        if( g == null || g.outgoing.size() < 2 ) throw error( "Must target 2 or more literals", g );
        
        Set<OWLLiteral> literals = new HashSet<OWLLiteral>();
        for( Connector conn : g.outgoing ) {
            literals.add( literalFor( conn.getHead() ) );
        }
        
        return literals;
    }
    
    /**
     * Get the data ranges targetted by a graphic
     */
    private Set<OWLDataRange> dataRangeTargetsFor( Graphic g ) {
        if( g == null || g.outgoing.size() < 2 ) throw error( "Must target 2 or more datatypes", g );
        
        Set<OWLDataRange> ranges = new HashSet<OWLDataRange>();
        for( Connector conn : g.outgoing ) {
            ranges.add( dataRangeFor( conn.getHead() ) );
        }
        
        return ranges;
    }
    
    /**
     * Get the data range ta for a graphic
     */
    private OWLDataRange dataRangeFor( Graphic g ) {
        switch( noteFrom( g ) ) {
            case datatype: return factory.getOWLDatatype( IRI.create( uriFromShape( shape(g) ) ) );
            
            case data_complement_of: return factory.getOWLDataComplementOf( dataRangeTargetFor( g ) );
            
            case data_union_of: return factory.getOWLDataUnionOf( dataRangeTargetsFor( g ) );

            case data_intersection_of: return factory.getOWLDataIntersectionOf( dataRangeTargetsFor( g ) );
            
            case data_one_of: return factory.getOWLDataOneOf( literalTargetsFor( g ) );
            
            case datatype_restriction: return datatypeRestrictionFor( g );
        }

        throw error( "Could not create Datatype from graphic", g );
    }
    
    /**
     * Get the datatype restriction represented by a graphic
     */
    private OWLDataRange datatypeRestrictionFor( Graphic g ) {
        
        OWLDatatype datatype = factory.getOWLDatatype( IRI.create( uriFromShape( shape(g) ) ) );
                
        Set<OWLFacetRestriction> restrictions = new HashSet<OWLFacetRestriction>();
        for( Connector conn : g.outgoing ) {
            Line line = (Line) conn;
            if( line.labels.size() != 1 ) throw error( "Facet restriction requires a facet URI label", line );
            
            Shape label = line.labels.iterator().next();         
            IRI facetIRI = IRI.create( uriFromShape( shape( label ) ) );
            
            OWLLiteral literal = literalFor( line.getHead() );
            OWLFacet   facet   = OWLFacet.getFacet( facetIRI );
            OWLFacetRestriction restriction = factory.getOWLFacetRestriction( facet, literal );
            restrictions.add( restriction );
        }
        
        if( restrictions.isEmpty() ) throw error( "Datatype restriction required some facet restrictions", g );
        
        return factory.getOWLDatatypeRestriction( datatype, restrictions );
    }
    
    /**
     * Get the OWL class expression represented by a graphic
     */
    private OWLClassExpression classExpressionFor( Graphic g ) {
        switch( noteFrom( g ) ) {
            case owl_class: return factory.getOWLClass( IRI.create( uriFromShape( shape(g) ) ) );
            
            case obj_union: return factory.getOWLObjectUnionOf( classTargets( shape(g) ) );
            
            case obj_intersection: return factory.getOWLObjectIntersectionOf( classTargets( shape(g) ) );
            
            case obj_complement: return factory.getOWLObjectComplementOf( classTargets( shape(g) ).iterator().next() );
            
            case obj_one_of: return factory.getOWLObjectOneOf( individualTargets( shape(g) ) );
            
            case has_value: return hasValueFor( g );
            
            case some_values: return someValuesFor( g );

            case all_values: return allValuesFor( g );
            
            case has_self: return hasSelfFor( g );
            
            case cardinality: return cardinalityFor( g );
        }
        
        throw error( "Could not create Class Expression from graphic", g );
    }

    private enum CardinalityType { Max, Min, Exact }
    
    //get the cardinality class expression for a graphic
    private OWLClassExpression cardinalityFor( Graphic g ) {
        if( g.outgoing.size() != 1 ) throw error( "cardinality expression can only have one outgoing connection", g );
        
        Connector conn = g.outgoing.iterator().next();
        OWLPropertyExpression<?,?> propEx = propExprForConnector( conn );

        String cardString = getCardinalityString( conn );
        CardinalityType type = getCardinalityType( cardString );
        int cardinality = getCardinality( cardString );
        
        if( propEx instanceof OWLObjectPropertyExpression ) {
            OWLObjectPropertyExpression ope = (OWLObjectPropertyExpression) propEx;
            if( conn.getHead() != null ) {
                OWLClassExpression classEx = classExpressionFor( conn.getHead() );
                
                switch( type ) {
                    case Exact: return factory.getOWLObjectExactCardinality( cardinality, ope, classEx );
                    case Max:   return factory.getOWLObjectMaxCardinality( cardinality, ope, classEx );
                    case Min:   return factory.getOWLObjectMinCardinality( cardinality, ope, classEx );
                }
            }
            else {
                switch( type ) {
                    case Exact: return factory.getOWLObjectExactCardinality( cardinality, ope );
                    case Max:   return factory.getOWLObjectMaxCardinality( cardinality, ope );
                    case Min:   return factory.getOWLObjectMinCardinality( cardinality, ope );
                }
            }
        }
        else {
            OWLDataPropertyExpression dpe = (OWLDataPropertyExpression) propEx;
            if( conn.getHead() != null ) {
                OWLDataRange range = dataRangeFor( conn.getHead() );
                
                switch( type ) {
                    case Exact: return factory.getOWLDataExactCardinality( cardinality, dpe, range );
                    case Max:   return factory.getOWLDataMaxCardinality( cardinality, dpe, range );
                    case Min:   return factory.getOWLDataMinCardinality( cardinality, dpe, range );
                }
            }
            else {
                switch( type ) {
                    case Exact: return factory.getOWLDataExactCardinality( cardinality, dpe );
                    case Max:   return factory.getOWLDataMaxCardinality( cardinality, dpe );
                    case Min:   return factory.getOWLDataMinCardinality( cardinality, dpe );
                }
            }
        }
        
        throw error( "Whoops !", g );
    }
    
    private CardinalityType getCardinalityType( String cardString ) {
        cardString = cardString.replace( '[', ' ' ).replace( ']', ' ' ).trim();
        if( cardString.startsWith( ".." ) ) return CardinalityType.Max;
        if( cardString.endsWith( ".." ) ) return CardinalityType.Min;
        return CardinalityType.Exact;
    }
    
    private int getCardinality( String cardString ) {
        cardString = cardString.replace( '[', ' ' ).replace( ']', ' ' ).replace( '.', ' ' ).trim();
        return Integer.parseInt( cardString );
    }
    
    private String getCardinalityString( Connector conn ) {
        if( !(conn instanceof Line )) throw error( "Cardinality must be a line", (Graphic) conn );

        for( Shape label : ((Line) conn).labels ) {
            if( label.text != null && label.text.trim().startsWith( "[" )) {
                return label.text.trim(); 
            }
        }
        
        throw error( "Cardinality must be a line label using square brackets", (Line) conn );
    }
    
    //get the has-self expression for a graphic
    private OWLObjectHasSelf hasSelfFor( Graphic g ) {
        if( g.outgoing.size() != 1 ) throw error( "all-values can only have one outgoing connection", g );
        
        Connector conn = g.outgoing.iterator().next();
        OWLPropertyExpression<?,?> propEx = propExprForConnector( conn );

        if( propEx instanceof OWLObjectPropertyExpression ) {
            return factory.getOWLObjectHasSelf((OWLObjectPropertyExpression) propEx );
        }
        else {
            throw error( "Has-self property must be an object property", (Graphic) conn );
        }        
    }
    
    //get the all-values class expression for a graphic
    private OWLClassExpression allValuesFor( Graphic g ) {
        if( g.outgoing.size() != 1 ) throw error( "all-values can only have one outgoing connection", g );
        
        Connector conn = g.outgoing.iterator().next();
        OWLPropertyExpression<?,?> propEx = propExprForConnector( conn );

        if( propEx instanceof OWLObjectPropertyExpression ) {
            return factory.getOWLObjectAllValuesFrom( 
                       (OWLObjectPropertyExpression) propEx, 
                       classExpressionFor( conn.getHead() ) );
        }
        else {
            return factory.getOWLDataAllValuesFrom( 
                       (OWLDataPropertyExpression) propEx, 
                       dataRangeFor( conn.getHead() ) );
        }
    }

    //get the some-values class expression for a graphic
    private OWLClassExpression someValuesFor( Graphic g ) {
        if( g.outgoing.size() != 1 ) throw error( "some-values can only have one outgoing connection", g );
        
        Connector conn = g.outgoing.iterator().next();
        OWLPropertyExpression<?,?> propEx = propExprForConnector( conn );

        if( propEx instanceof OWLObjectPropertyExpression ) {
            return factory.getOWLObjectSomeValuesFrom( 
                       (OWLObjectPropertyExpression) propEx, 
                       classExpressionFor( conn.getHead() ) );
        }
        else {
            return factory.getOWLDataSomeValuesFrom( 
                    (OWLDataPropertyExpression) propEx, 
                    dataRangeFor( conn.getHead() ) );
        }
    }
    
    //get the has-value class expression for a graphic
    private OWLHasValueRestriction<?,?,?> hasValueFor( Graphic g ) {
        if( g.outgoing.size() != 1 ) throw error( "has-value can only have one outgoing connection", g );
        
        Connector conn = g.outgoing.iterator().next();
        OWLPropertyExpression<?,?> propEx = propExprForConnector( conn );

        if( propEx instanceof OWLObjectPropertyExpression ) {
            return factory.getOWLObjectHasValue( 
                       (OWLObjectPropertyExpression) propEx, 
                       individualFor( conn.getHead() ) );
        }
        else {
            return factory.getOWLDataHasValue( 
                    (OWLDataPropertyExpression) propEx, 
                    literalFor( conn.getHead() ) );
        }
    }
    
    private Shape shape( Graphic g ) {
        if( !( g instanceof Shape) ) throw error( "Expecting a shape", g );
        return (Shape) g;        
    }
    
    /**
     * Add an axiom to the ontology
     */
    private void addAxiom( OWLAxiom axiom ) {
        manager.addAxiom( ontology, axiom );
    }
    
    /**
     * Get a URI from a shape
     */
    private String uriFromShape( Shape shape ) {
        String text = shape.text;
        if( text == null || (text = text.trim()).length() == 0 ) throw error( "Missing URI", shape );
        
        return uriFromString( text );
    }
    
    private String uriFromString( String text ) {
        if( text.startsWith( "http:" ) ) return text;
        
        int colon = text.indexOf( ":" );
        
        //prefixed name
        if( colon > 0 ) {
            String prefix = text.substring( 0, colon );
            String name   = text.substring( colon + 1 );
            
            String uri = namespaces.get( prefix );
            if( uri == null ) return null;
            
            return uri + normalize( name );
        }
        
        //default namespace
        return defaultNamespace + "#" + normalize( text );        
    }
    
    /**
     * Normalize a string by removing whitespace. Lowercase words are joined
     * by hyphens.
     */
    private String normalize( String s ) {
        StringBuilder buff = new StringBuilder();
        
        boolean inWS = false;
        for( char c : s.toCharArray() ) {
            //skip whitespace
            if( Character.isWhitespace( c ) ) {
                inWS = true;
                continue;
            }
            
            //start of word
            if( inWS ) {
                inWS = false;
                
                if( buff.length() > 0 ) { //not first word
                    if( Character.isLowerCase( c ) ) {
                        buff.append( "-" ); //join lowercase word by a hyphen
                    }
                }
            }
            
            buff.append( c );
        }
        
        return buff.toString();
    }
   
    /**
     * Find all individuals that are connection-targetted
     */
    private Set<OWLNamedIndividual> individualTargets( Shape start ) {
        Collection<Shape> shapes = shapeTargets( start );
        Set<OWLNamedIndividual> inds = new HashSet<OWLNamedIndividual>();
        
        for( Shape s : shapes ) inds.add( individualFor( s ) );
        
        return inds;
    }
    
    /**
     * Find all classes that are connection-targetted
     */
    private Set<OWLClassExpression> classTargets( Shape start ) {
        Collection<Shape> shapes = shapeTargets( start );
        Set<OWLClassExpression> exprs = new HashSet<OWLClassExpression>();
        
        for( Shape s : shapes ) exprs.add( classExpressionFor( s ) );
        
        return exprs;
    }
    
    /**
     * Find all shapes that are connection-targetted - throw up if any are not
     * shapes
     */
    private Collection<Shape> shapeTargets( Shape start ) {
        Collection<Shape> shapes = new HashSet<Shape>();
        
        for( Connector conn : start.outgoing ) {
            if( conn.getHead() == null ) throw error( "Missing target", (Graphic) conn );
            if( !( conn.getHead() instanceof Shape ) ) throw error( "Must be a shape", conn.getHead() );
            shapes.add( (Shape) conn.getHead() );
        }
        
        return shapes;
    }
    
    /**
     * Find all shapes with the given note - throw up if any are not
     * shapes
     */
    private Collection<Shape> shapes( GraphicNote note ) {
        Collection<Shape> shapes = new HashSet<Shape>();
        
        for( Graphic g : graphics( note ) ) {
            if( !( g instanceof Shape ) ) throw error( "Must be a shape", g );
            shapes.add( (Shape) g );
        }
        
        return shapes;
    }
    
    /**
     * Find all connectors with the given note - throw up if any are not
     * connectors
     */
    private Collection<Connector> connectors( GraphicNote note ) {
        Collection<Connector> connectors = new HashSet<Connector>();
        
        for( Graphic g : graphics( note ) ) {
            if( !( g instanceof Connector ) ) throw error( "Must be a connector", g );
            connectors.add( (Connector) g );
        }
        
        return connectors;
    }
    
    /**
     * Get the GraphicNote from a graphic - throw up if unrecognized
     */
    private GraphicNote noteFrom( Graphic g ) {
        if( g == null ) throw new RuntimeException( "Missing graphic" );
        String note = g.metadata.notes;
        if( note == null || (note = note.trim()).length() == 0 ) throw error( "Missing graphic note", g );
        
        GraphicNote gn = null;
        try {
            gn = GraphicNote.valueOf( note );
        }
        catch( Exception ex ) {
            throw error( "Unrecognized graphic note", g );
        }
        
        return gn;
    }
    
    /**
     * Find all graphics with the given note - always return non-null
     */
    private Collection<Graphic> graphics( GraphicNote note ) {
        Collection<Graphic> gg = graphics.get( note );
        if( gg == null ) gg = Collections.emptySet();
        return gg;
    }
    
    /**
     * Get the default namespace
     */
    public String getDefaultNamespace() { 
        return defaultNamespace;
    }
    
    /**
     * Get a namespace URI
     * @param prefix the namespace prefix
     * @return null if the prefix is unknown
     */
    public String getNamespace( String prefix ) {
        return namespaces.get( prefix );
    }
    
    /**
     * Find namespace definitions embedded in the diagram. Namespace definitions
     * are shapes with note "namespace". The shape text is of the form 
     * "prefix = uri" and the default namespace is just a URI.
     * 
     * @return the default namespace
     */
    private String findNamespaceDeclarations() {
        final String[] defNS = { "http://epistem.org/og-owl/" + diagram.file.getName() };
        
        diagram.accept( new DiagramVisitor.Impl() {
            @Override public void visitShape( Shape shape ) {
                if( ! "namespace".equals( shape.metadata.notes ) ) return;
                
                String text = shape.text;
                if( text == null || (text = text.trim()).length() == 0 ) {
                    throw error( "Blank namespace", shape );
                }
                
                int equals = text.indexOf( "=" );
                if( equals > 0 ) {
                    String prefix = text.substring( 0, equals ).trim();
                    String uri    = text.substring( equals + 1 ).trim();
                    addNamespace( prefix, uri );
                }
                else defNS[0] = text;
            }            
        });
        
        return defNS[0];
    }
    
    /**
     * Predefine the common OWL and RDF namespaces
     */
    private void addCommonNamespaces() {
        addNamespace( "owl",     "http://www.w3.org/2002/07/owl#" );
        addNamespace( "xsd",     "http://www.w3.org/2001/XMLSchema#" );
        addNamespace( "owl2xml", "http://www.w3.org/2006/12/owl2-xml#" );
        addNamespace( "rdfs",    "http://www.w3.org/2000/01/rdf-schema#" );
        addNamespace( "rdf",     "http://www.w3.org/1999/02/22-rdf-syntax-ns#" );      
        addNamespace( "dc",      "http://purl.org/dc/elements/1.1/" );
        addNamespace( "swrl",    "http://www.w3.org/2003/11/swrl#" );
        addNamespace( "swrlb",   "http://www.w3.org/2003/11/swrlb#" );
        addNamespace( "foaf",    "http://xmlns.com/foaf/0.1/" );
    }
    
    /**
     * Raise an error
     * 
     * @param message the error message
     * @param g the graphic at fault
     */
    private RuntimeException error( String message, Graphic g ) {
        if( g == null ) return new RuntimeException( message );
        return new RuntimeException( message + ": " + g.toLocationString() );
    }
    
    /**
     * Add a namespace
     * @param prefix the prefix
     * @param uri the URI
     */
    public void addNamespace( String prefix, String uri ) {
        namespaces.put( prefix, uri );
    }
    
    //visitor that gathers graphics in the "graphics" map
    private final DiagramVisitor noteVisitor = new DiagramVisitor.Impl() {

        private void register( Graphic g ) {
            String note = g.metadata.notes;
            if( note == null || (note = note.trim()).length() == 0 ) return;
            
            try {
                GraphicNote gn = GraphicNote.valueOf( note );
                
                Collection<Graphic> gg = graphics.get( gn );
                if( gg == null ) graphics.put( gn, gg = new HashSet<Graphic>() );
                gg.add( g );
            }
            catch( Exception ex ) {}
        }
        
        @Override public void visitConnectorShape( ConnectorShape shape ) {
            register( shape );
        }
        @Override public DiagramVisitor visitGroupStart( Group group ) {
            register( group );
            return super.visitGroupStart( group );
        }
        @Override public DiagramVisitor visitLineStart( Line line ) {
            register( line );
            return super.visitLineStart( line );
        }
        @Override public void visitShape( Shape shape ) {
            register( shape );
        }
        @Override public DiagramVisitor visitTableStart( Table table ) {
            register( table );
            return super.visitTableStart( table );
        }        
    };
}
