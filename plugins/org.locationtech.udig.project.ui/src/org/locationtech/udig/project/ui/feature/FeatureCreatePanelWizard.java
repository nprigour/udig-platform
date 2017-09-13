package org.locationtech.udig.project.ui.feature;

import java.util.List;

import org.apache.log4j.Logger;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.udig.project.AdaptableFeature;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.ui.IFeaturePanel;
import org.locationtech.udig.project.ui.IFeaturePanel2;
import org.locationtech.udig.project.ui.IFeatureWizardPanel;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * The  class creates a wizard of FeaturePanels for the feature provided
 * as argument in the constructor. The panels are created based on the list
 * of {@link FeaturePanelEntry} that are also provided during construction time.
 *     
 * @author u06543
 *
 */
public class FeatureCreatePanelWizard extends Wizard implements IPageChangingListener {

	//Log4J logger
    private static Logger log = Logger.getLogger(FeatureCreatePanelWizard.class);
    
	//list of FeaturePanelEntry
	protected List<FeaturePanelEntry> panels;
	
	//feature to be created
	protected SimpleFeature featureToCreate;
		
	/**
	 * Constructor creates a default FeatureSite and set the passed feature arg to it.  
	 * @param panels
	 * @param feature
	 */
	public FeatureCreatePanelWizard(List<FeaturePanelEntry> panels, SimpleFeature feature) {
		super();
		setNeedsProgressMonitor(true);
		this.panels = panels;
		this.featureToCreate = feature;
	}


	/**
	 * Method used to create a wizard page for each FeaturePanelEntry.
	 */
	@Override
	public void addPages() {
		for (FeaturePanelEntry panel : panels) {
			if (panel.isCreateFeature()) {
				FeaturePanelWizardPage wizardPage = new FeaturePanelWizardPage(
						panel, featureToCreate);
				//wizardPage.setPageComplete(false); //by default all pages are set to not complete
				addPage(wizardPage);
			}
		}
	}

	/**
	 * On finish we should try to refresh all wizard pages.
	 * Also call aboutToBeHidden for the current page.
	 * 
	 */
	@Override
	public boolean performFinish() {
		System.out.println("Finished pressed: " + featureToCreate);
		
		((FeaturePanelWizardPage)getContainer().getCurrentPage()).getFeaturePanel().aboutToBeHidden();
		
		for (IWizardPage page : getPages()) {
			((FeaturePanelWizardPage)page).getFeaturePanel().refresh();
		}
		return true;
	}

	//@Override
	public boolean performCancel() {
		System.out.println("Cancel pressed: " + featureToCreate);
		return super.performCancel();
	}	

	@Override
	public void handlePageChanging(PageChangingEvent event) {
		System.out.println("EVENT:" + event.getCurrentPage() + " " + event.getTargetPage());
		((FeaturePanelWizardPage)event.getCurrentPage()).featurePanel.aboutToBeHidden();
		((FeaturePanelWizardPage)event.getTargetPage()).featurePanel.aboutToBeShown();
	}
	
	/**
	 * Wraps a IFeaturePanel around a WizardPage.
	 * @author u06543
	 *
	 */
	public class FeaturePanelWizardPage extends WizardPage {

		//private FeaturePanelEntry panel;
		
		//feature to be updated 
		private SimpleFeature feature;
		
		//container of each feature panel
		private Composite container;
		
		//embedded feature panel 
		private IFeaturePanel featurePanel;

		/**
		 * Constructor. 
		 * 
		 * @param panel  the FeaturePanelEntry that maintains all information
		 * 				about the IFeaturePanel that will be instantiated.
		 * @param feature a reference to an object that extends SimpleFeature 
		 * 				(usually an @link {@link AdaptableFeature}) 
		 */
		protected FeaturePanelWizardPage(FeaturePanelEntry panel, SimpleFeature feature) {
			super(panel.getName(), panel.getTitle(), null);
			//this.panel = panel;
			this.feature = feature;
			featurePanel = panel.createFeaturePanel();
			
			//set the text and icon at the title bar
			if (panel.getLabelProvider() != null) {
				//System.out.println("getLabelProvider for " + panel.getName() + "( " + panel.getLabelProvider().getText(feature) + ")");
				setTitle(panel.getLabelProvider().getText(feature) + " (" + getTitle() + ")" );
				if (panel.getLabelProvider().getImage(feature) != null) {
					setImageDescriptor(ImageDescriptor.createFromImage(panel.getLabelProvider().getImage(feature)));
				}
			}
		}


		/**
		 * Creates, Initializes and sets the controls of the FeaturePanel. 
		 */
		@Override
		public void createControl(Composite parent) {
		    container = new Composite(parent, SWT.BORDER);
		    GridLayout layout = new GridLayout();
		    
			container.setLayout(layout);
			container.setLayoutData(new GridData(GridData.FILL_BOTH));
			//GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);

			//Required to avoid an error in the system.
			setControl(container);
			
			try {
				//it is expected that the feature panel will be of type IFeaturePanel2
				//if not a runtime exception will occur
				
				//1. Call all init() methods prior to creating the controls
				if (feature instanceof IAdaptable) {
					featurePanel.init(new FeatureSiteImpl(
							(ILayer)((IAdaptable) feature).getAdapter(ILayer.class)), null);
				}
				//call this only for IFeatureCreatePanels
				if (featurePanel instanceof IFeaturePanel2) {
					((IFeaturePanel2)featurePanel).init(feature);
				}
				
				//2. set the container
				if (featurePanel instanceof IFeatureWizardPanel) {
					((IFeatureWizardPanel)featurePanel).setContainer(this); 
					//setPageComplete(false); 
				}
				
				//3. set page complete flag to true since non- IFeatureWizardPanel 
				//panels do not have access to the WizardPage object and thus 
				//they are unable to change their state. IFeatureWizardPanel can
				//override this be calling their container.setPageComplete(false);
				//method  while in createPartControl.
				setPageComplete(true); 
				
				//4. create the part control
				featurePanel.createPartControl(container); 
				
				//we set the complete flag so that finish and next buttons are enabled by default
				//this is necessary in the current implementation since the created panels do
				//not have access to the FeaturePanelWizardPage that encompass them. TODO
				//setPageComplete(false);
			} catch (Exception e) {
				e.printStackTrace();
			}		
			
		} 

		/**
		 * re-implement set visible so that the appropriate methods of featurePanel are called.
		 */
		@Override
		public void setVisible(boolean visible) {
			System.out.println("SET VISIBLE CALLED for " + getClass().getName() + "(" + visible + ")");
			super.setVisible(visible);
			//special case not handled by the Wizard IPageChangingListener 
			//(the first time the wizard is spawn).
			if (visible && getWizard().getStartingPage().equals(this)) {
				featurePanel.aboutToBeShown();
			} 
			
		}

		/**
		 * 
		 */
	    public boolean canFlipToNextPage() {
	    	boolean canProceed = (featurePanel instanceof IFeatureWizardPanel ? 
    				((IFeatureWizardPanel)featurePanel).canProceed() : true);
	        return super.canFlipToNextPage() && canProceed;
	    }

		/**
		 * 
		 */
	    public boolean isPageComplete() {
	    	boolean canProceed = (featurePanel instanceof IFeatureWizardPanel ? 
    				((IFeatureWizardPanel)featurePanel).canProceed() : true);
	        return super.isPageComplete() && canProceed;
	    }
	    
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
		 */
	    public void dispose() {
	    	featurePanel.dispose();
	        super.dispose();
	    }
	    
		/**
		 * returns the IFeaturePanel.
		 * 
		 * @return
		 */
		public IFeaturePanel getFeaturePanel() {
			return featurePanel;
		}
		
	}


}
