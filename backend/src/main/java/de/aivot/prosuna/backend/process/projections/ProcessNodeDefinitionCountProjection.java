package de.aivot.prosuna.backend.process.projections;

public interface ProcessNodeDefinitionCountProjection {
    String getDefinitionKey();

    Integer getDefinitionVersion();

    long getTotal();
}
