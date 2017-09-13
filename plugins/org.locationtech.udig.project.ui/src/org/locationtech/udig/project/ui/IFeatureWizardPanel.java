package org.locationtech.udig.project.ui;

import org.eclipse.swt.widgets.Composite;

/**
 * Further extends {@link IFeaturePanel2} to provide some extra methods to support/facilitate
 * incorporation of panels in WizardPages. 
 * 
 * @author nprigour
 */
public abstract class IFeatureWizardPanel extends IFeaturePanel2{
 
    ///////////////////////////////////////////////
    //this object holds a reference to the IFeaturePanel container object
    //(usually a WizardPage or ViewPart) to be used for a possible callback.
    private Object container;
    
	public Object getContainer() {
		return container;
	}
	
	/**
	 * Method to be used for setting the container. (Should be called early
	 * , preferably prior to {@link #createPartControl(Composite)})
	 * @param container
	 */
	public void setContainer(Object container) {
		this.container = container;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean canProceed() {
		return true;
	}
	
}
