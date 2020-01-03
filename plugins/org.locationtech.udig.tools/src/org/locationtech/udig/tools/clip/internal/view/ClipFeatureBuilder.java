/* uDig - User Friendly Desktop Internet GIS client
 * http://udig.refractions.net
 * (C) 2012, Refractions Research Inc.
 * (C) 2006, Axios Engineering S.L. (Axios)
 * (C) 2006, County Council of Gipuzkoa, Department of Environment and Planning
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Axios BSD
 * License v1.0 (http://udig.refractions.net/files/asd3-v10.html).
 */
package org.locationtech.udig.tools.clip.internal.view;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.tools.geometry.clip.ClipStrategy;
import org.locationtech.udig.tools.geometry.internal.util.GeometryUtil;
import org.locationtech.udig.tools.internal.i18n.Messages;
import org.opengis.feature.Feature;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;

/**
 * A builder for a {@link Feature} that can be used to creates a new Feature
 * with geometry changed to be the clip result o the {@link #featureToClip} 
 * with all {@link #sourceFeatures}.
 * 
 * if the clipped geometry is needed then a call to {@link #buildClipGeometry()}
 * should be made.
 *  
 * </p>
 * <p>
 * Sample usage:
 * 
 * <pre>
 * &lt;code&gt;
 * FeatureCollection features = &lt;get desired source features&gt;...
 * Geometry clippedGeometry = &lt;get clipped geometry from features&gt;...
 * ClipFeatureBuilder builder = new ClipFeatureBuilder(features, union);
 * 
 * 
 * //get the resulting Feature
 * try{
 *  Feature clippedFeature = builder.buildClipFeature();
 * }catch(IllegalAttributeException e){
 *  //got an invalid attribute (may be a non nillable one got null?)
 *  LOGGER.log(Level.WARNING, &quot;Failed to create clip feature&quot;, e);
 * }
 * &lt;/code&gt;
 * </pre>
 * 
 * 
 * @author Nikolaos Pringouris <nprigour@gmail.com>
 */
class ClipFeatureBuilder {

	private static final double AREA_ERROR_MARGIN = 1.0E-6;
	private static final double LENGTH_ERROR_MARGIN = 1.0E-3;
	
	private SimpleFeatureType			featureType;
	
	/** maintains a list of features. A feature cannot be two times in the list*/
	private List<SimpleFeature>			sourceFeatures = Collections.synchronizedList( new LinkedList<SimpleFeature>() );

	/** the attributes of the feature to clip (in other words the feature to update its geometry)*/
	private SimpleFeature				featureToClip;
	
	/** Geometry if it have a null value the build executes the union operation in the source features*/
	private int							defaultGeometryIndex;
	
	/** the layer where the features will be clip (or working layer)*/
	private ILayer						layer;
	
	
	/**
	 * Creates a ClipFeatureBuilder that works over the provided 
	 * features and their geometries.
	 * @param layer The layer which contains the features.
	 */
	public ClipFeatureBuilder(ILayer layer) {
		
		assert layer != null;

		this.layer = layer;
		this.featureType = layer.getSchema();

		GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
		if( geometryDescriptor == null){
			throw new IllegalStateException( "The layer schema does not contain a geometry descriptor"); //$NON-NLS-1$
		}
		this.defaultGeometryIndex = featureType.indexOf(geometryDescriptor.getName());		
	}
	
	/**
	 * Checks all features have got the same feature type
	 * @param sourceFeatures
	 * @return
	 */	
	private boolean compatibleFeatureType(List<SimpleFeature> sourceFeatures) {

		this.featureType = sourceFeatures.get(0).getFeatureType();

		for (int i = 0; i < sourceFeatures.size(); i++) {
			SimpleFeature next = sourceFeatures.get(i);
			if (featureType != next.getFeatureType()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Adds the feature list to the existent source features
	 * @param featureList
	 */
	public void addSourceFeature(List<SimpleFeature> featureList) {
		
		for (SimpleFeature feature : featureList) {
			addSourceFeature(feature);
		}
		
	}
	
	/**
	 * Adds the feature as last element. If the feature is present in the list it won't be added.
	 * 
	 * @param feature
	 * @return return the position of the added feature.  -1 will be returned because the feature had added previously.
	 */
	public int addSourceFeature(SimpleFeature feature) {
		
		if(this.sourceFeatures.contains(feature)){
			return -1;
		}
		
		assert canClip(feature): "this precondition should be evaluated before call this method"; //$NON-NLS-1$
		
		this.sourceFeatures.add(feature);

		assert compatibleFeatureType(sourceFeatures):"Features in the collection must conform to a common schema"; //$NON-NLS-1$

		return this.sourceFeatures.size()-1;
	}



	/**
	 * Builds the clipped {@link Feature} by adapting its geometry.
	 * Note that the clip process does not use this method since 
	 * geometry is directly set to the {@link #featureToClip}
	 * 
	 * 
	 * @return a new {@link Feature} of the same {@link FeatureType} than the
	 *         provided source feature, with updated geometry
	 * @throws IllegalStateException
	 *             if it is not possible to create the Feature with the current
	 *             list of attribute values
	 */
	public SimpleFeature buildClippedFeature() throws IllegalStateException {
		SimpleFeature feature;
		try {

			// creates a copy of the feature so that the initial one is not affected
			feature = SimpleFeatureBuilder.copy(featureToClip);

			// build the geometry
			Geometry geometry = buildClipGeometry();
			feature.setDefaultGeometry(geometry);

		} catch (IllegalAttributeException e) {
			throw new IllegalStateException("Can't create clipped feature: " + e.getMessage(), e); //$NON-NLS-1$
		}
		return feature;
	}


	/**
	 * @return the attribute index of the default geometry
	 */
	public int getDefaultGeometryIndex() {
		assert defaultGeometryIndex >= 0;
		return defaultGeometryIndex;
	}



	/**
	 * Returns the attribute value at index <code>attributeIndex</code>of the
	 * source feature at index <code>featureIndex</code>
	 * 
	 * @param featureIndex
	 *            the index of the feature to retrieve the attribute from
	 * @param attributeIndex
	 *            the index of the attribute to retrieve from the feature at
	 *            index <code>featureIndex</code>
	 * @return the attribute value at index <code>attributeIndex</code>of the
	 *         source feature at index <code>featureIndex</code>
	 */
	public Object getAttribute(int featureIndex, int attributeIndex) {

		assert featureIndex < getFeatureCount();
		assert attributeIndex < getAttributeCount();

		SimpleFeature feature = getFeature(featureIndex);
		Object attribute = feature.getAttribute(attributeIndex);
		return attribute;
	}

	/**
	 * @param attIndex
	 *            the index of an attribute in the builder's {@link FeatureType}
	 * @return the name of the attribute at index <code>attIndex</code>
	 */
	public String getAttributeName(int attIndex) {

		assert attIndex < getAttributeCount();

		AttributeDescriptor attributeType = featureType.getDescriptor(attIndex);

		return attributeType.getLocalName();
	}
	
	/**
	 * @return the number of source features available to select target feature
	 *         attributes from
	 */
	public int getFeatureCount() {

		return sourceFeatures.size();
	}

	/**
	 * @return the number of attributes defined in the shared
	 *         {@link FeatureType} for the source features and the one to be
	 *         created
	 */
	public int getAttributeCount() {

		return featureType.getAttributeCount();
	}

	/**
	 * Returns the feature ID of the source feature at index
	 * <code>featureIndex</code>
	 * 
	 * @param featureIndex
	 * @return
	 */
	public String getID(int featureIndex) {

		assert featureIndex < getFeatureCount();

		SimpleFeature feature = getFeature(featureIndex);
		return feature.getID();
	}



	public SimpleFeature getFeature(int featureIndex) {

		SimpleFeature feature = sourceFeatures.get(featureIndex);

		return feature;
	}

	/**
	 * Get the layer.
	 * 
	 * @return The layer where the features are.
	 */
	public ILayer getLayer() {

		return this.layer;
	}


	
	public List<SimpleFeature> getSourceFeatures(){
		
		List<SimpleFeature> clone = new LinkedList<SimpleFeature>(this.sourceFeatures);
		return clone;
	}

	public SimpleFeature getFeatureToClip(){
		return featureToClip;
	}

	public void setFeatureToClip(SimpleFeature feature){
		this.featureToClip = feature;
	}
	
	
	public void removeFromSourceFeatures(SimpleFeature selectedFeature) {
		
		sourceFeatures.remove(selectedFeature);
	}
	public synchronized void removeFromSourceFeatures(List<SimpleFeature> featureList) {
		
		sourceFeatures.removeAll(featureList);
	}
	/**
	 * Used by ClipView to remove all features before adding new collection (while in Operation Mode)
	 */
        public synchronized void removeFromSourceFeaturesAll() {
            
            sourceFeatures.clear();
        }

	/**
	 * Checks if the feature's geometry fulfill the conditions to be added in the list of features to clip.
	 * 
	 * <lu>
	 * <li>should be compatible with the layer geometry.</li>
	 * <li>Multipolygon, MultiLineString, MultiPoint can be clip always.
	 * <li>Polygon, LineString, Point can be clip if and only if they intersect.<li>
	 * </lu>
	 * 
	 * @param newFeature
	 * @return true if the feature can be clip.
	 */
	public boolean canClip(SimpleFeature newFeature) {

		if(this.sourceFeatures.isEmpty()){
			return true; // this is the first feature, so there is nothing to check.
		}
		Geometry defaultGeometry = (Geometry) newFeature.getDefaultGeometry();
		assert defaultGeometry != null:"the feature " + newFeature.getID() + " has not geometry!";  //$NON-NLS-1$//$NON-NLS-2$


		// "Multi" geometries could be clip always.
		Class<? extends Object> geomClass = (defaultGeometry != null)? defaultGeometry.getClass(): Object.class;
		
		Class<? extends Geometry> layerGeometryClass = 
				(Class<? extends Geometry>) layer.getSchema().getGeometryDescriptor().getType().getBinding();
		if(!layerGeometryClass.isAssignableFrom( geomClass) ){
			return false; 
		}
		if(GeometryCollection.class.isAssignableFrom(geomClass)){
			return true;
		}
		assert (defaultGeometry instanceof Polygon) || (defaultGeometry instanceof LineString) || (defaultGeometry instanceof Point);
		
		// Simple geometries should intersects.
		for (SimpleFeature sourceFeature : this.sourceFeatures) {
			Geometry sourceGeometry = (Geometry) sourceFeature.getDefaultGeometry();
			
			if(defaultGeometry.intersects(sourceGeometry) )
				return true;
		}
		return false;
	}


	/**
	 * Builds the clip geometry. If any geometry was set in the clip feature, this
	 * method will build a new one executing the proper clip operation.
	 *  
	 * @return a Geometry for the clip feature.
	 */
	public Geometry buildClipGeometry() {
		Geometry resultGeom = null;
		Geometry clippedGeom =  (Geometry) featureToClip.getDefaultGeometry();
		
		resultGeom = (Geometry) clippedGeom.clone();
		for (SimpleFeature feature : sourceFeatures) {
			//ignore feature if same as featureToClip
			if (featureToClip.getIdentifier().equals(feature.getIdentifier())) {
				continue;
			}
			resultGeom = ClipStrategy.clipOp(resultGeom, (Geometry) feature.getDefaultGeometry());
		}
			
		
		List<Geometry> geomArray = new ArrayList<Geometry>();
		int diffs = checkAndClearGeometryElements(resultGeom, geomArray);
		if (diffs > 0 
				&& showMessageWithFeedback(Display.getDefault(), null, 
						MessageFormat.format(Messages.ClipFeatureBuilder_dialog_clear_msg, diffs))) {
			if (geomArray.size() == 0) {
				resultGeom = GeometryUtil.adapt(new GeometryFactory().createPolygon(), clippedGeom.getClass());
			} else {
				resultGeom = GeometryUtil.adapt((ArrayList<Geometry>)geomArray, clippedGeom.getClass());
			}
		}

		return resultGeom;
	}

	
	
	/**
	 * Returns  a number > 0 if clear operation has apply otherwise 0. The result array will contain the
	 * elements of the geometry after removal of spurious entries. The method applies only in elements of 
	 * dimension type 1 & 2.
	 *  
	 * @param resultGeom
	 * @param resultArray
	 *  
	 * @return
	 */
	private int checkAndClearGeometryElements(Geometry resultGeom, List<Geometry> resultArray) {
		
		if (resultGeom == null || resultGeom.isEmpty()) {
			return 0;
		} 
		
		Class<? extends Geometry> resultGeomClass = resultGeom.getClass();
		int dimension = 0;
		try {
			dimension = GeometryUtil.getDimension(resultGeomClass);
		} catch (Exception e) {
			e.printStackTrace();
		}
		int num = resultGeom.getNumGeometries();

		if (dimension == 2) {
			for (int i = 0; i < num; i++) {
				Geometry geomEntry = resultGeom.getGeometryN(i);
				if (geomEntry.getArea() > AREA_ERROR_MARGIN) {
					resultArray.add(geomEntry);
				}
			}
		} else if (dimension == 1) {
			for (int i = 0; i < num; i++) {
				Geometry geomEntry = resultGeom.getGeometryN(i);
				if (geomEntry.getLength() > LENGTH_ERROR_MARGIN) {
					resultArray.add(geomEntry);
				}
			}
		} else {
			return 0;
		}
		
		return num-resultArray.size();
	}

	
	/**
	 * @param resultGeom
	 * @return
	 */
	protected Geometry removeProblematicGeometryElements(Geometry resultGeom) {
		Class<? extends Geometry> resultGeomClass = resultGeom.getClass();
		if (GeometryUtil.getDimension(resultGeomClass) == 2) {
			int num = resultGeom.getNumGeometries();
			List<Geometry> geomArray = new ArrayList<Geometry>();
			for (int i = 0; i < num; i++) {
				Geometry geomEntry = resultGeom.getGeometryN(i);
				if (geomEntry.getArea() != 0) {
					geomArray.add(geomEntry);
				}
			}
			if (geomArray.size() == 0) {
				resultGeom = new GeometryFactory().createPolygon();
			}
			if (geomArray.size() != num) {
				resultGeom = GeometryUtil.adapt((ArrayList<Geometry>)geomArray, resultGeomClass);
			}
		} else if (GeometryUtil.getDimension(resultGeomClass) == 1) {
			int num = resultGeom.getNumGeometries();
			List<Geometry> geomArray = new ArrayList<Geometry>();
			for (int i = 0; i < num; i++) {
				Geometry geomEntry = resultGeom.getGeometryN(i);
				if (geomEntry.getLength() != 0) {
					geomArray.add(geomEntry);
				}
			}
			if (geomArray.size() == 0) {
				resultGeom = new GeometryFactory().createPolygon();
			}
			if (geomArray.size() != num) {
				resultGeom = GeometryUtil.adapt((ArrayList<Geometry>)geomArray, resultGeomClass);
			}
		}
		return resultGeom;
	}

	
	/**
	 * 
	 * @param display
	 * @param title
	 * @param msg
	 * @param type
	 * @return
	 */
	public static boolean showMessageWithFeedback( final Display display, final String title, final String msg) {

		final boolean[] result = new boolean[1];

		display.syncExec(new Runnable(){
			public void run() {
				result[0] = MessageDialog.openQuestion(display.getActiveShell(), title, msg);
			}
		});

		return result[0];
	}
}
