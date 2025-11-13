package de.aivot.GoverBackend.query.models;

import de.aivot.GoverBackend.query.exceptions.QueryValidationException;

public interface QueryPart {
    void validate() throws QueryValidationException;
    String build() throws QueryValidationException;
}
