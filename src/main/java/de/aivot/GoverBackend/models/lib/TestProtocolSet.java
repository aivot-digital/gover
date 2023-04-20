package de.aivot.GoverBackend.models.lib;

import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;

public class TestProtocolSet {
    private TestProtocol technicalTest;
    private TestProtocol professionalTest;

    public TestProtocolSet(Map<String, Object> data) {
        technicalTest = MapUtils.getApply(data, "technicalTest", Map.class, TestProtocol::new);
        professionalTest = MapUtils.getApply(data, "professionalTest", Map.class, TestProtocol::new);
    }

    public TestProtocol getTechnicalTest() {
        return technicalTest;
    }

    public void setTechnicalTest(TestProtocol technicalTest) {
        this.technicalTest = technicalTest;
    }

    public TestProtocol getProfessionalTest() {
        return professionalTest;
    }

    public void setProfessionalTest(TestProtocol professionalTest) {
        this.professionalTest = professionalTest;
    }
}
