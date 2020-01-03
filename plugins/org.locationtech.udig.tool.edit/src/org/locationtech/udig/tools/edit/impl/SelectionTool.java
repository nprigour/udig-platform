/* uDig - User Friendly Desktop Internet GIS client
 * http://udig.refractions.net
 * (C) 2004, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.tools.edit.impl;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import si.uom.SI;
import systems.uom.common.USCustomary;

import tec.uom.se.quantity.Quantities;
import tec.uom.se.unit.MetricPrefix;

import org.locationtech.udig.catalog.util.CRSUtil;
import org.locationtech.udig.core.StaticProvider;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent;
import org.locationtech.udig.tool.edit.internal.Messages;
import org.locationtech.udig.tools.edit.AbstractEditTool;
import org.locationtech.udig.tools.edit.Activator;
import org.locationtech.udig.tools.edit.Behaviour;
import org.locationtech.udig.tools.edit.DefaultEditToolBehaviour;
import org.locationtech.udig.tools.edit.EditToolConfigurationHelper;
import org.locationtech.udig.tools.edit.EditToolHandler;
import org.locationtech.udig.tools.edit.EnablementBehaviour;
import org.locationtech.udig.tools.edit.MutualExclusiveBehavior;
import org.locationtech.udig.tools.edit.activator.AdvancedBehaviourCommandHandlerActivator;
import org.locationtech.udig.tools.edit.activator.DeleteGlobalActionSetterActivator;
import org.locationtech.udig.tools.edit.activator.DrawGeomsActivator;
import org.locationtech.udig.tools.edit.activator.GridActivator;
import org.locationtech.udig.tools.edit.activator.SetSnapBehaviourCommandHandlerActivator;
import org.locationtech.udig.tools.edit.activator.DrawGeomsActivator.DrawType;
import org.locationtech.udig.tools.edit.behaviour.AcceptOnDoubleClickBehaviour;
import org.locationtech.udig.tools.edit.behaviour.CursorControlBehaviour;
import org.locationtech.udig.tools.edit.behaviour.DefaultCancelBehaviour;
import org.locationtech.udig.tools.edit.behaviour.DrawCreateVertexSnapAreaBehaviour;
import org.locationtech.udig.tools.edit.behaviour.InsertVertexOnEdgeBehaviour;
import org.locationtech.udig.tools.edit.behaviour.MoveGeometryBehaviour;
import org.locationtech.udig.tools.edit.behaviour.MoveVertexBehaviour;
import org.locationtech.udig.tools.edit.behaviour.SelectFeatureBehaviour;
import org.locationtech.udig.tools.edit.behaviour.SelectVertexBehaviour;
import org.locationtech.udig.tools.edit.behaviour.SelectVertexOnMouseDownBehaviour;
import org.locationtech.udig.tools.edit.behaviour.SelectVerticesWithBoxBehaviour;
import org.locationtech.udig.tools.edit.behaviour.SetSnapSizeBehaviour;
import org.locationtech.udig.tools.edit.behaviour.accept.AcceptChangesBehaviour;
import org.locationtech.udig.tools.edit.enablement.ValidToolDetectionActivator;
import org.locationtech.udig.tools.edit.enablement.WithinLegalLayerBoundsBehaviour;
import org.locationtech.udig.tools.edit.support.PrimitiveShape;
import org.locationtech.udig.tools.edit.support.Selection;
import org.locationtech.udig.tools.edit.support.ShapeType;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.geotools.geometry.jts.JTS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.spatial.Intersects;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Selects and modifies features.
 * 
 * @author jones
 * @since 1.1.0
 */
public class SelectionTool extends AbstractEditTool {
    @Override
    protected void initEnablementBehaviours( List<EnablementBehaviour> helper ) {
        helper.add(new WithinLegalLayerBoundsBehaviour());
        helper.add(new ValidToolDetectionActivator(new Class[]{Geometry.class, LineString.class,
                MultiLineString.class, Polygon.class, MultiPolygon.class, Point.class,
                MultiPoint.class}));
    }

    @Override
    protected void initActivators( Set<Activator> activators ) {
        DrawType type = DrawGeomsActivator.DrawType.POLYGON;
        Set<Activator> defaults = DefaultEditToolBehaviour.createDefaultEditActivators(type);
        activators.addAll(defaults);
        activators.add(new DeleteGlobalActionSetterActivator());
        activators.add(new SetSnapBehaviourCommandHandlerActivator());
        activators.add(new AdvancedBehaviourCommandHandlerActivator());
        activators.add(new GridActivator());
    }

    @Override
    protected void initAcceptBehaviours( List<Behaviour> acceptBehaviours ) {
        MutualExclusiveBehavior mutualExclusive = new MutualExclusiveBehavior();
        acceptBehaviours.add(mutualExclusive);
        mutualExclusive.getBehaviours().add(new AcceptChangesBehaviour(Polygon.class, false){
            @Override
            public boolean isValid( EditToolHandler handler ) {
                SimpleFeature feature = handler.getContext().getEditManager().getEditFeature();
                if (feature == null)
                    return false;
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Class< ? extends Geometry> class1 = geometry == null? null : geometry.getClass();
                
                return super.isValid(handler) && feature != null
                        && (class1 == Polygon.class || class1 == MultiPolygon.class);
            }
        });
        mutualExclusive.getBehaviours().add(new AcceptChangesBehaviour(LineString.class, false){
            @Override
            public boolean isValid( EditToolHandler handler ) {
                SimpleFeature feature = handler.getContext().getEditManager().getEditFeature();
                if (feature == null)
                    return false;
                Class< ? extends Geometry> class1 = ((Geometry) feature.getDefaultGeometry())
                        .getClass();
                return super.isValid(handler) && feature != null
                        && (class1 == LineString.class || class1 == MultiLineString.class);
            }
        });
        mutualExclusive.getBehaviours().add(new AcceptChangesBehaviour(Point.class, false){
            @Override
            public boolean isValid( EditToolHandler handler ) {
                SimpleFeature feature = handler.getContext().getEditManager().getEditFeature();
                if (feature == null)
                    return false;
                Class< ? extends Geometry> class1 = ((Geometry) feature.getDefaultGeometry())
                        .getClass();
                return super.isValid(handler) && feature != null
                        && (class1 == Point.class || class1 == MultiPoint.class);
            }
        });
    }

    @Override
    protected void initCancelBehaviours( List<Behaviour> cancelBehaviours ) {
        cancelBehaviours.add(new DefaultCancelBehaviour());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void initEventBehaviours( EditToolConfigurationHelper helper ) {
        helper.add(new DrawCreateVertexSnapAreaBehaviour());
        helper.add(new CursorControlBehaviour(handler, new StaticProvider<String>(
                Messages.SelectionTool_select), new CursorControlBehaviour.SystemCursorProvider(
                SWT.CURSOR_SIZEALL),
                new StaticProvider<String>(Messages.SelectionTool_move_vertex),
                new CursorControlBehaviour.SystemCursorProvider(SWT.CURSOR_CROSS),
                new StaticProvider<String>(Messages.SelectionTool_add_vertex)));

        // vertex selection OR geometry selection should not both happen so make them a mutual
        // exclusion behaviour
        helper.startMutualExclusiveList();

        helper.add(new SelectVertexOnMouseDownBehaviour());
        helper.add(new SelectVertexBehaviour());
        helper.add(new SelectFeatureBehaviour(new Class[]{Geometry.class}, Intersects.class));

        helper.startAdvancedFeatures();
        helper.add(new InsertVertexOnEdgeBehaviour());
        helper.stopAdvancedFeatures();

        helper.stopMutualExclusiveList();

        helper.startMutualExclusiveList();
        helper.add( new MoveVertexBehaviour() );
        helper.add( new MoveGeometryBehaviour());
        helper.stopMutualExclusiveList();

        helper.add( new SelectVerticesWithBoxBehaviour() );
        helper.add( new AcceptOnDoubleClickBehaviour() );
        helper.add( new SetSnapSizeBehaviour());     
        helper.done();
    }

    
	/* (non-Javadoc)
	 * @see org.locationtech.udig.tools.edit.AbstractEditTool#onMouseDragged(org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent)
	 */
	@Override
	protected void onMouseDragged(MapMouseEvent e) {
		super.onMouseDragged(e);
		computeAndDisplayEdgeLenghtInfo(e);
	}


	/**
	 * @see org.locationtech.udig.tools.edit.AbstractEditTool#onMouseMoved(org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent)
	 */
	@Override
	protected void onMouseMoved(MapMouseEvent e) {
		super.onMouseMoved(e);
		//computeAndDisplayEdgeLenghtInfo(e);
	}

	/**
	 * 
	 * @param e
	 */
	private void computeAndDisplayEdgeLenghtInfo(MapMouseEvent e) {
		try {
			Selection selection = handler.getEditBlackboard(handler.getEditLayer()).getSelection();
			if (selection.size() != 1) {
				return;
			}
			ShapeType type = getHandler().getCurrentGeom().getShapeType();
			if (type == ShapeType.LINE || type == ShapeType.POLYGON) {	
				final Coordinate current = getContext().pixelToWorld(e.x, e.y);
				PrimitiveShape shape = getHandler().getCurrentShape();
				if (shape != null) {
					int index = shape.getPoints().indexOf(selection.iterator().next());
					if (index > -1) {	
						int totalPoints = shape.getPoints().size();
						//final Coordinate[] coords = shape.coordArray();
						int previousIndex = index > 0 ? index-1 : -1;
						int nextIndex = index < totalPoints-1 ? index+1 : -1;
						if (type == ShapeType.POLYGON) {
							previousIndex = previousIndex != -1 ? previousIndex : totalPoints-2;
							nextIndex = nextIndex != -1 ? nextIndex : 0;
						}
						//Coordinate previous = shape.getCoordsAt(shape.getPoint(previousIndex)).get(0);
						//Coordinate after = shape.getCoordsAt(shape.getPoint(nextIndex)).get(0);
						Coordinate previous = previousIndex != -1 ? getContext().pixelToWorld(
								shape.getPoint(previousIndex).getX(), shape.getPoint(previousIndex).getY()) : null;
						Coordinate after = nextIndex != -1 ? getContext().pixelToWorld(
								shape.getPoint(nextIndex).getX(), shape.getPoint(nextIndex).getY()) : null;
						Double distanceFrom =  previous != null ? 
								JTS.orthodromicDistance(previous, current, getContext().getCRS()) : null;
						Double distanceTo = after != null ? 
								JTS.orthodromicDistance(current, after, getContext().getCRS()) : null;
						displayOnStatusBar(distanceFrom, distanceTo);
					}
				}
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}		
	}
	
	/**
	 * 
	 * @param distanceFrom
	 * @param distanceTo
	 */
    private void displayOnStatusBar( Double distanceFrom, Double distanceTo ) {
        final IStatusLineManager statusBar = getContext().getActionBars().getStatusLineManager();

        if (statusBar == null)
            return; // shouldn't happen if the tool is being used.

        IPreferenceStore preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.locationtech.udig.ui");

        String units = preferenceStore.getString(org.locationtech.udig.ui.preferences.PreferenceConstants.P_DEFAULT_UNITS);
        if (units.equals( org.locationtech.udig.ui.preferences.PreferenceConstants.AUTO_UNITS) && CRSUtil.isCoordinateReferenceSystemImperial(context.getCRS())){
            units = org.locationtech.udig.ui.preferences.PreferenceConstants.IMPERIAL_UNITS;
        }

        final Quantity<Length> distanceFromInMeter = 
        		distanceFrom != null ? Quantities.getQuantity(distanceFrom, SI.METRE) : null;
        final Quantity<Length> distanceToInMeter = 
        		distanceTo != null ? Quantities.getQuantity(distanceTo, SI.METRE) : null;
        
        Quantity<Length> resultFrom = null;
        Quantity<Length> resultTo = null;
        if (units.equals( org.locationtech.udig.ui.preferences.PreferenceConstants.IMPERIAL_UNITS)){
            
        	if (distanceFrom != null) {
	        	Quantity<Length> distanceFromInMiles = distanceFromInMeter.to(USCustomary.MILE);
	            double distInMilesValue = distanceFromInMiles.getValue().doubleValue();
	            if (distInMilesValue > Quantities.getQuantity(1, USCustomary.MILE).to(USCustomary.MILE).getValue().doubleValue()) {
	                // everything longer than a mile
	                resultFrom = distanceFromInMiles;
	            } else if (distInMilesValue > Quantities.getQuantity(1, USCustomary.FOOT).to(USCustomary.MILE).getValue().doubleValue()) {
	                // everything longer that a foot
	                resultFrom = distanceFromInMiles.to(USCustomary.FOOT);
	            } else {
	                // shorter than a foot
	                resultFrom = distanceFromInMiles.to(USCustomary.INCH);
	            }
        	}
            
        	if (distanceTo != null) {
	            Quantity<Length> distanceToInMiles = distanceToInMeter.to(USCustomary.MILE);
	            double distanceToInMilesValue = distanceToInMiles.getValue().doubleValue();
	            if (distanceToInMilesValue > Quantities.getQuantity(1, USCustomary.MILE).to(USCustomary.MILE).getValue().doubleValue()) {
	                // everything longer than a mile
	            	resultTo = distanceToInMiles;
	            } else if (distanceToInMilesValue > Quantities.getQuantity(1, USCustomary.FOOT).to(USCustomary.MILE).getValue().doubleValue()) {
	                // everything longer that a foot
	            	resultTo = distanceToInMiles.to(USCustomary.FOOT);
	            } else {
	                // shorter than a foot
	            	resultTo = distanceToInMiles.to(USCustomary.INCH);
	            }
        	}
        } else {
        	if (distanceFrom != null) {
	            double distanceFromInMeterValue = distanceFromInMeter.getValue().doubleValue();       
	            if (distanceFromInMeterValue > Quantities.getQuantity(1000, SI.METRE).to(SI.METRE).getValue().doubleValue()) {
	                resultFrom = distanceFromInMeter.to(MetricPrefix.KILO(SI.METRE));
	            } else if (distanceFromInMeterValue > Quantities.getQuantity(1, SI.METRE).to(SI.METRE).getValue().doubleValue()) {
	                resultFrom = distanceFromInMeter.to(SI.METRE);
	            } else if (distanceFromInMeterValue > Quantities.getQuantity(1, MetricPrefix.CENTI(SI.METRE)).to(SI.METRE).getValue().doubleValue()) {
	                resultFrom = distanceFromInMeter.to(MetricPrefix.CENTI(SI.METRE));
	            } else {
	                resultFrom = distanceFromInMeter.to(MetricPrefix.MILLI(SI.METRE));
	            }
        	}
        	
        	if (distanceTo != null) {
	            double distanceToInMeterValue = distanceToInMeter.getValue().doubleValue();
	            if (distanceToInMeterValue > Quantities.getQuantity(1000, SI.METRE).to(SI.METRE).getValue().doubleValue()) {
	            	resultTo = distanceToInMeter.to(MetricPrefix.KILO(SI.METRE));
	            } else if (distanceToInMeterValue > Quantities.getQuantity(1, SI.METRE).to(SI.METRE).getValue().doubleValue()) {
	            	resultTo = distanceToInMeter.to(SI.METRE);
	            } else if (distanceToInMeterValue > Quantities.getQuantity(1, MetricPrefix.CENTI(SI.METRE)).to(SI.METRE).getValue().doubleValue()) {
	            	resultTo = distanceToInMeter.to(MetricPrefix.CENTI(SI.METRE));
	            } else {
	            	resultTo = distanceToInMeter.to(MetricPrefix.MILLI(SI.METRE));
	            }
        	}
        }

        final String message = MessageFormat.format("Distance ({0}{1})", 
        		distanceFrom != null ? 
        				"previous point: " + round(resultFrom.getValue().doubleValue(), 3) + " " + resultFrom.getUnit() + ", " : "", 
        		distanceTo != null ? "next point: " + round(resultTo.getValue().doubleValue(), 3) + " " + resultTo.getUnit() : "");

        getContext().updateUI(new Runnable(){
            public void run() {
                statusBar.setErrorMessage(null);
                statusBar.setMessage(message);
            }
        });
    }
    
    
    private double round(double value, int decimalPlaces) {
        double divisor = Math.pow(10, decimalPlaces);
        double newVal = value * divisor;
        newVal =  (Long.valueOf(Math.round(newVal)).intValue())/divisor;
        return newVal;
    }

}
