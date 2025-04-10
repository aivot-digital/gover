package de.aivot.GoverBackend.elements.models.form.input;

import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.form.models.FormState;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.BaseInputElement;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.models.pdf.ValuePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

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
    public Collection<FileUploadFieldItem> formatValue(Object value) {
        Collection<FileUploadFieldItem> res = new LinkedList<>();

        if (value instanceof Collection<?> cValue) {
            for (Object fValue : cValue) {
                if (fValue instanceof Map<?, ?> mValue) {
                    res.add(new FileUploadFieldItem((Map<String, Object>) mValue));
                }
            }
        }

        return res.isEmpty() ? null : res;
    }

    @Override
    public void validate(Collection<FileUploadFieldItem> value) throws ValidationException {
        if (value == null && Boolean.TRUE.equals(getRequired())) {
            throw new RequiredValidationException(this);
        }

        if (value != null) {
            if (!Boolean.TRUE.equals(isMultifile) && value.size() > 1) {
                throw new ValidationException(this, "Zu viele Dateien. Es darf nur eine Datei hochgeladen werden.");
            }

            if (Boolean.TRUE.equals(isMultifile)) {
                if (minFiles != null && value.size() < minFiles) {
                    throw new ValidationException(this, "Zu wenige Dateien. Es müssen mindestens " + minFiles + " Dateien hochgeladen werden.");
                }

                if (maxFiles != null && value.size() > maxFiles) {
                    throw new ValidationException(this, "Zu viele Dateien. Es dürfen maximal " + maxFiles + " Dateien hochgeladen werden.");
                }
            }

            for (var file : value) {
                if (file.getSize() > 10 * 1024 * 1024) {
                    throw new ValidationException(this, "Datei zu groß. Die Datei " + file.getName() + " darf maximal 10 Megabyte groß sein.");
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
                                if (ext.equalsIgnoreCase(extension)) {
                                    extensionFound = true;
                                    break;
                                }
                            }
                            if (!extensionFound) {
                                throw new ValidationException(this, "Nicht erlaubte Dateiendung " + extension + ".");
                            }
                        } else {
                            throw new ValidationException(this, "Dateiendung konnte nicht ermittelt werden.");
                        }
                    } else {
                        throw new ValidationException(this, "Fehlerhafte Datei.");
                    }
                }
            }
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Collection<FileUploadFieldItem> value, String idPrefix, FormState formState) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        if (value != null && !value.isEmpty()) {
            List<FileUploadFieldItem> items = value.stream().toList();

            fields.add(new ValuePdfRowDto(
                    getLabel(),
                    items.getFirst().getName(),
                    this
            ));

            for (int i = 1; i < items.size(); i++) {
                fields.add(new ValuePdfRowDto(
                        "",
                        items.get(i).getName(),
                        this
                ));
            }
        } else {
            fields.add(new ValuePdfRowDto(
                    getLabel(),
                    "Keine Anlagen hinzugefügt",
                    this
            ));
        }

        return fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        FileUploadField that = (FileUploadField) o;

        if (!Objects.equals(extensions, that.extensions)) return false;
        if (!Objects.equals(isMultifile, that.isMultifile)) return false;
        if (!Objects.equals(maxFiles, that.maxFiles)) return false;
        return Objects.equals(minFiles, that.minFiles);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (extensions != null ? extensions.hashCode() : 0);
        result = 31 * result + (isMultifile != null ? isMultifile.hashCode() : 0);
        result = 31 * result + (maxFiles != null ? maxFiles.hashCode() : 0);
        result = 31 * result + (minFiles != null ? minFiles.hashCode() : 0);
        return result;
    }

    // region Getter & Setter

    public Collection<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(Collection<String> extensions) {
        this.extensions = extensions;
    }

    public Boolean getIsMultifile() {
        return isMultifile;
    }

    public void setIsMultifile(Boolean multifile) {
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

    // endregion
}
