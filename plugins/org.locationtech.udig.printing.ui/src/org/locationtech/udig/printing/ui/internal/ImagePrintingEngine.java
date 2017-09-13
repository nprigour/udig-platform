/*
 *    uDig - User Friendly Desktop Internet GIS client
 *    http://udig.refractions.net
 *    (C) 2004, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 *
 */
package org.locationtech.udig.printing.ui.internal;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.locationtech.udig.printing.model.Box;
import org.locationtech.udig.printing.model.BoxPrinter;
import org.locationtech.udig.printing.model.Page;

import org.eclipse.core.runtime.Assert;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;

/**
 * The engine that prints to a Buffered Image.
 * 
 * @author Nikolaos Pringouris <nprigour@gmail.com>
 */
public class ImagePrintingEngine extends AbstractPrintingEngine{

    private BufferedImage outputImage;

	/**
	 * 
	 * @param page
	 * @param outputFile
	 */
    public ImagePrintingEngine( Page page, File outputFile ) {
        super(page, outputFile);
    }

    /**
     * 
     * @param page
     */
    public ImagePrintingEngine( Page page) {
    	 super(page);
    }
    

    public boolean print() {

    	Assert.isNotNull(outputFile);
    	
    	String fileExtension = outputFile.getName().substring(outputFile.getName().lastIndexOf(".")+1).trim();
    	
        Dimension paperSize = page.getPaperSize();
        Dimension pageSize = page.getSize();

        float xScale = (float) paperSize.width / (float) pageSize.width;
        float yScale = (float) paperSize.height / (float) pageSize.height;

        try {
        
        	outputImage = new BufferedImage((int) paperSize.width(), 
            		(int) paperSize.height(), BufferedImage.TYPE_4BYTE_ABGR);
            Graphics graphics = outputImage.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, (int)paperSize.width(), (int)paperSize.height());
            List<Box> boxes = page.getBoxes();
            for( Box box : boxes ) {
                String id = box.getID();
 
    			if( box.getLocation() == null || box.getSize() == null) {
    				continue;
    			}
    			Point boxLocation = box.getLocation();
    			Dimension size = box.getSize();

    			//adapt size and position based on scaleFactor
    			Point newLocation = scaleBoxPosition(box, xScale, yScale);
    			Dimension newSize = scaleBoxSize(box, xScale, yScale);

                try {
					Graphics2D boxGraphics = (Graphics2D) graphics.create(
							newLocation.x(), newLocation.y(), newSize.width(), newSize.height());
					BoxPrinter boxPrinter = box.getBoxPrinter();
					boxPrinter.draw(boxGraphics, monitor);
				} catch (Exception e) {
					e.printStackTrace();
				}
                
				//restore original un-scaled position and location
				//this is necessary in order to address case where consequent prints
				//are made using the same Printing Engine instance
				box.setLocation(boxLocation);
				box.setSize(size);
            }
            ImageIO.write(outputImage, fileExtension, outputFile);
            graphics.dispose();
            //graphics.drawImage(bI, null, 0, 0);

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

	/**
	 * @return the outputImage
	 */
	public BufferedImage getOutputImage() {
		return outputImage;
	}

}
