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

import java.util.List;

import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.tools.clip.internal.view.ClipView;
import org.locationtech.udig.ui.operations.IOp;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

/**
 * Supports an Operation-started Workflow for ClipTool usage.
 * 
 * User selects some feature and, through right-click, open the "Operation -> Clip selected" menu:
 * this calls the present class.
 * <p>
 * This class provides a starting point for opening the ClipView in the CLIPMODE_OPERATION
 * status. This field is stored in ClipContext that acts as a blackboard, and is retrieved
 * throughout the whole plug-in whenever a difference in tool behaviour has been introduced
 * to support the operation-mode workflow (against the 'classic' ClipTool one)
 * </p>
 * 
 * @author Nikolaos Pringouris <nprigour@gmail.com>
 */
public class ClipOperation implements IOp {
    
    private ClipView clipView;

    @Override
    public void op(final Display display, Object target, IProgressMonitor monitor) throws Exception {
        
        //final FeatureSource  preSelectedLayer = (FeatureSource) target;

        Thread t = new Thread() {

            public void run() {
                try {
                    // Set tool mode (also set in ClipTool.setContext to CLIPMODE_TOOL) 
                    ClipContext clipContextSingleton = ClipContext.getInstance();
                    clipContextSingleton.setClipMode(ClipContext.CLIPMODE_OPERATION);
                    // Store eventual pre-selected features for later display in ClipView
                    ILayer preSelectedLayer = ApplicationGIS.getActiveMap().getEditManager().getSelectedLayer();
                    Filter preSelectedFilter = preSelectedLayer.getFilter();
                    if ( preSelectedFilter != Filter.EXCLUDE){
                        List<SimpleFeature> preSelectedFeatures = Util.retrieveFeatures(preSelectedFilter, preSelectedLayer);
                        clipContextSingleton.addPreselectedFeatures(preSelectedFeatures, preSelectedLayer);
                    }
                    
                    // Open view
                    clipView = (ClipView) ApplicationGIS.getView(true, ClipView.ID);
                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        display.asyncExec(t);

    }
}
