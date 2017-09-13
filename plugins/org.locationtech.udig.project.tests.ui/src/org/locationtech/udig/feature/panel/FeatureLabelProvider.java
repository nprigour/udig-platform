package org.locationtech.udig.feature.panel;

import java.io.IOException;

import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.style.GraphicalSymbol;

import org.locationtech.udig.core.AdapterUtil;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.style.sld.SLDContent;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;


/**
 * A default label provider that provides a text based of FeatureTypeName 
 * and an image based on layer "default rule" style if possible.
 * To be used in FeaturePanels.
 *  
 * @author Nikolaos Pringouris <nprigour@gmail.com>
 *
 */
public class FeatureLabelProvider extends LabelProvider {

	public FeatureLabelProvider() {
	}

	public String getText( Object element ) {
		
		if (AdapterUtil.instance.canAdaptTo(element, SimpleFeature.class)){
			try {
				SimpleFeature feature = AdapterUtil.instance.adaptTo(SimpleFeature.class, element, new NullProgressMonitor());
				return feature.getFeatureType().getTypeName();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public Image getImage(Object element) {

		//first check that the selected element can be adapted to an ILayer. 
		//If adaptation can be made then set the image by the retrieving
		//the ExternalGraphic image of the "default_rule" (if it exists).
		if (AdapterUtil.instance.canAdaptTo(element, ILayer.class)){
			try {
				ILayer layer = AdapterUtil.instance.adaptTo(ILayer.class, element, new NullProgressMonitor());
				Rule rule = getRuleStyle(layer, "default rule");
				if (rule != null) {
					Symbolizer symbol = rule.getSymbolizers()[0];
					if (symbol instanceof PointSymbolizer ) {
						if (!((PointSymbolizer)symbol).getGraphic().graphicalSymbols().isEmpty()) {
							GraphicalSymbol gsymbol = ((PointSymbolizer)symbol).getGraphic().graphicalSymbols().get(0);
							if (gsymbol instanceof ExternalGraphic) {
								return new Image(Display.getCurrent(), ((ExternalGraphic)gsymbol).getOnlineResource().getLinkage().getPath());
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		

		return super.getImage(element);
	}

	

        /**
         * Retrieves the Rule for a layer (based on info provided in the Styleblackboard
         * under the entry "net.refractions.udig.style.sld") using the ruleName. Generally this rule (if it exists). 
         * 
         * 
         * @param layer the layer to check 
         * @param ruleName the lname of the rule
         * 
         * @return a SLD rule or null if non is found.
         */
        public Rule getRuleStyle(ILayer layer, String ruleName) {
              
        //check the StyleBlackboard entry and retrieve the rules for a given featureTypeStyle
                Style sld = (Style) layer.getStyleBlackboard().get(SLDContent.ID);
                for (FeatureTypeStyle fstyle : sld.featureTypeStyles()) {
                        for (Rule rule : fstyle.rules()) {
                                if (ruleName.equalsIgnoreCase(rule.getName())) {
                                        return rule;
                                }
                        }
                }               
                return null;
        }

}
