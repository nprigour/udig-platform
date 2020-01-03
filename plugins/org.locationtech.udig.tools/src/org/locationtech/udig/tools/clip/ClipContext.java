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
package org.locationtech.udig.tools.clip;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.ui.commands.SelectionBoxCommand;
import org.locationtech.udig.project.ui.tool.IToolContext;
import org.locationtech.udig.tools.clip.internal.view.ClipView;

import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Envelope;

/**
 * Stores the status values of clip interactions.
 * 
 * The inputs for the clip command are grabbed using different user interface techniques like
 * feature selection by bbox drawing and clip feature definition using {@link ClipView}. Thus,
 * this context object provides a site where the clip command's parameters are stored.
 * 
 * @author Nikolaos Pringouris <nprigour@gmail.com>
 */
public class ClipContext {

    private static final ClipContext THIS = new ClipContext();

    /**
     * maintains the interaction selected by the user for merging features 	
     */
	public static final int CLIPMODE_TOOL = 1;
    public static final int CLIPMODE_OPERATION = 2;
    private int clipMode; 


    private Point bboxStartPoint = null;

    private SelectionBoxCommand selectionBoxCommand = new SelectionBoxCommand();

    private List<Envelope> boundList = new ArrayList<Envelope>();

    private ClipView clipView = null;

    private IToolContext toolContext = null;

    private List<SimpleFeature> preSelectedFeatures = Collections.emptyList();
    private ILayer preSelectedLayer = null;

    /**
     * Singleton use the getInstance methods
     */
    private ClipContext() {
        // singleton
    }

    /**
     * Singleton
     * 
     * @return return the instance of {@link ClipContext}
     * 
     */
    public static ClipContext getInstance() {
        return THIS;
    }

    public IToolContext getToolContext() {
        return toolContext;
    }

    public void setToolContext(IToolContext toolContext) {
        this.toolContext = toolContext;
    }

    /**
     * Reinitializes the status context
     */
    public void initContext() {

        bboxStartPoint = null;
        selectionBoxCommand = new SelectionBoxCommand();

        clipView = null;
        toolContext = null;

        boundList.clear();
    }

    /**
     * Set the start point of the bbox.
     * 
     * @param point
     */
    public synchronized void setBBoxStartPoint(Point point) {

        assert point != null;

        this.bboxStartPoint = point;
    }

    /**
     * Returns the start point of the bbox.
     * 
     * @return the left upper corner
     */
    public synchronized Point getBBoxStartPoint() {

        return this.bboxStartPoint;
    }

    /**
     * Returns the instance of {@link SelectionBoxCommand} maintained in this context.
     * 
     * @return {@link SelectionBoxCommand}
     */
    public SelectionBoxCommand getSelectionBoxCommand() {

        return selectionBoxCommand;
    }

    /**
     * Add an bound to the envelope list.
     * 
     * @param bound
     */
    public void addBound(Envelope bound) {

        assert bound != null;

        boundList.add(bound);
    }

    /**
     * Removes the indeed bound from the list of bounds.
     * 
     * @param bound
     */
    public void removeBound(Envelope bound) {

        assert bound != null;

        boundList.remove(bound);
    }

    /**
     * @return the list of bounds
     */
    public List<Envelope> getBoundList() {

        return boundList;
    }

    /**
     * 
     * @return the associated clip view
     */
    public ClipView getClipView() {
        return clipView;
    }

    /**
     * 
     * @return true If a clip view is opened, false in other case
     */
    public boolean isClipViewActive() {

        return (clipView != null) && !clipView.isDisposed();
    }

    /**
	 * 
	 */
    public void disposeClipView() {

        this.clipView = null;
    }

    /**
     * Set the associated clip view
     */
    public void activeClipView(ClipView view) {
        this.clipView = view;

    }

    /**
     * Set the mode in which the tool operates: Used by ClipOperatio.op Used by ClipTool.
     * 
     * @param mode
     */
    public void setClipMode(int mode) {
        clipMode = mode;
    }

    /**
     * Return the mode in which the tool is operating Tool mode = 1 (selection by Clip Tool)
     * Operation mode = 2 (with listeners on layers)
     * 
     * @return
     */
    public  int getClipMode() {
        return clipMode;
    }

    /**
     * Add pre-selected features to ClipContext class It is used by ClipOperation to store
     * features eventually pre-selected by the user before issuing the UI "Operation -> Clip"
     * command. These features are added to ClipView on opening when running in 'operation mode'
     * 
     * @param preSelectedFeatures
     */
    public void addPreselectedFeatures(List<SimpleFeature> preSelectedFeatures, ILayer preSelectedLayer) {
        this.preSelectedFeatures = preSelectedFeatures;
        this.preSelectedLayer = preSelectedLayer;
    }

    /**
     * Returns pre selected features
     * 
     * @return List<SimpleFeature>. Can be null if no features have been preselected on
     *         ClipOperation launch or if pre-selected features have been cleared after once-only
     *         addition to ClipView
     */
    public List<SimpleFeature> getPreselectedFeatures() {
        return preSelectedFeatures;
    }
    
    public ILayer getPreSelectedLayer() {
        return preSelectedLayer;
    }

    /**
     * Clear list of pre-selected features These features are added by ClipOperatio upon tool
     * activation and are cleared once added to ClipView
     */
    public void clearPreselectedFeatures() {
        preSelectedFeatures.clear();
    }
}