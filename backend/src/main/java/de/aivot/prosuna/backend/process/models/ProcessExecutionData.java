package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.elements.models.ComputedElementStates;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.submission.services.ElementDataTransformService;
import de.aivot.prosuna.backend.utils.MapUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the data structure used to hold process instance data during execution. It extends HashMap to allow dynamic
 * storage of key-value pairs.
 * <p>
 * Destination-key based reads, writes and removals live in {@link ProcessDataValueUtils}.
 * <p>
 * The following keys are always set:
 * <ul>
 * <li><strong>$</strong>: The process instance data itself. E.g. "$.applicant.name" gives access to the applicant's name.</li>
 * <li><strong>$$</strong>: The process instance metadata. E.g. "$$.identities" gives access to the identities of the process instance.</li>
 * <li><strong>_</strong>: The map of all process node results. E.g. "_.&lt;nodeDataKey&gt;" gives access to the result of the node with the given "nodeDataKey".</li>
 * </ul>
 */
public class ProcessExecutionData extends HashMap<String, Object> implements Cloneable {
    public static final String PROCESS_DATA_KEY = "$";
    public static final String PROCESS_METADATA_KEY = "$$";
    public static final String NODE_RESULTS_KEY = "_";

    public static final String[] PROCESS_EXEC_DATA_KEYS = {
            PROCESS_DATA_KEY,
            PROCESS_METADATA_KEY,
            NODE_RESULTS_KEY
    };

    public ProcessExecutionData() {
        put(PROCESS_DATA_KEY, new HashMap<String, Object>());
        put(PROCESS_METADATA_KEY, new HashMap<String, Object>());
        put(NODE_RESULTS_KEY, new HashMap<String, Object>());
    }

    @Override
    public ProcessExecutionData clone() {
        ProcessExecutionData clone = (ProcessExecutionData) super.clone();
        clone.putAll(MapUtils.deepCopy(this));
        return clone;
    }

    public Map<String, Object> getProcessData() {
        return getOrCreateMapSection(PROCESS_DATA_KEY);
    }

    public Map<String, Object> getProcessMetadata() {
        return getOrCreateMapSection(PROCESS_METADATA_KEY);
    }

    public Map<String, Object> getNodeData() {
        return getOrCreateMapSection(NODE_RESULTS_KEY);
    }

    public static ProcessExecutionData of(Map<String, Object> data) {
        var executionData = new ProcessExecutionData();
        executionData.putAll(data);
        return executionData;
    }

    public ProcessExecutionData addProcessData(Object processData) {
        put(PROCESS_DATA_KEY, processData);
        return this;
    }

    public ProcessExecutionData addProcessData(String key, Object value) {
        getProcessData().put(key, value);
        return this;
    }

    public ProcessExecutionData patchWithElementData(ElementDataTransformService elementDataTransformService,
                                                     BaseElement baseElement,
                                                     EffectiveElementValues effectiveElementValues) {
        return patchWithElementData(
                elementDataTransformService,
                baseElement,
                effectiveElementValues,
                new ComputedElementStates()
        );
    }

    public ProcessExecutionData patchWithElementData(ElementDataTransformService elementDataTransformService,
                                                     BaseElement baseElement,
                                                     EffectiveElementValues effectiveElementValues,
                                                     ComputedElementStates computedElementStates) {
        var clone = new ProcessExecutionData();
        clone.putAll(MapUtils.deepCopy(this));

        @SuppressWarnings("unchecked")
        Map<String, Object> processData = (Map<String, Object>) clone.get(PROCESS_DATA_KEY);
        if (processData == null) {
            processData = new HashMap<>();
        }

        var patchedProcessData = elementDataTransformService
                .buildPayload(
                        baseElement,
                        effectiveElementValues,
                        computedElementStates,
                        processData
                );
        clone.put(PROCESS_DATA_KEY, patchedProcessData);

        return clone;
    }

    public ProcessExecutionData addProcessMetadata(Object processMetadata) {
        put(PROCESS_METADATA_KEY, processMetadata);
        return this;
    }

    public ProcessExecutionData addNodeResults(ProcessNodeEntity node, Object nodeResults) {
        getNodeData().put(node.getDataKey(), nodeResults);
        return this;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOrCreateMapSection(String key) {
        var section = (Map<String, Object>) get(key);
        if (section == null) {
            section = new HashMap<>();
            put(key, section);
        }
        return section;
    }
}
