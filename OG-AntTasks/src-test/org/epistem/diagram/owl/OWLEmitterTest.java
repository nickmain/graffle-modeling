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

import junit.framework.TestCase;

import org.epistem.diagram.model.Diagram;
import org.epistem.diagram.model.Page;
import org.epistem.graffle.OmniGraffleDoc;
import org.semanticweb.owlapi.io.DefaultOntologyFormat;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.StreamDocumentTarget;
import org.semanticweb.owlapi.io.StringDocumentTarget;

public class OWLEmitterTest extends TestCase {

    private Diagram    diagram;
    private OWLEmitter emitter;

    public OWLEmitterTest( String name ) throws Exception {
        super( name );
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File file = new File( "test-diagrams/test-owl.graffle" );
        OmniGraffleDoc doc = new OmniGraffleDoc( file );
        diagram = new Diagram( doc );
        emitter = new OWLEmitter( diagram );
    }

    public void testNamespaces() {
        assertEquals( "http://www.test.com/test", emitter.getDefaultNamespace() );
        assertEquals( "http://www.test.com/foo", emitter.getNamespace( "foo" ) );
    }

    public void testObjectProps() {
        processPage( "Obj Props" );
        
        ontologyAssertions( 
            "Declaration(ObjectProperty(<http://www.test.com/test#obj-prop-b>))",
            "FunctionalObjectProperty(<http://www.test.com/test#obj-prop1>)",
            "ReflexiveObjectProperty(<http://www.test.com/test#obj-prop2>)",
            "InverseFunctionalObjectProperty(<http://www.test.com/test#obj-prop3>)",
            "IrreflexiveObjectProperty(<http://www.test.com/test#obj-prop4>)",
            "SymmetricObjectProperty(<http://www.test.com/test#obj-prop5>)",
            "TransitiveObjectProperty(<http://www.test.com/test#obj-prop6>)",
            "AsymmetricObjectProperty(<http://www.test.com/test#obj-prop7>)",
            "EquivalentObjectProperties(<http://www.test.com/test#obj-prop10> <http://www.test.com/test#obj-prop11>)",
            "InverseObjectProperties(<http://www.test.com/test#obj-prop9> <http://www.test.com/test#obj-prop8>)",
            "DisjointObjectProperties(<http://www.test.com/test#obj-prop12> <http://www.test.com/test#obj-prop13>)",
            "DisjointObjectProperties(<http://www.test.com/test#obj-prop14> <http://www.test.com/test#obj-prop15> <http://www.test.com/test#obj-prop16>)",
            "SubObjectPropertyOf(<http://www.test.com/test#obj-prop16> <http://www.test.com/test#obj-prop17>)",
            "ObjectPropertyDomain(<http://www.test.com/test#obj-prop18> <http://www.test.com/test#ClassA>)",
            "ObjectPropertyRange(<http://www.test.com/test#obj-prop18> ObjectUnionOf(<http://www.test.com/test#ClassC> <http://www.test.com/test#ClassB>))",
            "SubObjectPropertyOf(ObjectPropertyChain(<http://www.test.com/test#obj-prop-a> <http://www.test.com/test#obj-prop-b> <http://www.test.com/test#obj-prop-c>) <http://www.test.com/test#obj-prop-d>)"
        );
    }
    
    public void testDataClassExprs() {
        processPage( "Class Data Exprs" );
        
        ontologyAssertions(
            "SubClassOf(<http://www.test.com/test#ClassA> DataHasValue(<http://www.test.com/test#data-prop-a> \"32\"^^xsd:integer))",
            "SubClassOf(<http://www.test.com/test#ClassC> DataAllValuesFrom(<http://www.test.com/test#data-prop-a> xsd:nonNegativeInteger))",
            "SubClassOf(<http://www.test.com/test#ClassB> DataSomeValuesFrom(<http://www.test.com/test#data-prop-b> xsd:nonNegativeInteger))",
            "SubClassOf(<http://www.test.com/test#Class1> DataExactCardinality(4 <http://www.test.com/test#dprop1> xsd:foo))",
            "SubClassOf(<http://www.test.com/test#Class2> DataMaxCardinality(3 <http://www.test.com/test#dprop2> xsd:foo))",
            "SubClassOf(<http://www.test.com/test#Class3> DataMinCardinality(2 <http://www.test.com/test#dprop3> xsd:foo))",
            "SubClassOf(<http://www.test.com/test#Class4> DataExactCardinality(5 <http://www.test.com/test#dprop4>))",
            "SubClassOf(<http://www.test.com/test#Class5> DataMaxCardinality(6 <http://www.test.com/test#dprop5>))",
            "SubClassOf(<http://www.test.com/test#Class6> DataMinCardinality(7 <http://www.test.com/test#dprop6>))"
        );
    }
    
    public void testDataRanges() {
        processPage( "Data Ranges" );
        
        ontologyAssertions(
            "SubClassOf(<http://www.test.com/test#ClassX> DataSomeValuesFrom(<http://www.test.com/test#data-prop-x> DataComplementOf(xsd:nonNegativeInteger)))",
            "SubClassOf(<http://www.test.com/test#ClassY> DataSomeValuesFrom(<http://www.test.com/test#data-prop-y> DataUnionOf(xsd:string xsd:integer)))",
            "SubClassOf(<http://www.test.com/test#ClassZ> DataSomeValuesFrom(<http://www.test.com/test#data-prop-z> DataIntersectionOf(xsd:string xsd:integer)))",
            "SubClassOf(<http://www.test.com/test#ClassW> DataSomeValuesFrom(<http://www.test.com/test#data-prop-w> DataOneOf(\"45\"^^xsd:integer \"24\"^^xsd:string)))",
            "Declaration(Datatype(<http://www.test.com/test#foo-bar>))",
            "DatatypeDefinition(<http://www.test.com/test#foo-bar> DataOneOf(\"wonka\"^^xsd:string \"3\"^^xsd:integer))",
            "SubClassOf(<http://www.test.com/test#ClassV> DataSomeValuesFrom(<http://www.test.com/test#data-prop-v> "
                 +"DatatypeRestriction(xsd:integer ",
                 "xsd:maxExclusive \"66\"^^xsd:integer",
                 "xsd:minInclusive \"45\"^^xsd:integer"
        );
    }
    
    public void testDataProps() {
        processPage( "Data Props" );

        ontologyAssertions(
            "Declaration(DataProperty(<http://www.test.com/test#data-prop-a>))",
            "DataPropertyDomain(<http://www.test.com/test#data-prop-a> <http://www.test.com/test#ClassA>)",
            "EquivalentDataProperties(<http://www.test.com/test#data-prop-c> <http://www.test.com/test#data-prop-b>)",
            "DisjointDataProperties(<http://www.test.com/test#data-prop-e> <http://www.test.com/test#data-prop-d>)",
            "DisjointDataProperties(<http://www.test.com/test#data-prop-f> <http://www.test.com/test#data-prop-g> <http://www.test.com/test#data-prop-h>)",
            "FunctionalDataProperty(<http://www.test.com/test#data-prop-i>)",
            "SubDataPropertyOf(<http://www.test.com/test#data-prop-k> <http://www.test.com/test#data-prop-j>)",
            "DataPropertyRange(<http://www.test.com/test#data-prop-a> xsd:nonNegativeInteger)"
        );
    }
    
    public void testIndividuals() {
        processPage( "Individuals" );

        ontologyAssertions(  
            "Declaration(NamedIndividual(<http://www.test.com/test#IndividualA>))",
            "SameIndividual(<http://www.test.com/test#IndividualA> <http://www.test.com/test#IndividualB>)",
            "DifferentIndividuals(<http://www.test.com/test#IndividualC> <http://www.test.com/test#IndividualA>)",
            "ClassAssertion(<http://www.test.com/test#ClassA> <http://www.test.com/test#IndividualC>)",
            "ObjectPropertyAssertion(<http://www.test.com/test#obj-prop-a> <http://www.test.com/test#IndividualB> <http://www.test.com/test#IndividualC>)",
            "NegativeObjectPropertyAssertion(<http://www.test.com/test#obj-prop-b> <http://www.test.com/test#IndividualB> <http://www.test.com/test#IndividualC>)",
            "ObjectPropertyAssertion(ObjectInverseOf(<http://www.test.com/test#obj-prop-c>) <http://www.test.com/test#IndividualB> <http://www.test.com/test#IndividualC>)",
            "DataPropertyAssertion(<http://www.test.com/test#data-prop-a> <http://www.test.com/test#IndividualB> \"45\"^^xsd:integer)",
            "NegativeDataPropertyAssertion(<http://www.test.com/test#data-prop-b> <http://www.test.com/test#IndividualB> \"24\"^^xsd:string)"
        );
    }
    
    public void testClassExpressions() {
        processPage( "Class Exprs" );
        
        ontologyAssertions(  
            "SubClassOf(<http://www.test.com/test#ClassA> ObjectUnionOf(<http://www.test.com/test#ClassC> <http://www.test.com/test#ClassB>))",
            "EquivalentClasses(<http://www.test.com/test#ClassA> ObjectIntersectionOf(<http://www.test.com/test#ClassE> <http://www.test.com/test#ClassD>))",
            "SubClassOf(<http://www.test.com/test#ClassA> ObjectComplementOf(<http://www.test.com/test#ClassF>))",
            "EquivalentClasses(<http://www.test.com/test#ClassG> ObjectOneOf(<http://www.test.com/test#IndividualA> <http://www.test.com/test#IndividualB>))",
            "SubClassOf(<http://www.test.com/test#ClassA> ObjectHasValue(<http://www.test.com/test#obj-prop-a> <http://www.test.com/test#IndividualC>))",
            "SubClassOf(<http://www.test.com/test#ClassB> ObjectSomeValuesFrom(<http://www.test.com/test#obj-propX> <http://www.test.com/test#ClassX>))",
            "SubClassOf(<http://www.test.com/test#ClassC> ObjectAllValuesFrom(<http://www.test.com/test#obj-propY> <http://www.test.com/test#ClassY>))",
            "SubClassOf(<http://www.test.com/test#ClassD> ObjectHasSelf(<http://www.test.com/test#obj-propZ>))"
        );
    }

    public void testObjectCardinalityExpressions() {
        processPage( "Class Exprs" );
        
        ontologyAssertions(  
            "SubClassOf(<http://www.test.com/test#Class1> ObjectExactCardinality(4 <http://www.test.com/test#prop1> <http://www.test.com/test#Class11>))",
            "SubClassOf(<http://www.test.com/test#Class2> ObjectMaxCardinality(3 <http://www.test.com/test#prop2> <http://www.test.com/test#Class22>))",
            "SubClassOf(<http://www.test.com/test#Class3> ObjectMinCardinality(2 <http://www.test.com/test#prop3> <http://www.test.com/test#Class33>))",
            "SubClassOf(<http://www.test.com/test#Class4> ObjectExactCardinality(5 <http://www.test.com/test#prop4>))",
            "SubClassOf(<http://www.test.com/test#Class5> ObjectMaxCardinality(6 <http://www.test.com/test#prop5>))",
            "SubClassOf(<http://www.test.com/test#Class6> ObjectMinCardinality(7 <http://www.test.com/test#prop6>))"
        );
    }
    
    public void testSubclassOf() {
        processPage( "Class Axioms" );
        
        ontologyAssertions(  
            "SubClassOf(<http://www.test.com/test#ClassA> owl:Thing)",
            "SubClassOf(<http://www.test.com/test#ClassB> <http://www.test.com/test#ClassA>)" 
        );
    }

    public void testEquivalentClasses() {
        processPage( "Class Axioms" );
        
        ontologyAssertions(  
            "EquivalentClasses(<http://www.test.com/test#ClassC> <http://www.test.com/test#ClassB>)" 
        );
    }

    public void testDisjointClasses() {
        processPage( "Class Axioms" );
        
        ontologyAssertions(  
            "DisjointClasses(<http://www.test.com/test#ClassC> <http://www.test.com/test#ClassD>)",
            "DisjointClasses(<http://www.test.com/test#ClassE> <http://www.test.com/test#ClassF> <http://www.test.com/test#ClassG>)",
            "DisjointUnion(<http://www.test.com/test#ClassJ> <http://www.test.com/test#ClassI> <http://www.test.com/test#ClassH>)"
        );
    }

    public void testSWRL() {
        processPage( "SWRL" );
        
        ontologyAssertions(  
            "DLSafeRule(Body(BuiltInAtom(<http://www.w3.org/2003/11/swrlb#multiply> Variable(<http://www.test.com/test#swrlVar_ears>) Variable(<http://www.test.com/test#swrlVar_hc>) \"2\"^^xsd:integer) DataPropertyAtom(<http://www.test.com/test#headCount> Variable(<http://www.test.com/test#swrlVar_x>) Variable(<http://www.test.com/test#swrlVar_hc>)))Head(DataPropertyAtom(<http://www.test.com/test#earCount> Variable(<http://www.test.com/test#swrlVar_x>) Variable(<http://www.test.com/test#swrlVar_ears>))))",
            "DLSafeRule(Body(DifferentIndividualsAtom(Variable(<http://www.test.com/test#swrlVar_x>) Variable(<http://www.test.com/test#swrlVar_y>)))Head(ObjectPropertyAtom(<http://www.test.com/test#unlike> Variable(<http://www.test.com/test#swrlVar_x>) Variable(<http://www.test.com/test#swrlVar_y>))))",
            "DLSafeRule(Body(BuiltInAtom(<http://www.w3.org/2003/11/swrlb#greaterThan> \"1\"^^xsd:integer Variable(<http://www.test.com/test#swrlVar_hc>)) DataPropertyAtom(<http://www.test.com/test#headCount> Variable(<http://www.test.com/test#swrlVar_x>) Variable(<http://www.test.com/test#swrlVar_hc>)))Head())",
            "DLSafeRule(Body(SameIndividualAtom(Variable(<http://www.test.com/test#swrlVar_x>) Variable(<http://www.test.com/test#swrlVar_y>)))Head(ObjectPropertyAtom(<http://www.test.com/test#like> Variable(<http://www.test.com/test#swrlVar_x>) Variable(<http://www.test.com/test#swrlVar_y>))))"
        );
    }

    
    public void testHasKey() {
        processPage( "Class Axioms" );
        
        ontologyAssertions(  
            "HasKey(<http://www.test.com/test#ClassK> (<http://www.test.com/test#obj-prop-a> <http://www.test.com/test#obj-prop-b>) (<http://www.test.com/test#data-prop-b>))"
        );
    }
    
    public void testDumpOntology() {
        emitter.processDiagram();
        System.out.println( ontologyToString() );
    }

    //save ontology so that it can examined in Protege
    public void testSaveOntology() throws Exception {
        emitter.processDiagram();
        
        File genDir = new File( "generated" );
        genDir.mkdirs();
        
        FileOutputStream out = new FileOutputStream( new File( genDir, "test.rdf" ) );
        emitter.saveOntology( new DefaultOntologyFormat(), new StreamDocumentTarget( out ) );
        out.close();
    }
    
    //assert that strings are contained in ontology functional syntax
    private void ontologyAssertions( String...strings ) {
        String ontolText = ontologyToString();
        
        for( int i = 0; i < strings.length; i++ ) {
            assertTrue( strings[i], ontolText.contains( strings[i] ) );
        }        
    }
    
    //get the ontology as a functional-syntax string
    private String ontologyToString() {
        StringDocumentTarget target = new StringDocumentTarget();
        emitter.saveOntology( new OWLFunctionalSyntaxOntologyFormat(), target );

        return target.toString();        
    }
    
    /** Process just one page */
    private void processPage( String pageName ) {
        for( Page page : diagram.pages ) {
            if( pageName.equals( page.title ) ) {
                emitter.processPage( page );
            }
        }
    }
}
