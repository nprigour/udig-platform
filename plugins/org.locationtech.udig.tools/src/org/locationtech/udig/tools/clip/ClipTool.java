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

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.command.MapCommand;
import org.locationtech.udig.project.internal.commands.selection.BBoxSelectionCommand;
import org.locationtech.udig.project.render.IViewportModel;
import org.locationtech.udig.project.ui.AnimationUpdater;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.commands.SelectionBoxCommand;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent;
import org.locationtech.udig.project.ui.tool.ModalTool;
import org.locationtech.udig.project.ui.tool.SimpleTool;
import org.locationtech.udig.tools.clip.internal.view.ClipView;
import org.locationtech.udig.tools.edit.animation.MessageBubble;
import org.locationtech.udig.tools.edit.preferences.PreferenceUtil;
import org.locationtech.udig.tools.internal.i18n.Messages;
import org.locationtech.udig.tools.internal.ui.util.StatusBar;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

/**
 * Clip the features in bounding box
 * <p>
 * This implementation is based in BBoxSelection. The extension add behavior object which displays
 * the clip dialog.
 * </p>
 * 
 * @author Nikolaos Pringouris <nprigour@gmail.com>
 */
public class ClipTool extends SimpleTool implements ModalTool {

    /**
     * Comment for <code>ID</code>
     */
    public static final String ID = "org.locationtech.udig.tools.clip.ClipTool"; //$NON-NLS-1$

    private static final Logger LOGGER = Logger.getLogger(ClipTool.class.getName());

    private ClipContext clipContext;

    private SelectionBoxCommand selectionBoxCommand = null;

    /**
     * The tool will respond to the mouse event and map motion stimulus.
     */
    public ClipTool() {
        super(MOUSE | MOTION);
    }

    /**
     * When the Clip tool is activated by a click in the toolbar, if the Clip View is opened, it
     * will be closed.
     */
    @Override
    public void setActive(final boolean active) {

        super.setActive(active);

        if (active) {
            this.clipContext = ClipContext.getInstance();
            this.clipContext.setToolContext(getContext());

            // Check if ClipTool has been previously executed in Operation mode and, in the case,
            // close the ClipView
            if (this.clipContext.getClipMode() == ClipContext.CLIPMODE_OPERATION) {
                closeClipView();
            }

            // Set tool mode (also set in ClipOperation.op to MERGEMODE_OPERATION)
            this.clipContext.setClipMode(ClipContext.CLIPMODE_TOOL);

            // feedback to the user indeed that he can select some features to merege
            StatusBar.setStatusBarMessage(this.clipContext.getToolContext(), "");//$NON-NLS-1$
            if (this.clipContext.getToolContext().getMapLayers().size() > 0) {

                String message = Messages.ClipTool_select_features_to_clip;
                StatusBar.setStatusBarMessage(this.clipContext.getToolContext(), message);

            } else {

                String message = "The current Map has no layers. The tool cannot operate.";//$NON-NLS-1$
                StatusBar.setStatusBarMessage(this.clipContext.getToolContext(), message);
            }
        } else {
            // if the clip view is opened it will be closed
            if (this.clipContext.isClipViewActive()) {

                closeClipView();
            }
            this.clipContext = null;
        }
    }

    /**
     * Hide the clip view.
     */
    private void closeClipView() {
        Display.getCurrent().asyncExec(new Runnable() {

            public void run() {

                // When the tool is deactivated, hide the view.
                ApplicationGIS.getView(false, ClipView.ID);
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage();
                IViewPart viewPart = page.findView(ClipView.ID);
                page.hideView(viewPart);
                if (clipContext != null) {
                	clipContext.disposeClipView();
                }
            }
        });
    }

    /**
     * Begins the bbox selection. Saves in the clip context the start point of bbox
     */
    @Override
    protected void onMousePressed(MapMouseEvent e) {

        if (!isActive())
            return;

        if (e.button != MapMouseEvent.BUTTON1) {
            return;
        }
        // Draw the initial bbox
        selectionBoxCommand = new SelectionBoxCommand(); // this.clipContext.getSelectionBoxCommand();

        Point start = e.getPoint();
        this.clipContext.setBBoxStartPoint(start);

        selectionBoxCommand.setValid(true);
        selectionBoxCommand.setShape(new Rectangle(start.x, start.y, 0, 0));
        context.sendASyncCommand(selectionBoxCommand);
    }

    /**
     * Uses the position of last event as second corner of bbox drawn to select one or more
     * features.
     */
    @Override
    protected void onMouseDragged(MapMouseEvent e) {

        if (!isActive())
            return;

        // draw the selection box
        Point start = this.clipContext.getBBoxStartPoint();
        if (start == null) {
            start = e.getPoint();
        }

        selectionBoxCommand.setShape(new Rectangle(Math.min(start.x, e.x), Math.min(start.y, e.y),
                Math.abs(e.x - start.x), Math.abs(start.y - e.y)));
        context.getViewportPane().repaint();

    }

    /**
     * Remove the bbox drawn
     * 
     * @param start
     * @param end
     */
    private void removeBBox(final Point start, final Point end) {

        int x1 = Math.min(start.x, end.x);
        int y1 = Math.min(start.y, end.y);
        int x2 = Math.abs(end.x - start.x);
        int y2 = Math.abs(start.y - end.y);

        Coordinate c1 = context.getMap().getViewportModel().pixelToWorld(x1, y1);
        Coordinate c2 = context.getMap().getViewportModel().pixelToWorld(x2, y2);

        Envelope bounds = new Envelope(c1, c2);

        // remove the bounding box selection
        MapCommand cmd = new BBoxSelectionCommand(bounds, BBoxSelectionCommand.NONE);
        getContext().sendASyncCommand(cmd);

        selectionBoxCommand.setValid(false);
        getContext().getViewportPane().repaint();
    }

    /**
     * This hook is used to catch two events: <lu> <li>Bbox drawing action to select one or more
     * features was finished</li> <li>select individual feature for click (press and release in the
     * same position)</li> <li>Unselect one feature using control key and mouse pressed.</li> </lu>
     */
    @Override
    protected void onMouseReleased(MapMouseEvent mouseEvent) {

        if (!isActive())
            return;

        if (mouseEvent.button != MapMouseEvent.BUTTON1) {
            return;
        }
        // draw the selection box
        Point start = this.clipContext.getBBoxStartPoint();

        // search an existent view or open a new one
        if (!this.clipContext.isClipViewActive()) {
            openClipView(mouseEvent.x, mouseEvent.y, this.clipContext);
        }
        ClipView clipView = this.clipContext.getClipView();

        assert clipView != null;

        // presents the selected features in the map and the clip view
        ILayer selectedLayer = getContext().getSelectedLayer();
        if (!start.equals(mouseEvent.getPoint())) { // selection using a bbox

            removeBBox(start, mouseEvent.getPoint());
            displayFeaturesUnderBBox(mouseEvent.getPoint(), selectedLayer, clipView);

        } else { // selection using click over the a feature
            if (start.equals(mouseEvent.getPoint()) && !mouseEvent.isControlDown()) {
                displayFeatureOnView(mouseEvent, selectedLayer, clipView);
            }
        }
    }

    /**
     * Display the feature selected on the view
     * 
     * @param e mouse event
     * @param selectedLayer
     * @param clipView
     */
    private void displayFeatureOnView(MapMouseEvent e, ILayer selectedLayer, ClipView clipView) {

        Envelope bound = buildBoundForPoint(e.getPoint());

        // show selection in Map
        Filter filterSelectedFeatures = selectFeaturesUnderBBox(bound, BBoxSelectionCommand.NONE);

        // retrieve the feature and present its data in the clip view
        try {
            List<SimpleFeature> selectedFeatures = Util.retrieveFeatures(filterSelectedFeatures,
                    selectedLayer);

            clipView.addSourceFeatures(selectedFeatures);

        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

    }

    private Envelope buildBoundForPoint(Point p) {
        return getContext().getBoundingBox(p, 3);
    }

    /**
     * Presents the features selected using the bbox interaction in the clip view
     * 
     * @param xyMouse
     * @param selectedLayer
     * @param clipView
     */
    private void displayFeaturesUnderBBox(Point xyMouse, ILayer selectedLayer, ClipView clipView) {

        Filter filterSelectedFeatures;
        Envelope bound;
        // select features using the drawn bbox
        IViewportModel viewportModel = getContext().getMap().getViewportModel();
        Coordinate startPoint = viewportModel.pixelToWorld(clipContext.getBBoxStartPoint().x,
                clipContext.getBBoxStartPoint().y);
        Coordinate endPoint = viewportModel.pixelToWorld(xyMouse.x, xyMouse.y);

        if (startPoint.equals2D(endPoint)) {
            // when it was a click(start and end coordinates are equal)
            // get a little bbox around this point.
            bound = getContext().getBoundingBox(xyMouse, 3);
        } else {
            bound = new Envelope(startPoint, endPoint);
        }

        // builds a command to show the features selected to clip
        try {
            filterSelectedFeatures = selectFeaturesUnderBBox(bound, BBoxSelectionCommand.NONE);
            List<SimpleFeature> selectedFeatures = Util.retrieveFeatures(filterSelectedFeatures,
                    selectedLayer);

            clipView.addSourceFeatures(selectedFeatures);

            clipView.display();

        } catch (IOException e1) {
            LOGGER.warning(e1.getMessage());
            return;
        }
    }

    /**
     * Opens the Clip view
     * 
     * @param eventX
     * @param eventY
     * @param context
     */
    private void openClipView(int eventX, int eventY, ClipContext clipContext) {

        try {
            ClipView view = (ClipView) ApplicationGIS.getView(true, ClipView.ID);
            if (view == null) {
                // crates a new clip view
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage();
                view = (ClipView) page.findView(ClipView.ID);
            }
            assert view != null : "view is null"; //$NON-NLS-1$

            // associates this the clip view with the clip context
            view.setClipContext(clipContext);
            clipContext.activeClipView(view);

        } catch (Exception ex) {
            AnimationUpdater.runTimer(getContext().getMapDisplay(), new MessageBubble(eventX,
                    eventY, "It cannot be clip", //$NON-NLS-1$
                    PreferenceUtil.instance().getMessageDisplayDelay()));
        }

    }

    /**
     * Selects the features under the bbox. This method builds a command to show the features
     * selected to clip
     * 
     * @param boundDrawn the drawn bbox by the usr
     * @param context
     * 
     * @return {@link Filter} filter that contains the selected features
     */
    private Filter selectFeaturesUnderBBox(Envelope boundDrawn, int SelectionType) {

        // updates the clip context with bounds
        this.clipContext.addBound(boundDrawn);

        MapCommand command = context.getSelectionFactory().createBBoxSelectionCommand(boundDrawn,
                SelectionType);
        getContext().sendSyncCommand(command);

        // SelectionBoxCommand selectionBoxCommand = this.clipContext.getSelectionBoxCommand();
        // selectionBoxCommand.setValid(true);

        getContext().getViewportPane().repaint();

        Filter filterSelectedFeatures = getContext().getSelectedLayer().getFilter();

        return filterSelectedFeatures;
    }

}
