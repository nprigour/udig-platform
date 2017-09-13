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
package org.locationtech.udig.tools.geometry.clip;

import org.locationtech.udig.tools.geometry.internal.util.GeometryUtil;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;

/**
 * <p>
 * 
 * <pre>
 * Strategy for clipping of geometries.
 * 
 * The clipped geometry is computed as the difference between the 2 geometries. 
 * The resulting geometry is adapted to the type of the initial geometry depending 
 * on the shouldAdapt flag.
 * Usually the method should apply between features of the same type.  
 * </pre>
 * 
 * </p>
 * 
 * @author Nikolaos Pringouris <nprigour@gmail.com>
 * @since 1.4.0
 */
public class ClipStrategy {

	/* the geometry to clip. */
	private Geometry	clipGeometry	= null;

	private boolean shouldAdapt = true;

	/**
	 * Default constructor.
	 * 
	 * Stores one of the geometries you want to clip.
	 * 
	 * @param clipGeometry
	 *            One of the geometries to clip.
	 */
	public ClipStrategy(Geometry clipGeometry) {

		this(clipGeometry, true);
	}
	

	/**
	 * 
	 * Stores one of the geometries you want to clip.
	 * 
	 * @param clipGeometry
	 *            One of the geometries to clip.
	 * @param shouldAdapt whether the result should be adapted to 
	 * 		the source geometry
	 */
	public ClipStrategy(Geometry clipGeometry, boolean shouldAdapt) {

		if (clipGeometry == null) {
			throw new NullPointerException();
		}

		this.clipGeometry = clipGeometry;
		this.shouldAdapt = shouldAdapt;
	}

	/**
	 * Realize clip operation between the given geometries.
	 * 
	 * @param clipGeometry
	 *            The first geometry to clip.
	 * @param withGeometry
	 *            The second geometry to clip.
	 * @return The merged geometry.
	 */
	public static Geometry clipOp(Geometry clipGeometry, Geometry withGeometry) {

		ClipStrategy strategy = new ClipStrategy(clipGeometry);

		Geometry clipped = strategy.clip(withGeometry);
		return clipped;
	}

	/**
	 * Realize the clip between the clipGeometry (given on the constructor)
	 * and this geometry.
	 * 
	 * @param withGeometry
	 *            Geometry to clip with.
	 * @return The merged geometry.
	 */
	public Geometry clip(Geometry withGeometry) {

		if (withGeometry == null) {
			throw new NullPointerException();
		}

		Geometry clipResult = null;

		if (clipGeometry.getClass().equals(GeometryCollection.class)) {
			//clipping of geometry collections not supported.
		} else {
			clipResult = clipGeometry.difference(withGeometry);
		}

		if (shouldAdapt) {
			clipResult = GeometryUtil.adapt(clipResult, clipGeometry.getClass());
		}
		return clipResult;
	}

}
