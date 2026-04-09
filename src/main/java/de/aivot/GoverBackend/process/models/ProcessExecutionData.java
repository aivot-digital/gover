package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the data structure used to hold process instance data during execution.
 * It extends HashMap to allow dynamic storage of key-value pairs.
 * <p>
 * The following keys are always set:
 * <ul>
 * <li><strong>$</strong>: The process instance data itself. E.g. "$.applicant.name" gives access to the applicant's name.</li>
 * <li><strong>$$</strong>: The process instance metadata. E.g. "$$.identities" gives access to the identities of the process instance.</li>
 * <li><strong>_</strong>: The map of all process node results. E.g. "_.<nodeDataKey>" gives access to the result of the node with the given "nodeDataKey".</li>
 * </ul>
 */
public class ProcessExecutionData extends HashMap<String, Object> {
    private static final String PROCESS_DATA_KEY = "$";
    private static final String PROCESS_METADATA_KEY = "$$";
    private static final String NODE_RESULTS_KEY = "_";

    public static ProcessExecutionData of(Map<String, Object> data) {
        ProcessExecutionData executionData = new ProcessExecutionData();
        executionData.putAll(data);
        return executionData;
    }

    public ProcessExecutionData addProcessData(Object processData) {
        this.put(PROCESS_DATA_KEY, processData);
        return this;
    }

    public ProcessExecutionData addProcessData(String key, Object value) {
        this.putIfAbsent(PROCESS_DATA_KEY, new HashMap<String, Object>());

        @SuppressWarnings("unchecked")
        HashMap<String, Object> dataMap = (HashMap<String, Object>) this.get(PROCESS_DATA_KEY);
        dataMap.put(key, value);

        return this;
    }

    public ProcessExecutionData addProcessMetadata(Object processMetadata) {
        this.put(PROCESS_METADATA_KEY, processMetadata);
        return this;
    }

    private ProcessExecutionData addProcessMetadata(String key, Object value) {
        this.putIfAbsent(PROCESS_METADATA_KEY, new HashMap<String, Object>());

        @SuppressWarnings("unchecked")
        HashMap<String, Object> metadataMap = (HashMap<String, Object>) this.get(PROCESS_METADATA_KEY);
        metadataMap.put(key, value);

        return this;
    }

    public ProcessExecutionData addNodeResults(ProcessNodeEntity node, Object nodeResults) {
        this.putIfAbsent(NODE_RESULTS_KEY, new HashMap<String, Object>());

        @SuppressWarnings("unchecked")
        HashMap<String, Object> allNodeResults = (HashMap<String, Object>) this.get(NODE_RESULTS_KEY);
        allNodeResults.put(node.getDataKey(), nodeResults);

        return this;
    }
}
