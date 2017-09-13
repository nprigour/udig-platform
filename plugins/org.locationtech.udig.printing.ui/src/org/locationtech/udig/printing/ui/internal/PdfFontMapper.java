package org.locationtech.udig.printing.ui.internal;

import java.awt.Font;
import java.io.File;

import org.eclipse.core.runtime.Platform;

import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.DefaultFontMapper;

public class PdfFontMapper extends DefaultFontMapper {

    public static final String GREEK_ENC = "Cp1253";
    public static String DEFAULT_FONT_NAME = "Arial Unicode MS"; 
    
    private final String defaultFont;
    private final String encoding;

    public PdfFontMapper(String font, String encoding) {
            super();                
            this.defaultFont = font;
            this.encoding = encoding;
            loadDefaultSystemFonts();
            String extraFontDir = PrintingPlugin.getDefault().getPreferenceStore().getString(PrintingPreferences.FONT_DIR);
            if (extraFontDir != null) {
                    insertDirectory(extraFontDir);
            }
    }


    /**
     * 
     */
    private void loadDefaultSystemFonts() {
            try {
                    if (Platform.getOS().equals(Platform.OS_WIN32)) {
                            String windir = System.getenv("windir");
                            insertDirectory((windir != null) ? windir + "\\fonts"  :"c:\\windows\\fonts");
                    } else if (Platform.getOS().equals(Platform.OS_LINUX)) {
                            insertDirectory("/usr/share/fonts");            
                    } else if (Platform.getOS().equals(Platform.OS_MACOSX)) {
                            insertDirectory("/System/Library/Fonts");
                            insertDirectory("/Library/Fonts");
                    } else if (Platform.getOS().equals(Platform.OS_SOLARIS)) {
                            insertDirectory("/usr/openwin/lib/X11/fonts");
                    }
            } catch (Exception e) {
                    e.printStackTrace();
            }
    }
    
    
    /* (non-Javadoc)
     * @see com.lowagie.text.pdf.DefaultFontMapper#awtToPdf(java.awt.Font)
     */
    @Override
    public BaseFont awtToPdf(Font font) {
            try {
            StringBuffer lookupName = new StringBuffer(font.getName());
            if (font.isBold() && font.isItalic()) {
                    lookupName.append(" BoldItalic");       
            } else if (font.isBold()) {
                    lookupName.append(" Bold");     
            } else if (font.isItalic()) {
                    lookupName.append(" Italic");
            }                             
             
            BaseFontParameters p = getBaseFontParameters(lookupName.toString());
            System.out.println("lookup returned " + lookupName.toString() + " " + p);
            BaseFont f = null;
            if (p == null) {
                    f = BaseFont.createFont(defaultFont, encoding, BaseFont.NOT_EMBEDDED);
            } else {
                    f = BaseFont.createFont(p.fontName, encoding, BaseFont.NOT_EMBEDDED);
            }
            System.out.println("font " + f.getFamilyFontName() + " " + f.getCodePagesSupported() + " " + f.getEncoding());
                    return f;
            }
            catch (Exception e) {
                    e.printStackTrace();
            }
            return super.awtToPdf(font);
    }
    
    /** Inserts all the fonts recognized by iText in the
     * <CODE>directory</CODE> into the map. The encoding
     * will be the default encoding of the class but can be
     * changed later.
     * @param dir the directory to scan
     * @return the number of files processed
     */    
    public int insertDirectory(String dir) {
            File file = new File(dir);
            if (!file.exists() || !file.isDirectory())
                    return 0;
            File files[] = file.listFiles();
            if (files == null)
                    return 0;
            int count = 0;
            for (int k = 0; k < files.length; ++k) {
                    file = files[k];
                    if (file.isDirectory()) {
                            System.out.println("found dir");
                            insertDirectory(file.getPath());
                    } else {
                            String name = file.getPath().toLowerCase();
                            try {
                                    if (name.endsWith(".ttf") || name.endsWith(".otf") || name.endsWith(".afm")) {
                                            Object allNames[] = BaseFont.getAllFontNames(file.getPath(), encoding, null);
                                            insertNames(allNames, file.getPath());
                                            ++count;
                                    }
                                    else if (name.endsWith(".ttc")) {
                                            String ttcs[] = BaseFont.enumerateTTCNames(file.getPath());
                                            for (int j = 0; j < ttcs.length; ++j) {
                                                    String nt = file.getPath() + "," + j;
                                                    Object allNames[] = BaseFont.getAllFontNames(nt, encoding, null);
                                                    insertNames(allNames, nt);
                                            }
                                            ++count;
                                    }
                            } catch (Exception e) {
                                    //do nothing
                            }
                    }
            }
            return count;
    }
}
