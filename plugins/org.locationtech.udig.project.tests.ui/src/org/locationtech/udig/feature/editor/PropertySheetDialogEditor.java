package org.locationtech.udig.feature.editor;


import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.udig.feature.panel.PropertySheetPanelDefault;
import org.locationtech.udig.project.ui.IUDIGDialogPage;
import org.locationtech.udig.project.ui.tool.IToolContext;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PartInitException;


/**
 * This is an example of Dialog popup feature Editor that wraps a FeaturePanel
 * for displaying the edited feature. This example can server as a reference on
 * how to utilize feature panels for editing feature thus providing consistency
 * between feature editing and feature creation.  
 * 
 * @author nprigour
 */
public class PropertySheetDialogEditor implements IUDIGDialogPage {
	
	private IToolContext context;

	private SimpleFeature feature;
	
	private PropertySheetPanelDefault featureDisplay;
	
	/**
	 * Default constructor.
	 */
	public PropertySheetDialogEditor() {
		featureDisplay = new PropertySheetPanelDefault();
	}
	
	/**
	 * @see net.refractions.udig.project.ui.IUDIGView#setContext()
	 */
	public void setContext(IToolContext context) {
		this.context = context;
	}

	/**
	 * @see net.refractions.udig.project.ui.IUDIGView#getContext()
	 */
	public IToolContext getContext() {
		return context;
	}

	/**
	 * @see net.refractions.udig.project.ui.IUDIGDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		try {
			//initialize the feature
			featureDisplay.init(feature);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		//create the control
		featureDisplay.createPartControl(parent);
		//this method initialize the model data
		featureDisplay.aboutToBeShown();
	}

	public Point getPreferredSize() {
		return new Point(500, 600);
	}

	public Control getControl() {
		return featureDisplay.getFeatureDisplay().getControl();
	}

	public void setFeature(SimpleFeature feature) {
		this.feature = feature;
	}

	@Override
	public boolean performCompleteAction() {
		return true;
	}

	@Override
	public boolean performCancelAction() {
		return true;
	}

}
