package com.ig.testdrive.integration.solr.beans;

import java.io.Serializable;

/**
 * this class contains the mapping for resource types ,
 * cq Fields and solr fields.
 */
public class SolrFieldMappingBean implements Serializable {
    /**
     * SerialVersionId for serialization.
     */
    private static final long serialVersionUID = 99434343444355L;
    /**
     * It contains the resourceType of the component.
     */
    private transient String resourceType;
    /**
     * It contains the solrField.
     */
    private transient String solrField;
    /**
     * It contains the cqField.
     */
    private transient String cqField;
    /**
     * It contains the referenceField.
     */
    private transient String referenceField;
    /**
     * It contains the boolean value of reference to
     * indicate whether it is a reference component .
     */
    private transient boolean reference;

    /**
     * Getter method for referenceField.
     * @return It returns the referenceField.
     */
    public String getReferenceField() {
        return referenceField;
    }

    /**
     * Setter method for referenceField.
     * @param referenceField
     */
    public void setReferenceField(final String referenceField) {
        this.referenceField = referenceField;
    }

    /**
     * This method returns the boolean value for referencefield
     * @return isReference
     */
    public boolean isReference() {
        return reference;
    }

    /**
     * Parametrised constructor for SolrFieldMappingBean.
     * @param resourceType
     * @param reference
     * @param referenceField
     */
    public SolrFieldMappingBean(final String resourceType, final boolean reference, final String referenceField) {
        this.resourceType = resourceType;
        this.reference = reference;
        this.referenceField = referenceField;
    }

    /**
     * Setter method for reference.
     * @param reference
     */
    public void setReference(final boolean reference) {
        this.reference = reference;
    }

    /**
     * Parametrised constructor for SolrFieldMappingBean.
     * @param solrField
     * @param cqField
     * @param resourceType
     */
    public SolrFieldMappingBean(final String solrField, final String cqField, final String resourceType) {
        this.solrField = solrField;
        this.cqField = cqField;
        this.resourceType = resourceType;
    }

    /**
     * Default constructor for SolrFieldMappingBean
     */
    public SolrFieldMappingBean() {
        /* default constructor for SolrFieldMappingBean  */
    }

    /**
     * Getter for the solrField.
     * @return It return the solrField.
     */
    public String getSolrField() {
        return solrField;
    }

    /**
     * Setter for SolrField.
     * @param solrField
     */
    public void setSolrField(final String solrField) {
        this.solrField = solrField;
    }

    /**
     * Getter for the cqField.
     * @return It return the cqField.
     */
    public String getCqField() {
        return cqField;
    }

    /**
     * Setter for cqField.
     * @param cqField
     */
    public void setCqField(final String cqField) {
        this.cqField = cqField;
    }

    /**
     * Getter for resourceType.
     * @return  It returns the resourceType.
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Setter for resourceType.
     * @param resourceType
     */
    public void setResourceType(final String resourceType) {
        this.resourceType = resourceType;
    }


}
