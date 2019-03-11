package org.locationtech.udig.project.tests.ui.internal.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.easymock.EasyMock;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.locationtech.udig.project.ui.internal.commands.draw.DrawCoordinateCommand;
import org.locationtech.udig.ui.graphics.ViewportGraphics;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class DrawCoordinateCommandTest {

    private static final int DEFAULT_LINE_WIDTH = 1;

    @Test
    public void nullRectangleIfCoordinateIsNotSet() {
        DrawCoordinateCommand drawCoordinateCommand = new DrawCoordinateCommand(null,
                DefaultGeographicCRS.WGS84);

        assertNull(drawCoordinateCommand.getValidArea());
    }

    @Test
    public void defaultCRSisWGS84ifNotSet() {

        CoordinateReferenceSystem nullCRS = null;
        DrawCoordinateCommand drawCoordinateCommand = new DrawCoordinateCommand(null, nullCRS);

        assertEquals(DefaultGeographicCRS.WGS84, drawCoordinateCommand.getCrs());
    }

    @Test
    public void testLinePropertiesSetCorrectly() {
        DrawCoordinateCommand drawCoordinateCommand = new DrawCoordinateCommand(null,
                DefaultGeographicCRS.WGS84);
        drawCoordinateCommand.setStroke(ViewportGraphics.LINE_DASHDOT, 99);

        assertEquals(ViewportGraphics.LINE_DASHDOT, drawCoordinateCommand.getLineStyle());
        assertEquals(99, drawCoordinateCommand.getLineWidth());
    }

    @Test
    public void throwIllegalArgumentExceptionIfLineWidthIsToSmallerThanOne() {
        DrawCoordinateCommand drawCoordinateCommand = new DrawCoordinateCommand(null,
                DefaultGeographicCRS.WGS84);
        drawCoordinateCommand.setStroke(ViewportGraphics.LINE_DASHDOT, -1);
        assertEquals(DEFAULT_LINE_WIDTH, drawCoordinateCommand.getLineWidth());

        drawCoordinateCommand.setStroke(ViewportGraphics.LINE_DASHDOT, 0);
        assertEquals(DEFAULT_LINE_WIDTH, drawCoordinateCommand.getLineWidth());
    }

    @Test
    public void validateCRSisUsedForCoordinate() {
        CoordinateReferenceSystem crsMock = EasyMock
                .createNiceMock(CoordinateReferenceSystem.class);
        DrawCoordinateCommand drawCoordinateCommand = new DrawCoordinateCommand(null, crsMock);

        assertTrue(crsMock == drawCoordinateCommand.getCrs());
    }
}
