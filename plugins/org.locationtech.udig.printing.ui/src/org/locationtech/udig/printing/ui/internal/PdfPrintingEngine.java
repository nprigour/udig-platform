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

import java.awt.Graphics2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.locationtech.udig.printing.model.Box;
import org.locationtech.udig.printing.model.BoxPrinter;
import org.locationtech.udig.printing.model.Page;

import org.eclipse.core.runtime.Assert;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

/**
 * The engine that prints to pdf.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PdfPrintingEngine extends AbstractPrintingEngine{

    /**
     * Constructs a PdfPrintingEngine using the given Page and the file to which to dump to.
     * 
     * @param page the Page to be printed.
     * @param outputFile the file to which to dump to.
     */
    public PdfPrintingEngine( Page page, File outputFile ) {
        super(page, outputFile);
    }

    public PdfPrintingEngine( Page page) {
        super(page);
    }
    

    public boolean print() {

        Assert.isNotNull(outputFile);
        
        Dimension paperSize = page.getPaperSize();
        Dimension pageSize = page.getSize();

        float xScale = (float) paperSize.width / (float) pageSize.width;
        float yScale = (float) paperSize.height / (float) pageSize.height;

        Rectangle paperRectangle = new Rectangle(paperSize.width, paperSize.height);
        Document document = new Document(paperRectangle, 0f, 0f, 0f, 0f);

        try {

            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputFile));
            document.open();

            PdfContentByte cb = writer.getDirectContent();
            DefaultFontMapper mapper = new PdfFontMapper(PdfFontMapper.DEFAULT_FONT_NAME, BaseFont.IDENTITY_H);
            
            //the graphic object to print to
            Graphics2D graphics = null;
            boolean useShapes = PrintingPlugin.getDefault().getPreferenceStore().getBoolean(PrintingPreferences.PRINT_LABELS_AS_SHAPES);
            if (useShapes) {
                graphics = cb
                    .createPrinterGraphicsShapes(paperRectangle.getWidth(), paperRectangle.getHeight(), false, 10, null);
            } else {
                graphics = cb
                                .createGraphics(paperRectangle.getWidth(), paperRectangle.getHeight(), mapper);
            }
            // BufferedImage bI = new BufferedImage((int) paperRectangle.width(), (int)
            // paperRectangle
            // .height(), BufferedImage.TYPE_INT_ARGB);
            // Graphics graphics2 = bI.getGraphics();

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

            graphics.dispose();
            // ImageIO.write(bI, "png", new File("c:\\Users\\moovida\\Desktop\\test.png"));
            // graphics.drawImage(bI, null, 0, 0);

            document.newPage();
            document.close();
            writer.close();
        } catch (DocumentException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        
        return true;
    }
}
