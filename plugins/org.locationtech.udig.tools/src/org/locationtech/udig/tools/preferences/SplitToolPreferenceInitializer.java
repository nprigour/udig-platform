/*
 *    uDig - User Friendly Desktop Internet GIS client
 *    http://udig.refractions.net
 *    (C) 2012, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.tools.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.locationtech.udig.tools.Activator;

/**
 * 
 * @author nprigour
 *
 */
public class SplitToolPreferenceInitializer extends AbstractPreferenceInitializer {

    public SplitToolPreferenceInitializer() {
    }

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        store.setDefault(SplitToolPreferencePage.SPLIT_TOOL_AREA_MARGIN_WARNING, "10");
    }

}