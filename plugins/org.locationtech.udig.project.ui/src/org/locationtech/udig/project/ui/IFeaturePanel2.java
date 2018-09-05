package org.locationtech.udig.project.ui;

import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.udig.project.AdaptableFeature;
import org.locationtech.udig.project.IEditManager;
import org.locationtech.udig.project.ui.feature.EditFeature;

import org.eclipse.ui.PartInitException;

/**
 * Contribute a panel for editing a specific feature type to the user interface. This class
 * extends @link {@link IFeaturePanel} and provides the ability to set the feature to be edited
 * directly rather than obtaining it from the EditManager. It should be used mainly during 
 * direct feature creation (since in this case no feature is set to the edit manager).  
 * <p>
 * Panels are expected to be displayed in a view (using a series of tabs) and also in a dialog
 * or wizard page.
 * 
 * @author nprigour
 * 
 */
public abstract class IFeaturePanel2 extends IFeaturePanel{
	/**
	 * holds the feature to be created.
	 */
	private SimpleFeature feature;

	
	/**
	 * Returns the feature to be created (usually an instance of {@link AdaptableFeature}.  
	 * 
	 * @return SimpleFeature to be used
	 */
	public SimpleFeature getFeatureToCreate(){
		return feature;
	}
	/**
	 * Initializes the feature panel with the feature under creation. 
	 * <p>

	 * </p>
	 *
	 * @param feature Allows access to user interface facilities
	 * @param layer 
	 * @throws PartInitException 
	 */
	public void init(SimpleFeature feature) throws PartInitException{  
		if (feature == null) {
			throw new PartInitException("feature to be set is null");
		}
		this.feature = feature;
	}     

	/**
	 * helper method to obtain the feature to be manipulated. 
	 * Initially an {@link EditFeature}is looked through the {@link IFeatureSite}. This
	 * usually may return an {@link EditFeature}
	 * If not found then the {@link #getFeatureToCreate()} method is called (valid only for
	 * create panels).This usually returns a {@link AdaptableFeature}.
	 * 
	 * @return
	 */
	protected SimpleFeature obtainFeatureToManipulate() {
		//first check the feature attribute
		SimpleFeature feature = null;

		IFeatureSite site = getSite();   
		if( site != null ){
			IEditManager editManager = site.getEditManager();
			if( editManager != null ){
				feature = editManager.getEditFeature();
			}
		} 
		/*
		if (feature != null) {
			System.out.println("edit manager site returned a feature " + feature.getID());
		} else { 
			System.out.println("edit manager site returned no feature. Checking FeatureToCreate()." );
		}*/
		
		return feature != null ? feature : getFeatureToCreate();
	}

	//override for testing purposes only
	@Override
	public void aboutToBeShown() {
		System.out.println("aboutToBeShown CALLED. "  + getClass().getSimpleName());
	}

	@Override
	public void aboutToBeHidden() {
		System.out.println("aboutToBeHidden CALLED. "  + getClass().getSimpleName());
	}
}
