package org.locationtech.udig.feature.panel;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;

import org.locationtech.udig.project.ui.IFeatureSite;
import org.locationtech.udig.project.ui.IFeatureWizardPanel;
import org.locationtech.udig.project.ui.internal.properties.FeaturePropertySource;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * Default implementation of IFeaturePanel that uses an eclipse PropertySheetPage
 * that is bound to a {@link FeaturePropertySource} for displaying the {@link SimpleFeature}
 * that is set via the {@link #init(IFeatureSite, org.eclipse.ui.IMemento)} method.  
 * 
 */
public class PropertySheetPanelDefault extends IFeatureWizardPanel {

	//Log4J logger
    private static Logger log = Logger.getLogger(PropertySheetPanelDefault.class);
    
    //the feature display is a container for a propertySheet page
	protected PropertySheetPage featureDisplay;
	
		
	/**
	 * Default constructor. A zero argument constructor is needed since the panel will
	 * be instantiated as a {@link IContributionItem} 
	 */
	public PropertySheetPanelDefault() {
		super();
	}

	@Override
	public String getName() {
		return "Feature Editor ";
	}

	@Override
	public String getTitle() {
		return "Default Feature Editor";
	}

	@Override
	public String getDescription() {

		return "Default Feature Panel creator based on a PropertySheet Page";
	}
 
    /**
     * Creates the UI elements
     */
	@Override
	public void createPartControl(Composite parent) {

		featureDisplay = new PropertySheetPage() {
			public void handleEntrySelection(ISelection selection) {
				super.handleEntrySelection(selection);
				if (getContainer() != null) {
					((WizardPage)getContainer()).getWizard().getContainer().updateButtons();
				}
			}
		};
		featureDisplay.createControl(parent);
		featureDisplay.getControl().setLayoutData(parent.getLayoutData());
		
		//Add a MouseHover listener to display additional information about 
		//an attribute's binding when the user hovers the mouse over an item.
		featureDisplay.getControl().addListener(SWT.MouseHover, new Listener () {
			
			@Override
			public void handleEvent (Event event) {

				Point pt = new Point (event.x, event.y);	
				if (event.widget instanceof Tree) {
					Tree tree = (Tree)(event.widget);
					TreeItem item = tree.getItem(pt);
					if (item != null) {
						for (AttributeDescriptor attr : obtainFeatureToManipulate().getFeatureType().getAttributeDescriptors()) {
							if (attr.getLocalName().equalsIgnoreCase(item.getText())) {
								tree.setToolTipText(attr.getType().getBinding().getSimpleName());
								return;
							} 
						}
					}
				}
			}
		});

		//Add a listener that listens for low level paint event. This is the only 
		//way to provide a visual feedback to the user indicating that a value
		//is required since the LabelProvider of a PropertySheetEntry does not 
		//provide a way to set the background color.
		featureDisplay.getControl().addListener(SWT.PaintItem, new Listener() {

			@Override
			public void handleEvent(Event event) {
				TreeItem item = (TreeItem)event.item;
								
				//change the cell color only to indicate not nullable attributes that require
				//a value to be specified. The loop, although not very efficient, is necessary 
				//since obtaining directly the descriptor by name is problematic due to case 
				//sensitivity issues. 
				for (AttributeDescriptor attr : obtainFeatureToManipulate().getFeatureType().getAttributeDescriptors()) {
					if (attr.getLocalName().equalsIgnoreCase(item.getText()) && !attr.isNillable()
							&& StringUtils.isEmpty(obtainFeatureToManipulate().
									getAttribute(attr.getLocalName()) != null ? obtainFeatureToManipulate().
									getAttribute(attr.getLocalName()).toString() : null)) {
						item.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
						return;
					} 
				}
				
				//set default background color if we reach here.
				item.setBackground(null);
				
				/*
				SimpleFeatureType type = obtainFeatureToManipulate().getFeatureType();
				AttributeDescriptor descriptor = obtainFeatureToManipulate().getFeatureType().getDescriptor(item.getText().toLowerCase());
				if (descriptor != null && !descriptor.isNillable() 
						&& StringUtils.isEmpty(obtainFeatureToManipulate().getAttribute(item.getText().toLowerCase()).toString())) {
					item.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
				} else {
					item.setBackground(null);
				}
				*/
			}
		});
		
		//also initialize the data model 
		//(moved to aboutToBeShown() method)
	
        //make the page complete by default so that finish button is always enabled 
		//this is needed in case the Panel is the only one available.
		//featureDisplay.setPropertySourceProvider(new FeaturePropertySourceProvider());
		
        if (getContainer() instanceof WizardPage) {
        	((WizardPage)getContainer()).setPageComplete(true);
        }
	}


	@Override
	public void refresh() {
		featureDisplay.refresh();
	}
	
    public boolean controlsHaveBeenCreated() {
        return featureDisplay != null && featureDisplay.getControl().isDisposed() == false;
    }
    
    /**
     * Prior to showing the panel refresh the data model to be shown.
     * (non-Javadoc)
     * @see net.refractions.udig.project.ui.IFeaturePanel#aboutToBeShown()
     */
    public void aboutToBeShown() {   	
    	System.out.println("aboutToBeShown CALLED. "  + getClass().getSimpleName());
    	//featureDisplay.refresh();
		SimpleFeature feature = obtainFeatureToManipulate();
		FeaturePropertySource source = new FeaturePropertySource( feature, false, true);
        StructuredSelection sel = new StructuredSelection(source);
        featureDisplay.selectionChanged(null, sel);
    } 
    
    /*
     * (non-Javadoc)
     * @see net.refractions.udig.project.ui.IFeaturePanel#aboutToBeHidden()
     */
    public void aboutToBeHidden() {
    	System.out.println("aboutToBeHidden CALLED. "  + getClass().getSimpleName());
    	//do nothing 
    } 
    
    /*
     * (non-Javadoc)
     * @see net.refractions.udig.project.ui.IFeaturePanel#dispose()
     */
    public void dispose(){
    	System.out.println("dispose CALLED. "  + getClass().getSimpleName());
    	if (featureDisplay != null) {
    		featureDisplay.dispose();
    		featureDisplay = null;
    	}
        super.dispose();
    }
    
    /**
     * checks for a name attribute in the feature under manipulation. If not found
     * then true is returned by default. However if a "name" attribute exists  a check 
     * takes place. 
     */
	@Override
	public boolean canProceed() {
		for (AttributeDescriptor attr : obtainFeatureToManipulate().getFeatureType().getAttributeDescriptors()) {
			if (attr.getLocalName().equalsIgnoreCase("name")) {
				return StringUtils.isNotBlank((String)obtainFeatureToManipulate().getAttribute(attr.getName()));
			}
		}
		//AttributeType attr = obtainFeatureToManipulate().getFeatureType().getType("name");
		//if (attr != null) {
		//	return StringUtils.isNotBlank((String)obtainFeatureToManipulate().getAttribute(attr.getName()));
		//}
		return true;
	}

	public PropertySheetPage getFeatureDisplay() {
		return featureDisplay;
	}    
    
}
