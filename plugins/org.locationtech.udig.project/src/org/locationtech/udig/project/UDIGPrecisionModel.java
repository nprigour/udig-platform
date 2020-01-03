package org.locationtech.udig.project;

import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

/**
 * Can be used to obtain an appropriate PrecisionModel (and associated
 * GeometryPrecisionReducer based on the value specified in the system 
 * property "udig.feature.geom.precision". If value  is not set or is invalid
 * then null values are returned. 
 * The provided value should represent the number of desired decimal digits.
 * 
 * @author Nikolaos Pringouris <nprigour@gmail.com>
 *
 */
public class UDIGPrecisionModel {
	
	public static final String UDIG_GEOM_PRECISION_PROPERTY = "udig.geom.precision";
	
	private static PrecisionModel model = null;
	private static GeometryPrecisionReducer reducer = null;
	
	static {
		try {
			Integer precision = new Integer(System.getProperty(UDIG_GEOM_PRECISION_PROPERTY));
			if (precision > 0) {
				model = new PrecisionModel(Math.pow(10, precision));
				reducer = new GeometryPrecisionReducer(model);
			}
		} catch (Exception e) {
			//
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public static PrecisionModel getModel() {
		return model;
	}

	/**
	 * 
	 * @return
	 */
	public static GeometryPrecisionReducer getPrecisionReducer() {
		return reducer;
	}
	
}
