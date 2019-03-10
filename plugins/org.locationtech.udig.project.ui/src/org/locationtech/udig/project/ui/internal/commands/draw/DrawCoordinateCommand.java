/* uDig - User Friendly Desktop Internet GIS client
 * https://locationtech.org/projects/technology.udig
 * (C) 2015, Eclipse Foundation
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html).
*/
package org.locationtech.udig.project.ui.internal.commands.draw;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.ui.commands.AbstractDrawCommand;
import org.locationtech.udig.project.ui.internal.ProjectUIPlugin;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Draws a Coordinate at the specified location on the screen viewport.
 * 
 * @author nprigour
 * 
 * @since 2.0
 */
public class DrawCoordinateCommand extends AbstractDrawCommand {

    private CoordinateReferenceSystem crs;

    private Coordinate coord;

    private Color paintColor = null;

    private int lineStyle = -1;

    private int lineWidth = 1;

    private Color fillColor;

    private boolean useCircle;
    
    private boolean errorReported;
    
    
    /**
     * @param coord
     * @param map Map to consider as active for drawing
     */
    public DrawCoordinateCommand(Coordinate coord, IMap map) {
        this(coord, map.getEditManager().getSelectedLayer().getCRS());
        setMap(map);
    }

    /**
     * @param coord
     * @param crs
     */
    public DrawCoordinateCommand(Coordinate coord, CoordinateReferenceSystem crs) {
        this.coord = coord;
        if (crs == null) {
            this.crs = DefaultGeographicCRS.WGS84;
        } else {
            this.crs = crs;
        }
        useCircle = false;
    }

    /**
     * Does the actual job by drawing a 3x3 rectangle or circle at the 
     * coordinate location
     * 
     * @see org.locationtech.udig.project.command.MapCommand#run()
     */
    @Override
    public void run(IProgressMonitor monitor) throws Exception {

        final Point point = getMap().getViewportModel().worldToPixel(coord);
        Shape shape = useCircle ? new Ellipse2D.Double(point.x, point.y, 3, 3) : 
            new Rectangle2D.Double(point.x, point.y, 3, 3);
        
        if (fillColor != null) {
            graphics.setColor(fillColor);
            graphics.fill(shape);
        }

        if (paintColor != null)
            graphics.setColor(paintColor);
        if (lineStyle > -1)
            graphics.setStroke(lineStyle, lineWidth);
        doDraw(shape);
    }

    /**
     * actually the Shape is always of type Rectangle2D but we may enhance 
     * and make it configurable in the future.
     *  
     * @param shape
     */
    private void doDraw(Shape shape) {
        if (shape instanceof Rectangle2D) {
            Rectangle2D rect = (Rectangle2D) shape;
            graphics.drawRect((int) rect.getX(), (int) rect.getY(), (int) rect.getWidth(),
                    (int) rect.getHeight());
            return;
        }

        if (shape instanceof Ellipse2D) {
            Ellipse2D ellipse = (Ellipse2D) shape;
            graphics.drawOval((int) ellipse.getMinX(), (int) ellipse.getMinY(),
                    (int) ellipse.getWidth(), (int) ellipse.getHeight());
            return;
        }
    }

    /**
     * Paint color to set for coordinate painting
     * @param paintColor
     */
    public void setPaintColor(Color paintColor) {
        this.paintColor = paintColor;
    }

    /**
     * @return Currently set color to paint with. May be null
     */
    public Color getPaintColor() {
        return paintColor;
    }

    /**
     * @return Returns the line style.
     */
    public int getLineStyle() {
        return lineStyle;
    }

    /**
     * @return Returns the line width.
     */
    public int getLineWidth() {
        return lineWidth;
    }

    /**
     * Sets the line style
     * 
     * @param lineStyle the style of the line, see constants in ViewportGraphics
     * @param lineWidth the width of the line
     */
    public void setStroke(int lineStyle, int lineWidth) {
        this.lineStyle = lineStyle;
        if (lineWidth >= 1) {
            this.lineWidth = lineWidth;
        }
    }

    /**
     * Sets the color that the shape will be filled with. If fill is null then no fill will be
     * applied.
     * 
     * @param fillColor a color to be used to fill the shapeor null.
     */
    public void setFill(Color fillColor) {
        this.fillColor = fillColor;
    }


    /**
     * @return the useCircle
     */
    public boolean isUseCircle() {
        return useCircle;
    }

    /**
     * @param useCircle the useCircle to set
     */
    public void setUseCircle(boolean useCircle) {
        this.useCircle = useCircle;
    }

    @Override
    public Rectangle getValidArea() {
        if (coord != null) {
            try {
                Envelope bounds = new ReferencedEnvelope(coord.x - 3, coord.y - 3, coord.x + 3,
                        coord.y + 3, crs).transform(getMap().getViewportModel().getCRS(), true);
                double[] points = new double[] { bounds.getMinX(), bounds.getMinY(),
                        bounds.getMaxX(), bounds.getMaxY() };
                getMap().getViewportModel().worldToScreenTransform().transform(points, 0, points, 0,
                        2);
                return new Rectangle((int) points[0], (int) points[1],
                        (int) Math.abs(points[2] - points[0]),
                        (int) Math.abs(points[3] - points[1]));
            } catch (TransformException e) {
                if (!errorReported) {
                    errorReported = true;
                    ProjectUIPlugin.log(
                            "error calculating valid area, this will not be reported again", e);
                }
                return null;
            } catch (MismatchedDimensionException e) {
                if (!errorReported) {
                    errorReported = true;
                    ProjectUIPlugin.log(
                            "error calculating valid area, this will not be reported again", e);
                }
                return null;
            } catch (FactoryException e) {
                if (!errorReported) {
                    errorReported = true;
                    ProjectUIPlugin.log(
                            "error calculating valid area, this will not be reported again", e);
                }
                return null;
            }
        }
        return null;

    }

    @Override
    protected void finalize() {
        dispose();
    }

    public CoordinateReferenceSystem getCrs() {
        return crs;
    }
}
