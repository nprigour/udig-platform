/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.udig.catalog.jgrass.activeregion.dialogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.ICatalog;
import net.refractions.udig.catalog.IResolve;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import eu.udig.catalog.jgrass.JGrassPlugin;
import eu.udig.catalog.jgrass.core.JGrassMapGeoResource;
import eu.udig.catalog.jgrass.core.JGrassMapsetGeoResource;
import eu.udig.catalog.jgrass.core.JGrassService;

/**
 * <p>
 * This class supplies a tree viewer containing the JGrass mapsets that are in the catalog When a
 * mapset is selected it is passed to the WidgetObservers that are registered with this class.
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 * @since 1.1.0
 */
public class CatalogJGrassMapsetsTreeViewer extends Composite implements ISelectionChangedListener, IResourcesSelector {

    private final HashMap<String, JGrassMapsetGeoResource> itemsMap = new HashMap<String, JGrassMapsetGeoResource>();
    private LabelProvider labelProvider = null;
    private List<JGrassMapsetGeoResource> itemLayers;

    /**
     * @param parent
     * @param style
     * @param selectionStyle the tree selection style (single or multiple)
     * @param mapType the types of map to be filtered out (ex.
     *        {@link FeatureLayerTreeViewer.SHAPELAYER})
     */
    public CatalogJGrassMapsetsTreeViewer( Composite parent, int style, int selectionStyle ) {
        super(parent, style);
        setLayout(new GridLayout(1, false));
        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
        gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
        gridData.grabExcessVerticalSpace = true;
        setLayoutData(gridData);

        // Create the tree viewer to display the file tree
        PatternFilter patternFilter = new PatternFilter();
        final FilteredTree filter = new FilteredTree(this, selectionStyle, patternFilter);
        final TreeViewer tv = filter.getViewer();
        tv.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        tv.setContentProvider(new ContentProvider());
        labelProvider = new LabelProvider();
        tv.setLabelProvider(labelProvider);
        tv.setInput("dummy"); // pass a non-null that will be ignored //$NON-NLS-1$
        tv.addSelectionChangedListener(this);
    }

    public void selectionChanged( SelectionChangedEvent event ) {
        // if the selection is empty clear the label
        if (event.getSelection().isEmpty()) {
            return;
        }
        if (event.getSelection() instanceof IStructuredSelection) {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            Vector<String> itemNames = new Vector<String>();
            for( Iterator iterator = selection.iterator(); iterator.hasNext(); ) {
                Object domain = iterator.next();
                String value = labelProvider.getText(domain);
                itemNames.add(value);
            }
            itemLayers = new ArrayList<JGrassMapsetGeoResource>();
            for( String name : itemNames ) {
                JGrassMapsetGeoResource tmpLayer = itemsMap.get(name);
                if (tmpLayer != null) {
                    itemLayers.add(tmpLayer);
                }
            }

        }
    }

    /**
     * This class provides the content for the tree in FileTree
     */

    private class ContentProvider implements ITreeContentProvider {
        /**
         * Gets the children of the specified object
         * 
         * @param arg0 the parent object
         * @return Object[]
         */
        public Object[] getChildren( Object arg0 ) {

            if (arg0 instanceof JGrassService) {
                JGrassService map = (JGrassService) arg0;
                List<IResolve> layers = null;
                try {
                    layers = map.members(null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (layers == null)
                    return null;
                return filteredLayers(layers);
            } else if (arg0 instanceof JGrassMapsetGeoResource) {
                return null;
            }

            return null;
        }

        private Object[] filteredLayers( List<IResolve> layers ) {
            Vector<JGrassMapsetGeoResource> filteredLayers = new Vector<JGrassMapsetGeoResource>();
            for( IResolve layer : layers ) {
                if (layer instanceof JGrassMapsetGeoResource) {

                    filteredLayers.add((JGrassMapsetGeoResource) layer);
                    itemsMap.put(((JGrassMapsetGeoResource) layer).getTitle(), (JGrassMapsetGeoResource) layer);
                }

            }

            /*
             * now let's sort them for nice visualization
             */
            HashMap<String, JGrassMapsetGeoResource> tmp = new HashMap<String, JGrassMapsetGeoResource>();
            for( JGrassMapsetGeoResource resource : filteredLayers ) {
                tmp.put(resource.getTitle(), resource);
            }
            Map<String, JGrassMapsetGeoResource> sortedMap = new TreeMap<String, JGrassMapsetGeoResource>(tmp);
            filteredLayers.removeAllElements();
            for( JGrassMapsetGeoResource map : sortedMap.values() ) {
                filteredLayers.add(map);
            }

            return filteredLayers.toArray();
        }
        /**
         * Gets the parent of the specified object
         * 
         * @param arg0 the object
         * @return Object
         */
        public Object getParent( Object arg0 ) {
            if (arg0 instanceof JGrassMapsetGeoResource) {
                return null;
            } else if (arg0 instanceof JGrassMapGeoResource) {
                try {
                    return ((JGrassMapGeoResource) arg0).parent(null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        /**
         * Returns whether the passed object has children
         * 
         * @param arg0 the parent object
         * @return boolean
         */
        public boolean hasChildren( Object arg0 ) {
            if (arg0 instanceof JGrassService) {
                return true;
            } else if (arg0 instanceof JGrassMapsetGeoResource) {
                return false;
            }
            return false;
        }

        /**
         * Gets the root element(s) of the tree
         * 
         * @param arg0 the input data
         * @return Object[]
         */
        public Object[] getElements( Object arg0 ) {
            // add the service to the catalog
            ICatalog catalog = CatalogPlugin.getDefault().getLocalCatalog();
            try {
                ArrayList<IResolve> finalCatalogMembers = new ArrayList<IResolve>();
                List< ? extends IResolve> allCatalogMembers = catalog.members(null);
                for( IResolve resolve : allCatalogMembers ) {
                    if (resolve instanceof JGrassService) {
                        finalCatalogMembers.add(resolve);
                    }
                }

                if (finalCatalogMembers.size() > 0) {
                    return finalCatalogMembers.toArray();
                } else {
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * Disposes any created resources
         */
        public void dispose() {
            // Nothing to dispose
        }

        /**
         * Called when the input changes
         * 
         * @param arg0 the viewer
         * @param arg1 the old input
         * @param arg2 the new input
         */
        public void inputChanged( Viewer arg0, Object arg1, Object arg2 ) {
            // Nothing to change
        }
    }

    /**
     * This class provides the labels for the file tree
     */

    private static class LabelProvider implements ILabelProvider {
        // The listeners
        private final List<ILabelProviderListener> listeners;

        // Images for tree nodes
        private final Image rasterMaps;
        private final Image mainRasterMaps;

        // Label provider state: preserve case of file names/directories

        /**
         * Constructs a FileTreeLabelProvider
         */
        public LabelProvider() {
            // Create the list to hold the listeners
            listeners = new ArrayList<ILabelProviderListener>();

            // Create the images
            mainRasterMaps = AbstractUIPlugin
                    .imageDescriptorFromPlugin(JGrassPlugin.PLUGIN_ID, "icons/obj16/jgrassloc_obj.gif").createImage(); //$NON-NLS-1$
            rasterMaps = AbstractUIPlugin
                    .imageDescriptorFromPlugin(JGrassPlugin.PLUGIN_ID, "icons/obj16/jgrass_obj.gif").createImage(); //$NON-NLS-1$
        }

        // /**
        // * Sets the preserve case attribute
        // *
        // * @param preserveCase the preserve case attribute
        // */
        // public void setPreserveCase( boolean preserveCase ) {
        //
        // // Since this attribute affects how the labels are computed,
        // // notify all the listeners of the change.
        // LabelProviderChangedEvent event = new LabelProviderChangedEvent(this);
        // for( int i = 0, n = listeners.size(); i < n; i++ ) {
        // ILabelProviderListener ilpl = listeners.get(i);
        // ilpl.labelProviderChanged(event);
        // }
        // }

        /**
         * Gets the image to display for a node in the tree
         * 
         * @param arg0 the node
         * @return Image
         */
        public Image getImage( Object arg0 ) {
            if (arg0 instanceof JGrassService) {
                return mainRasterMaps;
            } else if (arg0 instanceof JGrassMapsetGeoResource) {
                return rasterMaps;
            } else {
                return null;
            }
        }

        /**
         * Gets the text to display for a node in the tree
         * 
         * @param arg0 the node
         * @return String
         */
        public String getText( Object arg0 ) {

            String text = null;
            try {
                if (arg0 instanceof JGrassMapsetGeoResource) {
                    text = ((JGrassMapsetGeoResource) arg0).getTitle();
                } else if (arg0 instanceof JGrassService) {
                    text = ((JGrassService) arg0).getInfo(null).getTitle();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return text;
        }

        /**
         * Adds a listener to this label provider
         * 
         * @param arg0 the listener
         */
        public void addListener( ILabelProviderListener arg0 ) {
            listeners.add(arg0);
        }

        /**
         * Called when this LabelProvider is being disposed
         */
        public void dispose() {
            // Dispose the images
            if (rasterMaps != null)
                rasterMaps.dispose();
        }

        /**
         * Returns whether changes to the specified property on the specified element would affect
         * the label for the element
         * 
         * @param arg0 the element
         * @param arg1 the property
         * @return boolean
         */
        public boolean isLabelProperty( Object arg0, String arg1 ) {
            return false;
        }

        /**
         * Removes the listener
         * 
         * @param arg0 the listener to remove
         */
        public void removeListener( ILabelProviderListener arg0 ) {
            listeners.remove(arg0);
        }
    }

    public List<JGrassMapsetGeoResource> getSelectedLayers() {
        return itemLayers;
    }

    public int getType() {
        // TODO Auto-generated method stub
        return 0;
    }

}
