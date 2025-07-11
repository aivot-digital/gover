package de.aivot.GoverBackend.elements.models.elements.form.input;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FileUploadField extends BaseInputElement<List<FileUploadFieldItem>> {
    @Nullable
    private List<String> extensions;
    @Nullable
    private Boolean isMultifile;
    @Nullable
    private Integer maxFiles;
    @Nullable
    private Integer minFiles;

    public FileUploadField() {
        super(ElementType.FileUpload);
    }

    @Nullable
    @Override
    public List<FileUploadFieldItem> formatValue(@Nullable Object value) {
        return _formatValue(value);
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable List<FileUploadFieldItem> value) {
        if (value == null || value.isEmpty()) {
            return "Keine Dateien hochgeladen";
        }

        return value
                .stream()
                .map(FileUploadFieldItem::getName)
                .filter(Objects::nonNull)
                .reduce((a, b) -> a + ", " + b)
                .orElse("Keine Dateien hochgeladen");
    }

    @Override
    public void performValidation(@Nullable List<FileUploadFieldItem> value) throws ValidationException {
        if (value == null && Boolean.TRUE.equals(getRequired())) {
            throw new RequiredValidationException(this);
        }

        if (value != null) {
            if (!Boolean.TRUE.equals(isMultifile) && value.size() > 1) {
                throw new ValidationException(this, "Zu viele Dateien. Es darf nur eine Datei hochgeladen werden.");
            }

            if (Boolean.TRUE.equals(isMultifile)) {
                if (minFiles != null && minFiles > 0 && value.size() < minFiles) {
                    throw new ValidationException(this, "Zu wenige Dateien. Es müssen mindestens " + minFiles + " Dateien hochgeladen werden.");
                }

                if (maxFiles != null && maxFiles > 0 && value.size() > maxFiles) {
                    throw new ValidationException(this, "Zu viele Dateien. Es dürfen maximal " + maxFiles + " Dateien hochgeladen werden.");
                }
            }

            for (var file : value) {
                if (file.getName() == null || file.getName().isEmpty()) {
                    throw new ValidationException(this, "Dateiname fehlt.");
                }

                if (file.getUri() == null || file.getUri().isEmpty()) {
                    throw new ValidationException(this, "Datei-URI fehlt.");
                }

                if (file.getSize() == null || file.getSize() <= 0) {
                    throw new ValidationException(this, "Dateigröße fehlt oder ist ungültig.");
                }

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

    @Nullable
    public static List<FileUploadFieldItem> _formatValue(@Nullable Object value) {
        var om = new ObjectMapper();

        List<FileUploadFieldItem> res = switch (value) {
            case null -> null;
            case Collection<?> cValue -> cValue
                    .stream()
                    .map(item -> switch (item) {
                        case FileUploadFieldItem fItem -> fItem;
                        case Map<?, ?> mItem -> om.convertValue(mItem, FileUploadFieldItem.class);
                        case String sItem -> {
                            try {
                                yield om.readValue(sItem, FileUploadFieldItem.class);
                            } catch (JsonProcessingException e) {
                                yield null;
                            }
                        }
                        default -> null;
                    })
                    .filter(Objects::nonNull)
                    .toList();
            case String sValue -> {
                try {
                    yield om
                            .readerForListOf(FileUploadFieldItem.class)
                            .readValue(sValue);
                } catch (JsonProcessingException e) {
                    yield null;
                }
            }
            default -> null;
        };

        return res == null || res.isEmpty() ? null : res;
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        FileUploadField that = (FileUploadField) o;
        return Objects.equals(extensions, that.extensions) && Objects.equals(isMultifile, that.isMultifile) && Objects.equals(maxFiles, that.maxFiles) && Objects.equals(minFiles, that.minFiles);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(extensions);
        result = 31 * result + Objects.hashCode(isMultifile);
        result = 31 * result + Objects.hashCode(maxFiles);
        result = 31 * result + Objects.hashCode(minFiles);
        return result;
    }

    // endregion

    // region Getter & Setter

    @Nullable
    public List<String> getExtensions() {
        return extensions;
    }

    public FileUploadField setExtensions(@Nullable List<String> extensions) {
        this.extensions = extensions;
        return this;
    }

    @Nullable
    public Boolean getMultifile() {
        return isMultifile;
    }

    public FileUploadField setMultifile(@Nullable Boolean multifile) {
        isMultifile = multifile;
        return this;
    }

    @Nullable
    public Integer getMaxFiles() {
        return maxFiles;
    }

    public FileUploadField setMaxFiles(@Nullable Integer maxFiles) {
        this.maxFiles = maxFiles;
        return this;
    }

    @Nullable
    public Integer getMinFiles() {
        return minFiles;
    }

    public FileUploadField setMinFiles(@Nullable Integer minFiles) {
        this.minFiles = minFiles;
        return this;
    }

    // endregion
}
