/* uDig - User Friendly Desktop Internet GIS client
 * http://udig.refractions.net
 * (C) 2004, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.project.internal.render.impl.renderercreator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.geotools.data.FeatureSource;
import org.geotools.util.Range;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.render.Renderer;
import org.locationtech.udig.project.render.AbstractRenderMetrics;
import org.locationtech.udig.project.render.IRenderContext;
import org.locationtech.udig.project.render.IRenderMetricsFactory;
import org.locationtech.udig.project.render.IRenderer;

/**
 * For testing.  Creates a normal Renderer.  Accepts resources that resolve to FeatureSource objects.
 * 
 * @author Nikolaos Pringouris <nprigour@gmail.com>
 */
public class SingleFeatureRenderMetricsFactory implements IRenderMetricsFactory {

    public class SingleFeatureRendererMetrics extends AbstractRenderMetrics {

		public SingleFeatureRendererMetrics( IRenderContext context, IRenderMetricsFactory factory ) {
            super(context, factory, new ArrayList<String>());
            latencyMetric =100;
            timeToDrawMetric = 200;
        }

        
        public boolean canAddLayer( ILayer layer ) {
            return layer.hasResource(FeatureSource.class);
        }

        public boolean canStyle( String styleID, Object value ) {
            return false;
        }

        public Renderer createRenderer() {
            return new SingleFeatureRenderer();
        }

        @SuppressWarnings("unchecked")
        public Set<Range<Double>> getValidScaleRanges() {
            return new HashSet<Range<Double>>();
        }
        
    }

    public boolean canRender( IRenderContext context ) throws IOException {
        return context.getGeoResource().canResolve(FeatureSource.class);
    }

    public AbstractRenderMetrics createMetrics( IRenderContext context ) {
        return new SingleFeatureRendererMetrics(context, this);
    }

    public Class< ? extends IRenderer> getRendererType() {
        return SingleFeatureRenderer.class;
    }

}
