/*
 *    uDig - User Friendly Desktop Internet GIS client
 *    http://udig.refractions.net
 *    (C) 2004, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 *
 */
package org.locationtech.udig.printing.ui.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.locationtech.udig.printing.ui.TemplateFactory;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Provides ...TODO summary sentence
 * <p>
 * TODO Description
 * </p><p>
 * Responsibilities:
 * <ul>
 * <li>
 * <li>
 * </ul>
 * </p><p>
 * Example Use:<pre><code>
 * PrintingPreferences x = new PrintingPreferences( ... );
 * TODO code example
 * </code></pre>
 * </p>
 * @author Richard Gould
 * @since 0.3
 */
public class PrintingPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String PRINT_LABELS_AS_SHAPES = "PRINT_LABELS_AS_SHAPES";
    public static final String FONT_DIR = "FONT_DIR";
    
    private String defaultTemplate;
    private List list;
    private ArrayList templateIds;

    /**
     * Default constructor
     */
    public PrintingPreferences() {
            super(GRID);
    }
    
    /**
     * @param composite
     */
    private void createPrintTemplateList(Composite composite) {
        templateIds = new ArrayList();

        GridData gridData;
        gridData = new GridData();

        Label urlLabel = new Label(composite, SWT.NONE);
        urlLabel.setText(Messages.PrintingPreferences_label_defaultTemplate); 
        urlLabel.setLayoutData(gridData);

        gridData = new GridData(GridData.FILL_HORIZONTAL);

        Map templates = PrintingPlugin.getDefault().getTemplateFactories();

        list = new List(composite, SWT.SINGLE|SWT.BORDER);
        list.setLayoutData(gridData);

        Iterator iter = templates.entrySet().iterator();
        for(int i = 0; iter.hasNext(); i++) {
            Map.Entry entry = (Map.Entry) iter.next();

            TemplateFactory templateFactory = (TemplateFactory) entry.getValue();

            templateIds.add(i, entry.getKey());

            if (defaultTemplate.equals(templateFactory.getName())) {
                list.select(i);
            }

            list.add(templateFactory.getName());
        }
    }


    
    @Override
    protected void createFieldEditors() {
            addField(new BooleanFieldEditor(PRINT_LABELS_AS_SHAPES , Messages.PrintingPreferences_label_labels_as_shapes, getFieldEditorParent())); 
            addField(new DirectoryFieldEditor(FONT_DIR , Messages.PrintingPreferences_label_fontDir, getFieldEditorParent())); 
            createPrintTemplateList(getFieldEditorParent());
    }
    
    /**
     * TODO summary sentence for init ...
     * 
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     * @param workbench
     */
    @Override
    public void init( IWorkbench workbench ) {
        defaultTemplate = PrintingPlugin.getDefault().getPluginPreferences().getString(PrintingPlugin.PREF_DEFAULT_TEMPLATE);
        setPreferenceStore(PrintingPlugin.getDefault().getPreferenceStore());
        setDescription(Messages.PrintingPreferences_preferences_description);
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
    }
    
    @Override
    public boolean performOk() {
        int selectionIndex = list.getSelectionIndex();
        if( selectionIndex==-1 || selectionIndex>templateIds.size()-1 )
            return super.performOk();
        defaultTemplate = (String) templateIds.get(selectionIndex);
        PrintingPlugin.getDefault().getPluginPreferences().setValue(PrintingPlugin.PREF_DEFAULT_TEMPLATE, defaultTemplate);
        PrintingPlugin.getDefault().savePluginPreferences();
        return super.performOk();
    }


    
}
