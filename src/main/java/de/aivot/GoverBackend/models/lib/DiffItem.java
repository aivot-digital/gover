package de.aivot.GoverBackend.models.lib;

import java.io.Serializable;

public record DiffItem(String field, Object oldValue, Object newValue) implements Serializable {

}
