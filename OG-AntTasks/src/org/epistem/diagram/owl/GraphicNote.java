package org.epistem.diagram.owl;

/**
 * The Graphic notes representing ontology elements
 *
 * @author nickmain
 */
public enum GraphicNote {

    ontology_import,
    annotation,
    rule,
    
    //--Class Axioms
    subclass_of,
    equivalent_classes,
    disjoint_classes,
    pairwise_disjoint,
    disjoint_union,
    has_key,    
    
    //--Class Expressions
    owl_class,
    obj_union,
    obj_intersection,
    obj_complement,
    obj_one_of,
    has_value,
    some_values,
    all_values,
    has_self,
    cardinality,
    
    //--Individuals
    individual,
    prop_assertion,
    neg_prop_assertion,
    same_individual,
    different_individuals,
    class_assertion,
    
    //--Object Properties
    object_property,
    inverse_object_property,
    functional,
    reflexive,
    inverse_functional,
    irreflexive,
    symmetric,
    transitive,
    asymmetric,
    inverse_properties,
    equivalent_object_properties,
    disjoint_object_properties,
    pairwise_disjoint_object_properties,
    sub_object_property_of,
    object_property_domain,
    object_property_range,
    sub_object_property_chain,
    
    //--Data Properties
    data_property,
    data_property_domain,
    data_property_range,
    equivalent_data_properties,
    disjoint_data_properties,
    pairwise_disjoint_data_properties,
    functional_data_property,
    sub_data_property_of,
    
    //--Data types
    datatype,
    literal,
    data_complement_of,
    data_union_of,
    data_intersection_of,
    data_one_of,
    datatype_restriction,
    datatype_definition,
    
    //--Annotations
    
}
