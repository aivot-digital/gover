package de.aivot.GoverBackend.models.elements;


import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ConditionSetOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.models.functions.FunctionCode;
import de.aivot.GoverBackend.models.functions.FunctionNoCode;
import de.aivot.GoverBackend.models.functions.conditions.ConditionOperandReference;
import de.aivot.GoverBackend.models.functions.conditions.ConditionOperandValue;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaseElementTest {
    @Test
    void testSerializeSuccessful() throws JSONException {
        var jsonStr = """
                {
                    "id": "id",
                    "type": 1,
                    "name": "name",
                    "testProtocolSet": {
                        "technicalTest": {
                            "userId": 0,
                            "timestamp": "2023-04-07"
                        },
                        "professionalTest": {
                            "userId": 1,
                            "timestamp": "2023-04-08"
                        }
                    },
                    "functionSet": {
                        "isVisible": {
                            "requirements": "requirements isVisible",
                            "functions": {
                                "main": "alert('main')"
                            },
                            "mainFunction": "main"
                        },
                        "isValid": {
                            "requirements": "requirements isValid",
                            "conditionSet": {
                                "operator": 0,
                                "conditions": [{
                                    "operator": 0,
                                    "operandA": {
                                        "ref": "ref0",
                                    },
                                    "operandB": {
                                        "value": "value0",
                                    }
                                }],
                                "conditionSets": [{
                                    "operator": "1",
                                    "conditions": [{
                                        "operator": 1,
                                        "operandA": {
                                            "ref": "ref1",
                                        },
                                        "operandB": {
                                            "value": "value1",
                                        }
                                    }],
                                }],
                            }
                        },
                        "isDisabled": {
                            "requirements": "requirements isDisabled",
                            "functions": {
                                "main": "alert('main')"
                            },
                            "mainFunction": "main"
                        },
                        "patchElement": {
                            "requirements": "requirements patchElement",
                            "functions": {
                                "main": "alert('main')"
                            },
                            "mainFunction": "main"
                        },
                        "computeValue": {
                            "requirements": "requirements computeValue",
                            "functions": {
                                "main": "alert('main')"
                            },
                            "mainFunction": "main"
                        },
                    }
                }
                    """;

        var json = new JSONObject(jsonStr).toMap();
        var item = new BaseElement(null, json) {
        };

        assertEquals("id", item.getId());
        assertEquals(ElementType.Step, item.getType());
        assertEquals("name", item.getName());

        assertNotNull(item.getTestProtocolSet());

        assertNotNull(item.getTestProtocolSet().getTechnicalTest());
        assertEquals(0, item.getTestProtocolSet().getTechnicalTest().getUserId());
        assertEquals("2023-04-07", item.getTestProtocolSet().getTechnicalTest().getTimestamp());

        assertNotNull(item.getTestProtocolSet().getProfessionalTest());
        assertEquals(1, item.getTestProtocolSet().getProfessionalTest().getUserId());
        assertEquals("2023-04-08", item.getTestProtocolSet().getProfessionalTest().getTimestamp());

        assertNotNull(item.getFunctionSet());
        assertNotNull(item.getFunctionSet().getIsVisible());
        assertEquals("requirements isVisible", item.getFunctionSet().getIsVisible().getRequirements());
        assertInstanceOf(FunctionCode.class, item.getFunctionSet().getIsVisible());
        assertEquals("main", ((FunctionCode) item.getFunctionSet().getIsVisible()).getMainFunction());
        assertEquals(1, ((FunctionCode) item.getFunctionSet().getIsVisible()).getFunctions().size());

        assertNotNull(item.getFunctionSet().getIsValid());
        assertEquals("requirements isValid", item.getFunctionSet().getIsValid().getRequirements());
        assertInstanceOf(FunctionNoCode.class, item.getFunctionSet().getIsValid());
        assertNotNull(((FunctionNoCode) item.getFunctionSet().getIsValid()).getConditionSet());
        assertEquals(ConditionSetOperator.All, ((FunctionNoCode) item.getFunctionSet().getIsValid()).getConditionSet().getOperator());
        assertEquals(1, ((FunctionNoCode) item.getFunctionSet().getIsValid()).getConditionSet().getConditions().size());
        assertEquals(1, ((FunctionNoCode) item.getFunctionSet().getIsValid()).getConditionSet().getConditionsSets().size());
        assertEquals(ConditionOperator.Equals, ((FunctionNoCode) item.getFunctionSet().getIsValid()).getConditionSet().getConditions().stream().toList().get(0).getOperator());
        assertInstanceOf(ConditionOperandReference.class, ((FunctionNoCode) item.getFunctionSet().getIsValid()).getConditionSet().getConditions().stream().toList().get(0).getOperandA());
        assertInstanceOf(ConditionOperandValue.class, ((FunctionNoCode) item.getFunctionSet().getIsValid()).getConditionSet().getConditions().stream().toList().get(0).getOperandB());
    }

    @Test
    void testSerializeEmpty() {
        var jsonStr = "{}";

        var json = new JSONObject(jsonStr).toMap();
        var item = new BaseElement(null, json) {
        };

        assertNull(item.getId());
        assertNull(item.getType());
        assertNull(item.getName());
    }

    @Test
    void testSerializeInvalidJson() {
        var jsonStr = "INVALID JSON";

        assertThrows(JSONException.class, () -> {
            var json = new JSONObject(jsonStr).toMap();
            new BaseElement(null, json) {
            };
        });
    }
}
