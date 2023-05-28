package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.BaseInputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.*;

public class FileUploadField extends BaseInputElement<Collection<FileUploadFieldItem>> {
    private Collection<String> extensions;
    private Boolean isMultifile;
    private Integer maxFiles;
    private Integer minFiles;

    public FileUploadField(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        extensions = MapUtils.getStringCollection(values, "extensions");
        isMultifile = MapUtils.getBoolean(values, "isMultifile");
        maxFiles = MapUtils.getInteger(values, "maxFiles");
        minFiles = MapUtils.getInteger(values, "minFiles");
    }

    @Override
    protected Collection<FileUploadFieldItem> formatValue(Object value) {
        Collection<FileUploadFieldItem> res = new LinkedList<>();

        if (value instanceof Collection<?> cValue) {
            for (Object fValue : cValue) {
                if (fValue instanceof Map<?,?> mValue) {
                    res.add(new FileUploadFieldItem((Map<String, Object>) mValue));
                }
            }
        }

        return res.isEmpty() ? null : res;
    }

    @Override
    public void validate(RootElement root, Map<String, Object> customerInput, Collection<FileUploadFieldItem> value, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
        if (value == null && Boolean.TRUE.equals(getRequired())) {
            throw new RequiredValidationException(this);
        }

        if (value != null) {
            if (Boolean.TRUE.equals(isMultifile) && value.size() > 1) {
                throw new ValidationException(this, "Too many files");
            }

            if (Boolean.TRUE.equals(isMultifile)) {
                if (minFiles != null && value.size() < minFiles) {
                    throw new ValidationException(this, "Not enough files");
                }

                if (maxFiles != null && value.size() > maxFiles) {
                    throw new ValidationException(this, "Too many files");
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
                                throw new ValidationException(this, "Invalid extension " + extension);
                            }
                        } else {
                            throw new ValidationException(this, "Invalid empty extension");
                        }
                    } else {
                        throw new ValidationException(this, "Broken file upload item");
                    }
                }
            }
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, Collection<FileUploadFieldItem> value, String idPrefix, ScriptEngine scriptEngine) {
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


    //region Getter & Setter

    public Collection<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(Collection<String> extensions) {
        this.extensions = extensions;
    }

    public Boolean getMultifile() {
        return isMultifile;
    }

    public void setMultifile(Boolean multifile) {
        isMultifile = multifile;
    }

    public Integer getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(Integer maxFiles) {
        this.maxFiles = maxFiles;
    }

    public Integer getMinFiles() {
        return minFiles;
    }

    public void setMinFiles(Integer minFiles) {
        this.minFiles = minFiles;
    }

    //endregion
}
