package org.locationtech.udig.project.ui.feature;

import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.IFeaturePanel;
import org.locationtech.udig.project.ui.IFeatureSite;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;

/**
 * The  class creates a wizard of Feature panels for the feature provided
 * as argument in the constructor. The panels are created based on the list
 * of {@link FeaturePanelEntry} that are also provided during construction time.
 *     
 * @author u06543
 *
 */
public class FeaturePanelWizard extends Wizard {

	//list of FeaturePanelEntry
	protected List<FeaturePanelEntry> panels;
	
	//the that will be used to retrieve the feature to be processed. 
	private IFeatureSite site;
	
	public static SimpleFeature feature;
	
	/**
	 * Constructor creates a default FeatureSite and set the passed feature arg to it.  
	 * @param panels
	 * @param feature
	 */
	public FeaturePanelWizard(List<FeaturePanelEntry> panels, SimpleFeature feature) {
		super();
		setNeedsProgressMonitor(true);
		this.panels = panels;
		site = new FeatureSiteImpl(ApplicationGIS.getActiveMap());
		((FeatureSiteImpl)site).setFeature(feature);
		this.feature = feature;
	}



	/**
	 * Method used to create a wizard page for each FeaturePanelEntry.
	 * The page complete value is not set (by default true)
	 */
	@Override
	public void addPages() {

		for (FeaturePanelEntry panel : panels) {
			FeaturePanelWizardPage wizardPage = new FeaturePanelWizardPage(panel, site);
			//wizardPage.setPageComplete(true);
			addPage(wizardPage);
		}
	}


	/**
	 * On finish we should close the Wizard popup. 
	 * 
	 */
	@Override
	public boolean performFinish() {
		System.out.println("Finished pressed: " + site.getEditFeature());
		System.out.println(getContainer());
		System.out.println(getContainer().getShell());
		feature = null;
		//getContainer().getShell().dispose(); //TODO check whether this action can be moved at a higher level.
		return true;
	}

	//@Override
	public boolean performCancel() {
		System.out.println("Cancel pressed: " + site.getEditFeature());
		System.out.println(getContainer().getShell());
		feature = null;
		//getContainer().getShell().close(); //TODO check whether this action can be moved at a higher level.
		return super.performCancel();
	}	
	
	/**
	 * 
	 * @author u06543
	 *
	 */
	public static class FeaturePanelWizardPage extends WizardPage {

		private FeaturePanelEntry panel;
		private IFeatureSite site;
		private Composite container;


		protected FeaturePanelWizardPage(FeaturePanelEntry panel, IFeatureSite site) {
			super(panel.getName(), panel.getTitle(), null);
			this.panel = panel;
			this.site = site;

		}


		@Override
		public void createControl(Composite parent) {
		    container = new Composite(parent, SWT.BORDER);
		    GridLayout layout = new GridLayout();
		    
			container.setLayout(layout);
			container.setLayoutData(new GridData(GridData.FILL_BOTH));
			//GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);

			//Required to avoid an error in the system.
			setControl(container);

			IFeaturePanel featurePanel = panel.createFeaturePanel();
			try {
				featurePanel.init(site, null); //method should be called prior to anything else
				featurePanel.createPartControl(container); //create the part control
			} catch (PartInitException e) {
				e.printStackTrace();
			}
			
			
		} 
		
	}

}
