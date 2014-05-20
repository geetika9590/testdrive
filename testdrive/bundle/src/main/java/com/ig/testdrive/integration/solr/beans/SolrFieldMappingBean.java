package com.ig.testdrive.integration.solr.beans;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: Geetika
 * Date: 19/5/14
 * Time: 10:32 AM
 */
public class SolrFieldMappingBean implements Serializable {
    private static final long  serialVersionUID=99434343444355L;

    private transient String resourceType;
    private transient String solrField;
    private transient String cqField;
    private transient String referenceField;
    private transient boolean isReference;

    public String getReferenceField() {
        return referenceField;
    }

    public void setReferenceField(final String referenceField) {
        this.referenceField = referenceField;
    }

    public boolean isReference() {
        return isReference;
    }

    public SolrFieldMappingBean(final String resourceType, final boolean reference,final String referenceField) {
        this.resourceType = resourceType;
        isReference = reference;
        this.referenceField = referenceField;
    }


    public void setReference(final boolean reference) {
        isReference = reference;
    }

    public SolrFieldMappingBean(final String solrField,final String cqField,final String resourceType) {
        this.solrField = solrField;
        this.cqField = cqField;
        this.resourceType = resourceType;
    }

    public SolrFieldMappingBean() {
        /* default constructor for SolrFieldMappingBean  */
    }

    public String getSolrField() {
        return solrField;
    }

    public void setSolrField(final String solrField) {
        this.solrField = solrField;
    }

    public String getCqField() {
        return cqField;
    }

    public void setCqField(final String cqField) {
        this.cqField = cqField;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(final String resourceType) {
        this.resourceType = resourceType;
    }


}
