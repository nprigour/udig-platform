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
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent;
import org.locationtech.udig.tool.edit.internal.Messages;
import org.locationtech.udig.tools.edit.AbstractEditTool;
import org.locationtech.udig.tools.edit.Activator;
import org.locationtech.udig.tools.edit.Behaviour;
import org.locationtech.udig.tools.edit.DefaultEditToolBehaviour;
import org.locationtech.udig.tools.edit.EditToolConfigurationHelper;
import org.locationtech.udig.tools.edit.EnablementBehaviour;
import org.locationtech.udig.tools.edit.activator.AdvancedBehaviourCommandHandlerActivator;
import org.locationtech.udig.tools.edit.activator.DeleteGlobalActionSetterActivator;
import org.locationtech.udig.tools.edit.activator.DrawGeomsActivator;
import org.locationtech.udig.tools.edit.activator.GridActivator;
import org.locationtech.udig.tools.edit.activator.SetSnapBehaviourCommandHandlerActivator;
import org.locationtech.udig.tools.edit.activator.DrawGeomsActivator.DrawType;
import org.locationtech.udig.tools.edit.behaviour.AcceptOnDoubleClickBehaviour;
import org.locationtech.udig.tools.edit.behaviour.AcceptWhenOverFirstVertexBehaviour;
import org.locationtech.udig.tools.edit.behaviour.AddVertexWhileCreatingBehaviour;
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
import org.locationtech.udig.tools.edit.behaviour.StartEditingBehaviour;
import org.locationtech.udig.tools.edit.behaviour.accept.AcceptChangesBehaviour;
import org.locationtech.udig.tools.edit.behaviour.accept.DeselectEditShapeAcceptBehaviour;
import org.locationtech.udig.tools.edit.enablement.ValidToolDetectionActivator;
import org.locationtech.udig.tools.edit.enablement.WithinLegalLayerBoundsBehaviour;
import org.locationtech.udig.tools.edit.support.PrimitiveShape;
import org.locationtech.udig.tools.edit.support.ShapeType;
import org.locationtech.udig.tools.edit.validator.PolygonCreationValidator;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.geotools.geometry.jts.JTS;
import org.opengis.filter.spatial.Intersects;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

/**
 * Creates and edits Polygons
 * 
 * @author Jesse
 * @since 1.1.0
 */
public class PolygonTool extends AbstractEditTool {

    @Override
    protected void initEnablementBehaviours( List<EnablementBehaviour> helper ) {
        helper.add(new WithinLegalLayerBoundsBehaviour());
        helper.add(new ValidToolDetectionActivator(new Class[]{Geometry.class, Polygon.class, MultiPolygon.class}));
    }

    @Override
    protected void initCancelBehaviours( List<Behaviour> cancelBehaviours ) {
        cancelBehaviours.add(new DefaultCancelBehaviour());
    }

    @Override
    protected void initEventBehaviours( EditToolConfigurationHelper helper ) {
        helper.add( new DrawCreateVertexSnapAreaBehaviour());
        helper.startAdvancedFeatures();
        helper.add( new CursorControlBehaviour(handler, new ConditionalProvider(handler, Messages.PolygonTool_add_vertex_or_finish, Messages.PolygonTool_create_feature),
                new CursorControlBehaviour.SystemCursorProvider(SWT.CURSOR_SIZEALL),new ConditionalProvider(handler, Messages.PolygonTool_move_vertex,null), 
                new CursorControlBehaviour.SystemCursorProvider(SWT.CURSOR_CROSS), new ConditionalProvider(handler, Messages.PolygonTool_add_vertex, null)));
        helper.stopAdvancedFeatures();
//      vertex selection OR geometry selection should not both happen so make them a mutual exclusion behaviour
        helper.startMutualExclusiveList();
        helper.startOrderedList(false);
        AddVertexWhileCreatingBehaviour addVertexWhileCreatingBehaviour = new AddVertexWhileCreatingBehaviour();
        addVertexWhileCreatingBehaviour.setEditValidator(new PolygonCreationValidator());
        helper.add( addVertexWhileCreatingBehaviour);
        helper.add( new AcceptWhenOverFirstVertexBehaviour());
        helper.stopOrderedList();
        helper.startAdvancedFeatures();
        helper.add(new SelectVertexOnMouseDownBehaviour());
        helper.add( new SelectVertexBehaviour());
        helper.stopAdvancedFeatures();

        helper.startAdvancedFeatures();
        SelectFeatureBehaviour selectGeometryBehaviour = new SelectFeatureBehaviour(new Class[]{Polygon.class, MultiPolygon.class}, Intersects.class);
        selectGeometryBehaviour.initDefaultStrategies(ShapeType.POLYGON);
        helper.add( selectGeometryBehaviour);
        helper.add( new InsertVertexOnEdgeBehaviour() );
        
        helper.startElseFeatures();
        helper.add(new StartEditingBehaviour(ShapeType.POLYGON));
        helper.stopElseFeatures();
        
        helper.stopAdvancedFeatures();
        helper.stopMutualExclusiveList();
        
        helper.startAdvancedFeatures();
        helper.startMutualExclusiveList();
        helper.add( new MoveVertexBehaviour() );
        helper.add( new MoveGeometryBehaviour());
        helper.stopMutualExclusiveList();
        helper.stopAdvancedFeatures();
        
        helper.startAdvancedFeatures();
        helper.add( new SelectVerticesWithBoxBehaviour() );
        helper.stopAdvancedFeatures();
        
        helper.add( new AcceptOnDoubleClickBehaviour() );
        helper.add( new SetSnapSizeBehaviour());
        helper.done();

    }

    @Override
    protected void initAcceptBehaviours( List<Behaviour> acceptBehaviours ) {
        acceptBehaviours.add( new AcceptChangesBehaviour(Polygon.class, false) );
        acceptBehaviours.add( new DeselectEditShapeAcceptBehaviour() );
    }

    @Override
    protected void initActivators( Set<Activator> activators ) {
        DrawType type = DrawGeomsActivator.DrawType.POLYGON;
        Set<Activator> defaults = DefaultEditToolBehaviour.createDefaultCreateActivators(type);
        activators.addAll(defaults);
        activators.add(new DeleteGlobalActionSetterActivator());
        activators.add(new SetSnapBehaviourCommandHandlerActivator());
        activators.add(new AdvancedBehaviourCommandHandlerActivator());
        activators.add(new GridActivator());
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
		computeAndDisplayEdgeLenghtInfo(e);
	}

	/**
	 * 
	 * @param e
	 */
	private void computeAndDisplayEdgeLenghtInfo(MapMouseEvent e) {
		try {
			final Coordinate current = getContext().pixelToWorld(e.x, e.y);
			PrimitiveShape shape = getHandler().getCurrentShape();
			if (shape != null) {
				final Coordinate[] coords = shape.coordArray();
				displayOnStatusBar(
						JTS.orthodromicDistance(coords[coords.length-1], current, getContext().getCRS()),
						JTS.orthodromicDistance(current, coords[0], getContext().getCRS()));
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
    private void displayOnStatusBar( double distanceFrom, double distanceTo ) {
        final IStatusLineManager statusBar = getContext().getActionBars().getStatusLineManager();

        if (statusBar == null)
            return; // shouldn't happen if the tool is being used.

        IPreferenceStore preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.locationtech.udig.ui");

        String units = preferenceStore.getString(org.locationtech.udig.ui.preferences.PreferenceConstants.P_DEFAULT_UNITS);
        if (units.equals( org.locationtech.udig.ui.preferences.PreferenceConstants.AUTO_UNITS) && CRSUtil.isCoordinateReferenceSystemImperial(context.getCRS())){
            units = org.locationtech.udig.ui.preferences.PreferenceConstants.IMPERIAL_UNITS;
        }

        final Quantity<Length> distanceFromInMeter = Quantities.getQuantity(distanceFrom, SI.METRE);
        final Quantity<Length> distanceToInMeter = Quantities.getQuantity(distanceTo, SI.METRE);
        
        Quantity<Length> resultFrom = null;
        Quantity<Length> resultTo = null;
        if (units.equals( org.locationtech.udig.ui.preferences.PreferenceConstants.IMPERIAL_UNITS)){
            
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
        } else {
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

        final String message = MessageFormat.format("Distance (previous point:{0}, next point: {1}", 
        		round(resultFrom.getValue().doubleValue(), 3) + " " + resultFrom.getUnit(), 
        		round(resultTo.getValue().doubleValue(), 3) + " " + resultTo.getUnit());

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
