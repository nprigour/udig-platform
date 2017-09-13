package org.locationtech.udig.project.ui.feature;

import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.IFeatureSite;
import org.locationtech.udig.project.ui.internal.tool.impl.ToolContextImpl;

/**
 * Facilitate editing of feature content.
 */
public class SimpleFeatureSiteImpl extends ToolContextImpl implements IFeatureSite {

    EditFeature editFeature;

    public SimpleFeatureSiteImpl() {

    }

    public SimpleFeatureSiteImpl( ILayer layer ) {
        this(layer.getMap());
    }

    public SimpleFeatureSiteImpl( IMap map ) {
        setMapInternal((Map) map);
    }

    public void setFeature( SimpleFeature feature ) {
        if (feature == null) {
            editFeature = null;
            return;
        }
        if (editFeature != null && feature != null && editFeature.getID().equals(feature.getID())) {
            // they are the same
            return;
        }
        editFeature = new EditFeature(getEditManager(), feature);
    }

    public EditFeature getEditFeature() {
        if( editFeature == null && getEditManager() != null ){
            setFeature( getEditManager().getEditFeature() );
        }
        return editFeature;
    }

    public void setMapInternal( Map map ) {
        if( map == getMap() ){
            return;
        }
        super.setMapInternal(map);
    }

    /**
     * Copy the provided FeatureSite.
     * 
     * @param copy
     */
    public SimpleFeatureSiteImpl( SimpleFeatureSiteImpl copy ) {
        super(copy);
    }

    public SimpleFeatureSiteImpl copy() {
        return new SimpleFeatureSiteImpl(this);
    }
}
