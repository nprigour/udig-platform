/* uDig - User Friendly Desktop Internet GIS client
 * http://udig.refractions.net
 * (C) 2004, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.printing.model.impl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.locationtech.udig.printing.model.AbstractBoxPrinter;
import org.locationtech.udig.printing.model.PrintingModelPlugin;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.ILayerListener;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.IMapCompositionListener;
import org.locationtech.udig.project.IMapListener;
import org.locationtech.udig.project.IProject;
import org.locationtech.udig.project.LayerEvent;
import org.locationtech.udig.project.MapCompositionEvent;
import org.locationtech.udig.project.MapEvent;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import org.locationtech.udig.project.internal.render.ViewportModel;
import org.locationtech.udig.project.render.IViewportModel;
import org.locationtech.udig.project.render.IViewportModelListener;
import org.locationtech.udig.project.render.RenderException;
import org.locationtech.udig.project.render.ViewportModelEvent;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.BoundsStrategy;
import org.locationtech.udig.project.ui.SelectionStyle;
import org.locationtech.udig.project.ui.ApplicationGIS.DrawMapParameter;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.emf.common.util.URI;
import org.eclipse.ui.IMemento;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Envelope;

/**
 * Box Printer for MapBox objects.
 * 
 * @author Jesse
 * @since 1.1.0
 */
public class MapBoxPrinter extends AbstractBoxPrinter implements IAdaptable {
    
    protected static final int DEFAULTDPI = 72;
    
    Map map;
    
    //used this for multiple prints without copying original Map each time
    //(Note: using this alleviates a memory leak problem that seems to exist 
    //when draw is called)
    private  IMap internalMap = null;
    
    //indicates whether the internal map will be used during drawing process
    private boolean useInternalMap = false;
    
    //indicates whether a new copy of the map will be created when draw 
    //method is called (applies only when internalMap is used).
    private boolean doCopyMapOnDraw = false;
    
    //the scale denom to be used (must be set to a value either than -1 if
    //prints based on scale value are to be made.
    private double scaleDenom;
    
    private SelectionStyle selectionStyle; 
    private int usedDpi = DEFAULTDPI;
    
    /**
     * draw a map at the current scale
     */
    public MapBoxPrinter() {
        super();
        this.scaleDenom = -1;  
        this.selectionStyle = SelectionStyle.EXCLUSIVE_ALL_SELECTION;
    }
    
    /**
     * draw a map at the current scale
     */
    public MapBoxPrinter(SelectionStyle ss) {
        super();
        this.scaleDenom = -1;  
        this.selectionStyle = ss;
    }
    
    /**
     * force the map to the given scale.  (-1 means not set.)
     * @param scaleDenom the scale denominator
     */
    public MapBoxPrinter(double scaleDenom, SelectionStyle selectionStyle) {
        super();
        this.selectionStyle = selectionStyle;
        this.scaleDenom = scaleDenom;        
    }
    
    
    ILayerListener layerListener = new ILayerListener(){

        public void refresh( LayerEvent event ) {
            setDirty(true);
        }
        
    };

    IMapCompositionListener mapCompositionListener = new IMapCompositionListener(){

        public void changed( MapCompositionEvent event ) {
            setDirty(true);

            removeLayerListenersFromRemovedLayers(event);

            addLayerListenerToNewLayers(event);
            
            
        }

        @SuppressWarnings("unchecked")
        private void removeLayerListenersFromRemovedLayers( MapCompositionEvent event ) {
            if( event.getOldValue()!=null ){
                if (event.getOldValue() instanceof ILayer) {
                    ILayer layer = (ILayer) event.getOldValue();
                    layer.removeListener(layerListener);
                }
                if (event.getOldValue() instanceof List) {
                    List<ILayer> layers = (List<ILayer>) event.getOldValue();
                    for( ILayer layer2 : layers ) {
                        layer2.removeListener(layerListener);
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        private void addLayerListenerToNewLayers( MapCompositionEvent event ) {
            if( event.getNewValue()!=null ){
                if (event.getNewValue() instanceof ILayer) {
                    ILayer layer = (ILayer) event.getNewValue();
                    layer.addListener(layerListener);
                }
                if (event.getNewValue() instanceof List) {
                    List<ILayer> layers = (List<ILayer>) event.getNewValue();
                    for( ILayer layer2 : layers ) {
                        layer2.addListener(layerListener);
                    }
                }
            }
        }

    };

    IViewportModelListener viewportListener = new IViewportModelListener(){

        public void changed( ViewportModelEvent event ) {
            setDirty(true);
        }
    };

    IMapListener mapListener = new IMapListener(){

        public void changed( MapEvent event ) {
            switch( event.getType() ) {
            case VIEWPORT_MODEL:
                if (event.getNewValue() != null) {
                    ((ViewportModel) event.getNewValue())
                            .addViewportModelListener(viewportListener);
                }
                if (event.getOldValue() != null) {
                    ((ViewportModel) event.getOldValue())
                            .removeViewportModelListener(viewportListener);
                }
                break;

            default:
                break;
            }
        }

    };

    public Map getMap() {
        return map;
    }

    @SuppressWarnings("unchecked")
    public void setMap( IMap map2 ) {
        Map oldMap = map;
        if (oldMap != null) {
            oldMap.removeMapCompositionListener(mapCompositionListener);
            oldMap.getViewportModel().removeViewportModelListener(viewportListener);
            oldMap.removeMapListener(mapListener);
            List<Layer> layers = oldMap.getLayersInternal();
            for( Layer layer : layers ) {
                layer.removeListener(layerListener);
            }
        }
        this.map = (Map) map2;
        if (map != null) {
            map.addMapCompositionListener(mapCompositionListener);
            map.getViewportModel().addViewportModelListener(viewportListener);
            map.addMapListener(mapListener);
            List<Layer> layers = map.getLayersInternal();
            for( Layer layer : layers ) {
                layer.addListener(layerListener);
            }
        }
        
        //during set perform a copy of the original map
        //This can be used for calling #draw(...) method without
        //affecting the original map.
        internalMap = ApplicationGIS.copyMap(map);
    }

    public void draw( Graphics2D graphics, IProgressMonitor monitor ) {
        super.draw(graphics, monitor);
        
        try {
            Dimension size = this.getBox().getSize();
            
            //reduce set a 1 pixel clip bound around outside to prevent 
            //some Graphics2D implementations (itext!) from bleeding into space
            //outside the graphics canvas
            //graphics.setClip(1, 1, size.width-2, size.height-2);
            
            java.awt.Dimension awtSize = new java.awt.Dimension(
                    size.width, size.height);
            
            IMap printMap = null;
            //Executes the actual job of drawing the map to an appropriate graphic object
            //Depending on settings a copy of the map may be created or not. 
            //Note that if the original map is used then always a copy is created 
            //(the flag doCopyMapOnDraw is ignored)
            boolean useSameMap = isUseInternalMap() && !getDoCopyMapOnDraw();
            if (scaleDenom == -1) {
                //ApplicationGIS.drawMap(new DrawMapParameter(graphics, awtSize, getMap(), monitor, true));
                printMap = ApplicationGIS.drawMap(
                        new DrawMapParameter(graphics, awtSize, 
                                isUseInternalMap() ? internalMap : map, 
                                        null /*use current scale*/, usedDpi, selectionStyle, monitor, true, true), 
                                        useSameMap ? false : true);
            } else {
                BoundsStrategy boundsStrategy = new BoundsStrategy(scaleDenom);
                getMap().getBlackboard().put("scale", scaleDenom); //set the scaleDenom to be used by the mapgraphics
                printMap = ApplicationGIS.drawMap(
                        new DrawMapParameter(graphics, awtSize, isUseInternalMap() ? internalMap : map, 
                                boundsStrategy, usedDpi, selectionStyle, monitor, true, true), 
                                useSameMap ? false : true);
            }
            System.out.println("Called MapBoxPrinter: Page=" +this.getBox().getPage().hashCode() +  ", MapBoxPrinter=" +this.hashCode() + ", BOX=" +this.getBox().hashCode() + ", MAP SIZE=" + size + "bounds=" + printMap.getViewportModel().getWidth() + "," + printMap.getViewportModel().getHeight());
            System.out.println("Called MapBoxPrinter: map=" + getMap().hashCode() + " printMap=" +printMap.hashCode());

            //ApplicationGIS.drawMap makes a copy of the map, and may change its bounds.  If it does change
            //the bounds then update the original map to match (this will force the mapgraphics to update too)
            if (!map.getViewportModel().getBounds().equals(printMap.getViewportModel().getBounds())) {
                SetViewportBBoxCommand cmdBBox = new SetViewportBBoxCommand(printMap.getViewportModel().getBounds());
                map.sendCommandSync(cmdBBox);
            }

            //restore regular clip rectangle
            graphics.setClip(0, 0, size.width, size.height);


        } catch (RenderException e) {
            PrintingModelPlugin.log(null, e);
        } finally {
            
        }
    }

    BufferedImage preview;
    State current;
    public void createPreview( Graphics2D graphics, final IProgressMonitor monitor ) {
		Dimension size = getBox().getSize();

        try {
            if (map != null) {
                preview = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = preview.createGraphics();
                try {
                    ApplicationGIS.drawMap(new DrawMapParameter(g, new java.awt.Dimension(
                            size.width, size.height), this.map, monitor));
                } finally {
                    g.dispose();
                }
                current = new State(map.getViewportModel(), getBox().getSize());
                graphics.drawImage(preview, 0, 0, size.width, size.height, 0, 0,
                        preview.getWidth(), preview.getHeight(), null);
            }
        } catch (Exception e) {
            PrintingModelPlugin.log("", e); //$NON-NLS-1$
            String message = "Error rendering Map:  " + e.getMessage(); //$NON-NLS-1$
            drawErrorMessage(graphics, message);
        }
        setDirty(false);
    }

    private void drawErrorMessage( Graphics2D g, String message ) {
        Dimension size = getBox().getSize();
        g.setColor(Color.YELLOW);
        Rectangle2D bounds = g.getFontMetrics().getStringBounds(message, g);
        g.fillRect((int) (size.width / 2 - bounds.getWidth() / 2) - 2,
                (int) (size.height / 2 - bounds.getHeight() / 2) + 2, (int) bounds.getWidth() + 4,
                (int) bounds.getHeight() + 4);
        g.setColor(Color.BLACK);
        g.drawString(message, (int) (size.width / 2 - bounds.getWidth() / 2),
                (int) (size.height / 2 + bounds.getHeight() / 2));
    }

    static class State {
        private double aspectRatio;
        public State( IViewportModel viewportModel, Dimension size ) {
            bounds = viewportModel.getBounds();
            crs = viewportModel.getCRS();
            this.aspectRatio = (double) size.height / (double) size.width;
        }
        Envelope bounds;
        CoordinateReferenceSystem crs;
    }

    public boolean isNewPreviewNeeded() {
        final Dimension size = getBox().getSize();
        double aspectRatio = (double) size.height / (double) size.width;
        return super.isNewPreviewNeeded() || preview == null || current == null
                || !current.bounds.equals(map.getViewportModel().getBounds())
                || current.crs != map.getViewportModel().getCRS()
                || Math.abs(current.aspectRatio - aspectRatio) > 0.00001;
    }

    public void save( IMemento memento ) {
        System.out.println(map.getID());
        memento.putString("mapId", map.getID().toString()); //$NON-NLS-1$
        memento.putString("projectId", map.getProject().getID().toString()); //$NON-NLS-1$
        memento.putInteger("scaleDenom", (int) scaleDenom); //$NON-NLS-1$
    }

    public void load( IMemento value ) {
        URI mapId = URI.createURI(value.getString("mapId")); //$NON-NLS-1$
        URI projectId = URI.createURI(value.getString("projectId")); //$NON-NLS-1$
        try {
            scaleDenom = value.getInteger("scaleDenom");
        } catch (Exception e) {
        }
        
        List< ? extends IProject> projects = ApplicationGIS.getProjects();

        for( IProject project : projects ) {
            if (project.getID().equals(projectId)) {
                List<IMap> maps = project.getElements(IMap.class);
                for( IMap map : maps ) {
                    if (map.getID().equals(mapId)) {
                        setMap(map);
                        break;
                    }
                }
            }
        }
    }

    public String getExtensionPointID() {
        return "org.locationtech.udig.printing.ui.standardBoxes"; //$NON-NLS-1$
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter( Class adapter ) {
        if (adapter.isAssignableFrom(Map.class)) {
            return this.map;
        }
        return Platform.getAdapterManager().getAdapter(this, adapter);
    }

    
    /**
     * @return the scaleDenom
     */
    public double getScaleDenom() {
        return scaleDenom;
    }

    /**
     * @param scaleDenom the scaleDenom to set
     */
    public void setScaleDenom(double scaleDenom) {
        this.scaleDenom = scaleDenom;
    }

    /**
     * @return the selectionStyle
     */
    public SelectionStyle getSelectionStyle() {
        return selectionStyle;
    }

    /**
     * @param selectionStyle the selectionStyle to set
     */
    public void setSelectionStyle(SelectionStyle selectionStyle) {
        this.selectionStyle = selectionStyle;
    }

    /**
     * @return the usedDpi
     */
    public int getUsedDpi() {
        return usedDpi;
    }

    /**
     * @param usedDpi the usedDpi to set
     */
    public void setUsedDpi(int usedDpi) {
        this.usedDpi = usedDpi;
    }

    public IMap getInternalMap() {
        return internalMap;
    }

    /**
     * @return the useInternalMap
     */
    public boolean isUseInternalMap() {
        return useInternalMap;
    }

    /**
     * @param useInternalMap the useInternalMap to set
     */
    public void setUseInternalMap(boolean useInternalMap) {
        this.useInternalMap = useInternalMap;
    }

    public boolean getDoCopyMapOnDraw() {
        return doCopyMapOnDraw;
    }

    public void setDoCopyMapOnDraw(boolean copyMapOnDraw) {
        this.doCopyMapOnDraw = copyMapOnDraw;
    }

}
