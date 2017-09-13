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
package org.locationtech.udig.printing.model.impl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.locationtech.udig.printing.model.AbstractBoxPrinter;
import org.locationtech.udig.printing.model.Box;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;

public class ImageBoxPrinter extends AbstractBoxPrinter implements IAdaptable{
	
	public static final String[] fileExtensions = {"*.jpg;*.JPG;*.bmp;*.BMP;*.png;*.PNG;*.gif;*.GIF"};
	
	private BufferedImage image; 
	
	private URL imageURL; 
	
	public ImageBoxPrinter() {
		
	}
	
	public ImageBoxPrinter(URL imageURL) {
		super();
		
		try {
			image = ImageIO.read(imageURL);
			this.imageURL = imageURL;
		} catch(IOException e) {
			image = null;
		}
	}

	/**
	 * Draw a scaled instance of the buffered image that fits to the box bounds.
	 */
	@Override
	public void draw(Graphics2D graphics, IProgressMonitor monitor) {
		
		if(image == null) {
			loadImageFromURL(imageURL);
		}
		
		if(image == null) {
			promptForImage();
		}
		
		if(image != null) {
			double scale = computeScale(getBox(), image);
			graphics.drawImage(image, AffineTransform.getScaleInstance(scale, scale), null);
		} else {
			drawWarning(graphics, "An image needs to be provided.");
		}
		
	}

	/**
	 * 
	 * @param box
	 * @param image
	 */
	private double computeScale(Box box, BufferedImage image) {
		org.eclipse.draw2d.geometry.Dimension size = box.getSize();
		double xScale = size.width/(double)image.getWidth();
		double yScale = size.height/(double)image.getHeight();
		double preferedScale = Math.min(xScale, yScale);
		return preferedScale  < 1 ? preferedScale : 1;
	}
	
	/**
	 * 
	 */
    public String getExtensionPointID() {
        return "net.refractions.udig.printing.ui.standardBoxes"; //$NON-NLS-1$
    }
    
    /**
     * 
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
    public  Object getAdapter( Class adapter ) {
        if( adapter.isAssignableFrom( BufferedImage.class )){
            return image;
        }
        return Platform.getAdapterManager().getAdapter(this, adapter);
    }

    
	/**
	 * @return the image
	 */
	public BufferedImage getImage() {
		return image;
	}

	/**
	 * @param image the image to set
	 */
	public void setImage(BufferedImage image) {
		this.image = image;
	}

	
	/**
	 * @return the imageURL
	 */
	public URL getImageURL() {
		return imageURL;
	}

	/**
	 * @param imageURL the imageURL to set
	 */
	public void setImageURL(URL imageURL) {
		this.imageURL = imageURL;
	}

	/* (non-Javadoc)
	 * @see net.refractions.udig.printing.model.AbstractBoxPrinter#save(org.eclipse.ui.IMemento)
	 */
	@Override
	public void save(IMemento memento) {
		 memento.putTextData(imageURL.toString());
	}

	
	/* (non-Javadoc)
	 * @see net.refractions.udig.printing.model.AbstractBoxPrinter#load(org.eclipse.ui.IMemento)
	 */
	@Override
	public void load(IMemento memento) {
		 String url = memento.getTextData();
		 try {
			imageURL = new URL(url);
			image = ImageIO.read(imageURL);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		 
	}

	
	/**
	 * 
	 */
	private void promptForImage() {
		final Display display = findDisplay();
		display.syncExec(new Runnable(){
			public void run() {
				FileDialog dialog = new FileDialog( display.getActiveShell(), SWT.OPEN);
				dialog.setFilterPath(null);
				dialog.setText("select image to load");
				dialog.setFilterExtensions(fileExtensions);
				dialog.setFilterIndex(1);
				String imagePath = dialog.open();
				if (imagePath != null) {
					try {
						File file = new File(imagePath);
						imageURL = file.toURI().toURL();
						image = ImageIO.read(imageURL);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}
	
	/**
	 * 
	 * @param imageURL
	 */
	private void loadImageFromURL(URL imageURL) {
		try {
			if (imageURL != null) {
				image = ImageIO.read(imageURL);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    private static Display findDisplay() {
        Display display = Display.getCurrent();
        if (display == null) {
            display = PlatformUI.getWorkbench().getDisplay();
        }
        return display;
    }
    
    /**
     * Warns the user that a MapGraphic needs to be set
     */
    private void drawWarning( Graphics2D graphics, String message ) {
        graphics.setColor(Color.BLACK);
        int height = graphics.getFontMetrics().getHeight();

        int base = (getBox().getSize().height - height) / 2 + height;

        graphics.drawString(message, 0, base);

    }
}
