package tk.crazysoft.ego.components;

import java.io.File;
import java.io.FilenameFilter;

public class MultiPartFilenameFilter implements FilenameFilter {
    private final String templateName;
    private final String templateExt;
    private final boolean checkAccessible;

    public MultiPartFilenameFilter(String template) {
        this(template, false);
    }

    public MultiPartFilenameFilter(String template, boolean checkAccessible) {
        int lastDot = template.lastIndexOf('.');

        if (lastDot == -1) {
            this.templateName = template;
            templateExt = null;
        } else {
            this.templateName = template.substring(0, lastDot);
            this.templateExt = template.substring(lastDot);
        }
        this.checkAccessible = checkAccessible;
    }


    @Override
    public boolean accept(File dir, String filename) {
        int firstDot = filename.indexOf('.');
        if (firstDot == -1) {
            return templateExt == null && templateName.equals(filename) && (!checkAccessible || isAccessible(new File(dir, filename)));
        }

        int lastDot = filename.lastIndexOf('.');
        String name = filename.substring(0, firstDot);
        String ext = filename.substring(lastDot);

        return templateName.equals(name) && templateExt.equals(ext) && (!checkAccessible || isAccessible(new File(dir, filename)));
    }

    private boolean isAccessible(File file) {
        return file.exists() && file.isFile() && file.canRead();
    }
}
