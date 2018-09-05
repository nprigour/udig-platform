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

import java.util.Arrays;

import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.command.MapCommand;
import org.locationtech.udig.project.command.UndoableCommand;
import org.locationtech.udig.project.command.UndoableComposite;
import org.locationtech.udig.project.command.UndoableMapCommand;
import org.locationtech.udig.project.command.factory.EditCommandFactory;
import org.locationtech.udig.project.internal.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Clip Command Factory
 * <p>
 * This is a wrapper over UndoableComposite which is responsible to construct
 * the command list required to create the clipped feature. 
 * </p>
 * 
 * @author Nikolaos Pringouris <nprigour@gmail.com>
 * @since 1.1.0
 */
final class ClipFeaturesCommand implements UndoableMapCommand {

	private UndoableComposite	compositeCmd		= null;

	public static ClipFeaturesCommand getInstance(	ILayer layer, SimpleFeature clipdFeature, Geometry clippedGeometry) {

		return new ClipFeaturesCommand(layer, clipdFeature, clippedGeometry);
	}

	/**
	 * Creates the set of commands required to delete the selected features 
	 * and add a new the new feature (clip feature).
	 *   
	 * @param layer
	 * @param sourceFeatures selected features
	 * @param clipdFeature	 the new feature
	 */
	private ClipFeaturesCommand(	final ILayer layer,
									final SimpleFeature featureToClip,
									final Geometry clippedGeometry) {

		assert layer != null;
		assert featureToClip != null;

		// creates the command to delete selected features

		final EditCommandFactory cmdFactory = EditCommandFactory.getInstance();

		UndoableCommand cmd = null;
		if (clippedGeometry == null || clippedGeometry.isEmpty()) {
			cmd = cmdFactory.createDeleteFeature(featureToClip, layer);
		} else {
			cmd = cmdFactory.createSetGeomteryCommand(
					featureToClip, layer, clippedGeometry);
		}

		compositeCmd = new UndoableComposite(Arrays.asList(cmd));
	}

	
	public void rollback(IProgressMonitor monitor) throws Exception {

		this.compositeCmd.rollback(monitor);
	}

	public MapCommand copy() {
		return this.compositeCmd.copy();
	}

	public String getName() {
		return "Clip Features Command"; //$NON-NLS-1$
	}

	public void setMap(final IMap map) {
		this.compositeCmd.setMap(map);
	}

	public Map getMap() {
		return this.compositeCmd.getMap();
	}

	public void run(IProgressMonitor monitor) throws Exception {
		this.compositeCmd.run(monitor);
	}

}
