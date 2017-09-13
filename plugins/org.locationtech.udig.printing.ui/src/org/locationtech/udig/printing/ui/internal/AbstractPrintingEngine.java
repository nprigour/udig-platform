package org.locationtech.udig.printing.ui.internal;

import java.io.File;

import org.locationtech.udig.printing.model.Box;
import org.locationtech.udig.printing.model.Page;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;

/**
 * An abstract class that can form the basis for concrete
 * Printing Engines.
 *  
 * @author Nikolaos Pringouris <nprigour@gmail.com>
 *
 */
public abstract class AbstractPrintingEngine {

    protected Page page;
    protected IProgressMonitor monitor;
    protected File outputFile;
    
    public AbstractPrintingEngine( Page page, File outputFile ) {
        this.page = page;
        this.outputFile = outputFile;
    }

    public AbstractPrintingEngine( Page page) {
        this.page = page;
    }
    
    /**
     * @param monitor
     */
    public void setMonitor( IProgressMonitor monitor ) {
        this.monitor = monitor;
    }
    
    public abstract boolean print();
    
	/**
	 * 
	 * @param box
	 * @param xScale
	 * @param yScale
	 * @return
	 */
	protected Point scaleBoxPosition(Box box,  float xScale,  float yScale) {
		Point boxLocation = box.getLocation();
		int x = boxLocation.x;
		int y = boxLocation.y;

		float newX = xScale * (float) x;
		float newY = yScale * (float) y;

		Point result = new Point((int) newX, (int) newY);
		box.setLocation(result);
		return result;
	}


	/**
	 * 
	 * @param box
	 * @param xScale
	 * @param yScale
	 * @return
	 */
	protected Dimension scaleBoxSize(Box box,  float xScale,  float yScale) {
		Dimension size = box.getSize();
		int w = size.width;
		int h = size.height;

		float newW = xScale * (float) w;
		float newH = yScale * (float) h;

		Dimension result = new Dimension((int) newW, (int) newH);
		box.setSize(new Dimension((int) newW, (int) newH));
		return result;
	}
	
    /**
	 * @param outputPdfFile the outputFile to set
	 */
	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}
	

	/**
	 * @return the outputFile
	 */
	public File getOutputFile() {
		return outputFile;
	}

    /**
	 * @return the page
	 */
	public Page getPage() {
		return page;
	}

}
