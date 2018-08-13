/*
 *    uDig - User Friendly Desktop Internet GIS client
 *    http://udig.refractions.net
 *    (C) 2012, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.project.internal.impl;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.Interaction;
import org.locationtech.udig.project.UDIGPrecisionModel;
import org.locationtech.udig.project.internal.EditManager;
import org.locationtech.udig.project.internal.Messages;
import org.locationtech.udig.project.internal.ProjectPlugin;
import org.locationtech.udig.ui.PlatformGIS;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.opengis.filter.identity.FeatureId;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.precision.GeometryPrecisionReducer;

/**
 * A FeatureStore decorator that does not allow the transaction to be set more than once. (Only if
 * the current transaction is "AUTO_COMMIT" can the transaction be set)
 * 
 * @author jones
 * @since 1.0.0
 * @version 1.2.1
 */
public class UDIGFeatureStore implements FeatureStore<FeatureType,Feature>, UDIGStore {

    FeatureStore<FeatureType,Feature> wrapped;
    ILayer layer;

    /**
     * Create a new FeatureStore decorator that does not allow the transaction to be set more than
     * once. (Only if the current transaction is "AUTO_COMMIT" can the transaction be set)
     * 
     * @param store the feature store that will be decorated
     * @param layer layer providing context
     */
    public UDIGFeatureStore( FeatureStore<FeatureType, Feature> featureStore, ILayer layer ) {
        wrapped = featureStore;
        this.layer = layer;
    }

    public Name getName() {
        return wrapped.getName();
    }

    public ResourceInfo getInfo() {
        return wrapped.getInfo();
    }

    public void removeFeatures( Filter filter ) throws IOException {
        setTransactionInternal();
        try {
        	wrapped.removeFeatures(filter);
	    } catch (Exception e) {
	    	handleException(e);
			throw e;
	    }
    }

    @Deprecated
    public void modifyFeatures( AttributeDescriptor[] descriptors, Object[] values, Filter filter )
            throws IOException {
        setTransactionInternal();
        for (Object value : values) {
        	value = applyPrecisionModel(value, filter);     
        }
        try {
        	wrapped.modifyFeatures(descriptors, values, filter);
        } catch (Exception e) {
        	handleException(e);
    		throw e;
        }
    }
    
    public void modifyFeatures( Name[] names, Object[] values, Filter filter ) throws IOException {
        setTransactionInternal();
        for (Object value : values) {
        	value = applyPrecisionModel(value, filter);     
        }
        try {
        	wrapped.modifyFeatures(names, values, filter);
        } catch (Exception e) {
        	handleException(e);
    		throw e;
        }
    }
    
    public void modifyFeatures( Name name, Object value, Filter filter ) throws IOException {
        setTransactionInternal();
        value = applyPrecisionModel(value, filter);
        try {
	        wrapped.modifyFeatures(name, value, filter);
        } catch (Exception e) {
        	handleException(e);
        }
    }

    @Deprecated
    public void modifyFeatures( AttributeDescriptor attribute, Object value, Filter selectFilter )
            throws IOException {
        setTransactionInternal();
        value = applyPrecisionModel(value, selectFilter);
        try {
	        wrapped.modifyFeatures(attribute, value, selectFilter);
        } catch (Exception e) {
        	handleException(e);
        }
    }

    public void setFeatures( FeatureReader<FeatureType, Feature> features )
            throws IOException {
        setTransactionInternal();
        //consider PrecisionModel that may be set
        if (UDIGPrecisionModel.getModel() != null) {
            final GeometryPrecisionReducer reducer = new GeometryPrecisionReducer(UDIGPrecisionModel.getModel());
            reducer.setPointwise(true);
            while (features.hasNext()) {
            	Feature feature = features.next();
            	GeometryAttribute attrib = feature.getDefaultGeometryProperty();
                attrib.setValue(reducer.reduce((
                        Geometry)feature.getDefaultGeometryProperty().getValue()));
                feature.setDefaultGeometryProperty(attrib);
            }
        }
        try {
        	wrapped.setFeatures(features);
	    } catch (Exception e) {
	    	handleException(e);
			throw e;
	    }
    }

    public void setTransaction( Transaction transaction ) {
        throw new IllegalArgumentException(Messages.UDIGFeatureStore_0
                + Messages.UDIGFeatureStore_1);
    }
    
    /** Called when commitRollbackCompleted to restore Transaction.AUTO_COMMIT */
    public void editComplete() {
        wrapped.setTransaction(Transaction.AUTO_COMMIT);
    }
    /**
     * Called when any method that may modify feature content is used.
     * <p>
     * This method is responsible for setting the transaction prior to use.
     */
    private void setTransactionInternal() {
        if (!layer.getInteraction(Interaction.EDIT)) {
            String message = "Attempted to open a transaction on a non-editable layer (Aborted)";
            IllegalStateException illegalStateException = new IllegalStateException( message );
            ProjectPlugin.log(message, illegalStateException);
            throw illegalStateException;
        }
        // grab the current map transaction
        EditManager editManager = (EditManager) layer.getMap().getEditManager();
        Transaction transaction = editManager.getTransaction();
        
        if (wrapped.getTransaction() == null 
        		|| wrapped.getTransaction() == Transaction.AUTO_COMMIT) {
            // change over from autocommit to transactional
            wrapped.setTransaction(transaction);
        }
        else if (wrapped.getTransaction() != transaction){
            // a transaction is already present? huh ...
            String msg = "Layer transaction already set "+wrapped.getTransaction(); //$NON-NLS-1$
            IllegalStateException illegalStateException = new IllegalStateException(msg);
            ProjectPlugin.log(msg,illegalStateException);
            throw illegalStateException;
        }
    }

    /**
     * Used to start a transaction.
     * <p>
     * Q: V(italus) Think out how to provide for developers the opportunity to use its own FeatureStore
     * wrapper, not UDIGFeatureStore.
     * <p>
     * A: (Jody) They can use the id; and grab the actual IResource from
     * the catalog; and get there own that way.
     */
    public void startTransaction() {
        if (wrapped.getTransaction() == Transaction.AUTO_COMMIT) {
            Transaction transaction = ((EditManager) layer.getMap().getEditManager())
                    .getTransaction();
            wrapped.setTransaction(transaction);
        }
    }

    public Transaction getTransaction() {
        // may need to check that this is not auto commit?
        return wrapped.getTransaction();
    }

    public DataAccess<FeatureType,Feature> getDataStore() {
        return wrapped.getDataStore();
    }

    public void addFeatureListener( FeatureListener listener ) {
        wrapped.addFeatureListener(listener);
    }

    public void removeFeatureListener( FeatureListener listener ) {
        wrapped.removeFeatureListener(listener);
    }

    public FeatureCollection<FeatureType,Feature> getFeatures( Query query )
            throws IOException {
        return wrapped.getFeatures(query);
    }

    public FeatureCollection<FeatureType,Feature> getFeatures( Filter filter )
            throws IOException {
        return wrapped.getFeatures(filter);
    }

    public FeatureCollection<FeatureType,Feature> getFeatures() throws IOException {
        return wrapped.getFeatures();
    }

    public FeatureType getSchema() {
        return wrapped.getSchema();
    }

    public ReferencedEnvelope getBounds() throws IOException {
        return wrapped.getBounds();
    }

    public ReferencedEnvelope getBounds( Query query ) throws IOException {
        return wrapped.getBounds(query);
    }

    public int getCount( Query query ) throws IOException {
        return wrapped.getCount(query);
    }

    public List<FeatureId> addFeatures( FeatureCollection<FeatureType, Feature> features )
            throws IOException {
        setTransactionInternal();

        //consider PrecisionModel that may be set
        if (UDIGPrecisionModel.getModel() != null) {
            final GeometryPrecisionReducer reducer = new GeometryPrecisionReducer(UDIGPrecisionModel.getModel());
            reducer.setPointwise(true);
            features.accepts(new FeatureVisitor() {         
                @Override
                public void visit(Feature feature) {
                    GeometryAttribute attrib = feature.getDefaultGeometryProperty();
                    attrib.setValue(reducer.reduce((
                            Geometry)feature.getDefaultGeometryProperty().getValue()));
                    feature.setDefaultGeometryProperty(attrib);
                }
            }, null);
        }
        
        try {
        	return wrapped.addFeatures(features);
	    } catch (Exception e) {
	    	handleException(e);
			throw e;
	    }
    }

    // Jody -This was unused
    public boolean sameSource( Object source ) {
        return source == wrapped || source == this;
    }
    
    public FeatureStore<?,?> wrapped() {
        return wrapped;
    }

    public Set<Key> getSupportedHints() {
        return wrapped.getSupportedHints();
    }

    public QueryCapabilities getQueryCapabilities() {
        return wrapped.getQueryCapabilities();
    }
    
    
    /**
     * log and provide feedback on exception
     * 
     * @param e
     * @throws IOException
     */
	private void handleException(Exception e) throws IOException {
		
		EditManager editManager = (EditManager) layer.getMap().getEditManager();
        Transaction transaction = editManager.getTransaction();
        transaction.rollback();
        editComplete();
        
		ProjectPlugin.getPlugin().log(e);
		PlatformGIS.syncInDisplayThread(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openError(new Shell(), null, "An error occured while trying to update/create/delete features. See log view for details");        
			}               
		});
	}
	
	/**
	 * apply precision model and check validity.
	 * 
	 * @param value
	 * @param filter
	 * @return
	 * @throws IOException
	 */
	private Object applyPrecisionModel(Object value, Filter filter) throws IOException {
		if (value instanceof Geometry) {
            //consider PrecisionModel that may be set
            Geometry geom = (Geometry) ((UDIGPrecisionModel.getModel() == null) ? 
                    value : UDIGPrecisionModel.getPrecisionReducer().reduce((Geometry)value));
            if (!geom.isValid()) {
                WKTWriter writer = new WKTWriter();
                String wkt = writer.write(geom);
                String where = filter.toString();
                if (filter instanceof Id) {
                    Id id = (Id) filter;
                    where = id.getIDs().toString();
                }
                String msg = "Modify fetures (WHERE " + where + ") failed with invalid geometry:"
                        + wkt;
                ProjectPlugin.log(msg);
                throw new IOException(msg);
            }
            value = geom;
        }
		return value;
	}
}
