/* uDig - User Friendly Desktop Internet GIS client
 * http://udig.refractions.net
 * (C) 2004-2011, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.tools.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.locationtech.udig.tools.Activator;
import org.locationtech.udig.tools.internal.i18n.Messages;

/**
 * This  preference page provides access to all preference relating to split tool.
 *
 * @author nprigour
 *
 */
public class SplitToolPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    
	public static final String SPLIT_TOOL_AREA_MARGIN_WARNING = "SPLIT_TOOL_AREA_MARGIN_WARNING";


    public SplitToolPreferencePage() {
        super(GRID);
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        setPreferenceStore(store);
        setTitle(Messages.Split_Title);
        setDescription(Messages.Split_Description);
    }

    @Override
    protected Control createContents(Composite parent) {
        return super.createContents(parent);
    }

    @Override
    protected void createFieldEditors() {
    	addField( new StringFieldEditor(SPLIT_TOOL_AREA_MARGIN_WARNING, 
    			"Ask confirmation during split if area below:", getFieldEditorParent()) {
    		
    		 @Override
    			protected boolean checkState() {

    		        Text text = getTextControl();

    		        if (text == null) {
    					return false;
    				}

    		        String numberString = text.getText();
    		        try {
    		            double number = Double.valueOf(numberString).doubleValue();
    		            if (number >= 0 && number <= Double.MAX_VALUE) {
    						clearErrorMessage();
    						return true;
    					}

    					showErrorMessage();
    					return false;

    		        } catch (NumberFormatException e1) {
    		            showErrorMessage();
    		        }

    		        return false;
    		    }
    	});
     
    }

    @Override
    public void init( IWorkbench workbench ) {
    }
}