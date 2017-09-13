package org.locationtech.udig.printing.ui.internal.editor;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.locationtech.udig.printing.model.impl.ImageBoxPrinter;
import org.locationtech.udig.printing.ui.IBoxEditAction;
import org.locationtech.udig.printing.ui.internal.editor.parts.BoxPart;

import org.eclipse.gef.commands.Command;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;

/**
 * Changes the Image within the Image Box
 * 
 * @author Nikolaos Pringouris <nprigour@gmail.com>
 * 
 */
public class SetImageAction implements IBoxEditAction {

    private BoxPart owner;
    private URL imageURL;

    public Command getCommand() {
        final URL newURL = imageURL;
        imageURL = null;
        final ImageBoxPrinter boxPrinter = getBoxPrinter();
        final URL old = boxPrinter.getImageURL();
        
        return new Command(){
            @Override
            public void execute() {
                boxPrinter.setImageURL(newURL);
                boxPrinter.setImage(null);
            }
            
            @Override
            public void undo() {
                boxPrinter.setImageURL(old);
            }
        };
    }

    public void init( BoxPart owner ) {
        this.owner = owner;
    }

    public boolean isDone() {
        return imageURL!=null;
    }

    public void perform() {
    	imageURL = null;
    	
    	String[] fileExtensions = {"*.jpg;*.JPG;*.bmp;*.BMP;*.png;*.PNG;*.gif;*.GIF"};
    	 
		Display display = Display.getCurrent();
		FileDialog dialog = new FileDialog( display.getActiveShell(), SWT.OPEN);
		dialog.setFilterPath(null);
		dialog.setText("select image to load");
		dialog.setFilterExtensions(fileExtensions);
		dialog.setFilterIndex(1);
        String imagePath = dialog.open();
        
        if (imagePath != null) {
        	try {
				this.imageURL = new File(imagePath).toURI().toURL();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
    }
    
    private ImageBoxPrinter getBoxPrinter(){
        return (ImageBoxPrinter) owner.getBoxPrinter();
    }

}
