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
package org.locationtech.udig.tool.tests;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.udig.tools.internal.CursorPosition;

import com.vividsolutions.jts.geom.Coordinate;

public class CursorPositionTest_FR extends CursorPositionTest {

    @Rule public final DefaultLocaleRule defaultLocaleRule = new DefaultLocaleRule(Locale.FRENCH);
    @Test  
    public void testParseString() throws Exception {

        //System.out.println(Locale.getDefault());
        
        // Locales with ',' as decimal operator  should work correctly
        Coordinate coord = CursorPosition.parse(" 124,88 234,22", DefaultGeographicCRS.WGS84); //$NON-NLS-1$
        assertEquals(new Coordinate(124.88, 234.22), coord);
        
        // Locales with ',' as decimal operator  should NOT work correctly
        coord = CursorPosition.parse(" 124.88, 234.22", DefaultGeographicCRS.WGS84); //$NON-NLS-1$
        assertEquals(new Coordinate(124.88, 234.22), coord);
        
        // Locales with ',' as decimal operator  should NOT work correctly
        coord = CursorPosition.parse(" 124,88, 234,22", DefaultGeographicCRS.WGS84); //$NON-NLS-1$
        assertEquals(new Coordinate(124.88, 234.22), coord);

    }

}
