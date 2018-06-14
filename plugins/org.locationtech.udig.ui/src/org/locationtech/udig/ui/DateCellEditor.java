

package org.locationtech.udig.ui;


import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Class represents a special cell editor which will open a calendar/date 
 * dialog in which a date can be selected.
 * The class can return a java.util.Date or java.sql.Date
 * 
 * @author Nikolaos Pringouris <nprigour@gmail.com>
 */
public class DateCellEditor extends DialogCellEditor {

    public DateCellEditor() {
        super();
    }

    public DateCellEditor(Composite parent, int style) {
        super(parent, style);
    }

    public DateCellEditor(Composite parent) {
        super(parent);
    }


    
    @Override
    protected Object openDialogBox(Control cellEditorWindow) {
        DatePickerDialog dialog = new DatePickerDialog(cellEditorWindow.getShell(), "choose date:", true);
        Date d= (Date) doGetValue();
        java.util.Calendar c = Calendar.getInstance();
        if (d != null) {
        	c.setTime(d);
        }
        dialog.setDate(c);

        if (dialog.open() != Dialog.CANCEL) {
        	if (dialog.shouldNullify()) {//call explicitly doSeValue passing null as argument
        		doSetValue(null);
        		return null;
        	} else {
        		return (dialog.getDate() != null) ? dialog.getDate().getTime() : null;
        	}
        }
        return null;
    }
   
    
    
    @Override
	protected Object doGetValue() {
    	Object object = super.doGetValue();
    	//System.out.println("doGet Object is " + object + " type " + (object!= null ? object.getClass() : "null") );
    	if (object == null) {
    		return null;
    	} else if (Date.class.isAssignableFrom(object.getClass())) {
    		return object;
    	} else if (object instanceof  String) {
    		try {
				return DateUtils.parseDate((String)object, new String[]{"dd/MM/yyyy", "yyyy"}); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (ParseException e) {
				// probably a an empty String
				//e.printStackTrace();
			}
    	}
		return null;
	}

    
	@Override
	protected void doSetValue(Object value) {
		//if instance of Date apply the appropriate format
		//System.out.println("doSet Object is " + value + " type " + (value!= null ? value.getClass() : "null") );
		if (value instanceof Date) {
			super.doSetValue(getDateFormatter().format(value));
		} else {
			super.doSetValue(value);
		}
	}

	/**
     * @return localized date formatter to use
     */
    protected DateFormat getDateFormatter() {
        return DateFormat.getDateInstance(DateFormat.SHORT);
    }

}
