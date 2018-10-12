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

import org.locationtech.udig.tools.clip.internal.view.ClipView;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * This class implements the org.eclipse.ui.startup extension point which is used to perform a
 * key-action for the coexistence of the two workflows available for ClipTool usage ('classic'
 * workflow -> tool activated by using Feature Editing -> Clip tool || new 'operation' workflow ->
 * tool activated using right click in map -> "Operations -> Clip Selected")
 * 
 * The key-action consists in removing (actually hiding) an eventually present ClipView during uDig
 * startup. The ClipView window could be present as left opened by the user in the last uDig
 * shutdown and, so, is reopened in an inconsistent state by Eclipse workbench restoring activities.
 * For the ClipView to operate correctly in each workflow ('classic' and 'operation mode') it must
 * be opened by either the ClipTool or by the ClipOperation classes, through the relative UI-user
 * interacitons.
 * 
 * @author Nikolaos Pringouris <nprigour@gmail.com>
 * 
 */
public class ClipStartup implements IStartup {

    /**
     * 
     */
    public ClipStartup() {
        // TODO Auto-generated constructor stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IStartup#earlyStartup()
     */
    @Override
    public void earlyStartup() {
        IWorkbench wb = PlatformUI.getWorkbench();
        // IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
        IWorkbenchWindow[] winArray = wb.getWorkbenchWindows();
        final IWorkbenchPage page = winArray[0].getActivePage();
        // IWorkbenchPage page = win.getActivePage();
        final IViewReference viewRef = page.findViewReference(ClipView.ID);
        // If there is an opened ClipView then close it!
        if (viewRef != null) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    page.hideView(viewRef.getView(false));
                }
            });
        }
    }
}
