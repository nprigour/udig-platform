/**
 * 
 */
package org.locationtech.udig.tools.clip;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.ui.tool.IToolContext;
import org.locationtech.udig.tools.internal.ui.util.LayerUtil;

import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Or;

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author Nikolaos Pringouris <nprigour@gmail.com>
 *
 */
public final class Util {
	
	private Util(){}

	/**
	 * Retrieves the features from layer using the filter
	 * 
	 * @param filter
	 * @param layer
	 * 
	 * @return List of {@link SimpleFeature}}
	 * @throws IOException
	 */
	public static List<SimpleFeature> retrieveFeatures(Filter filter, ILayer layer) throws IOException {

		FeatureCollection<SimpleFeatureType, SimpleFeature> features = LayerUtil.getSelectedFeatures(layer, filter);
		
		List<SimpleFeature> featureList = new ArrayList<SimpleFeature>();
		FeatureIterator<SimpleFeature> iter = null;
		try {
			iter = features.features();
			while (iter.hasNext()) {
				SimpleFeature f = iter.next();
				featureList.add(f);
			}
		} finally {
			if (iter != null) {
				iter.close();
			}
		}
		return featureList;
	}
	
	
	
	public static List<SimpleFeature> retrieveFeaturesInBBox(List<Envelope> bbox, IToolContext context) throws IOException {

		ILayer selectedLayer = context.getSelectedLayer();

		FeatureSource<SimpleFeatureType, SimpleFeature> source = selectedLayer.getResource(FeatureSource.class, null);

		String typename = source.getSchema().getName().toString();

		// creates the query with a bbox filter
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

		Filter filter = selectedLayer.createBBoxFilter(bbox.get(0), null);
		Filter clippedFilter;
		Or filterOR = null;
		for (int index = 0; index < bbox.size(); index++) {

			clippedFilter = selectedLayer.createBBoxFilter(bbox.get(index), null);
			filterOR = ff.or(filter, clippedFilter);
		}

		Query query = new Query(typename, filterOR);

		// retrieves the feature in the bbox
		FeatureCollection<SimpleFeatureType, SimpleFeature> features = source.getFeatures(query);

		List<SimpleFeature> featureList = new ArrayList<SimpleFeature>();
		FeatureIterator<SimpleFeature> iter = null;
		try {
			iter = features.features();
			while (iter.hasNext()) {
				SimpleFeature f = iter.next();
				featureList.add(f);
			}
		} finally {
			if (iter != null) {
				iter.close();
			}
		}
		return featureList;
	}
	
}
