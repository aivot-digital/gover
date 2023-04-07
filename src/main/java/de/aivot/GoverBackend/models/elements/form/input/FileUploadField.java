package de.aivot.GoverBackend.models.elements.form.input;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.form.InputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;

import java.util.*;

public class FileUploadField extends InputElement<Collection<FileUploadFieldItem>> {
    private Collection<String> extensions;
    private Boolean isMultifile;
    private Integer maxFiles;
    private Integer minFiles;

    public FileUploadField(BaseElement parent, Map<String, Object> data) {
        super(data);

        extensions = (Collection<String>) data.get("extensions");
        isMultifile = (Boolean) data.get("isMultifile");
        maxFiles = (Integer) data.get("maxFiles");
        minFiles = (Integer) data.get("minFiles");
    }

    @Nullable
    public Collection<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(Collection<String> extensions) {
        this.extensions = extensions;
    }

    @Nullable
    public Boolean getMultifile() {
        return isMultifile;
    }

    public void setMultifile(Boolean multifile) {
        isMultifile = multifile;
    }

    @Nullable
    public Integer getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(Integer maxFiles) {
        this.maxFiles = maxFiles;
    }

    @Nullable
    public Integer getMinFiles() {
        return minFiles;
    }

    public void setMinFiles(Integer minFiles) {
        this.minFiles = minFiles;
    }

    @Override
    public boolean isValid(Collection<FileUploadFieldItem> value, String idPrefix) {
        if (value == null) {
            return !Boolean.TRUE.equals(getRequired());
        }

        if (Boolean.TRUE.equals(isMultifile) && value.size() > 1) {
            return false;
        }

        if (Boolean.TRUE.equals(isMultifile)) {
            if (minFiles != null && value.size() < minFiles) {
                return false;
            }

            if (maxFiles != null && value.size() > maxFiles) {
                return false;
            }
        }

        if (extensions != null) {
            for (FileUploadFieldItem item : value) {
                String itemName = item.getName();
                if (itemName != null) {
                    if (itemName.contains(".")) {
                        String extension = item.getName().substring(itemName.lastIndexOf(".") + 1);
                        boolean extensionFound = false;
                        for (String ext : extensions) {
                            if (ext.equals(extension)) {
                                extensionFound = true;
                                break;
                            }
                        }
                        if (!extensionFound) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Collection<FileUploadFieldItem> value, String idPrefix) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        if (value != null && !value.isEmpty()) {
            List<FileUploadFieldItem> items = value.stream().toList();

            fields.add(new ValuePdfRowDto(
                    getLabel(),
                    items.get(0).getName()
            ));

            for (int i = 1; i < items.size(); i++) {
                fields.add(new ValuePdfRowDto(
                        "",
                        items.get(i).getName()
                ));
            }
        } else {
            fields.add(new ValuePdfRowDto(
                    getLabel(),
                    "Keine Anlagen hinzugefügt"
            ));
        }

        return fields;
    }

}
