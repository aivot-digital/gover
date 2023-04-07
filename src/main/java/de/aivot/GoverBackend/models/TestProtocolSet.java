package de.aivot.GoverBackend.models;

import com.sun.istack.Nullable;

import java.util.Map;

public class TestProtocolSet {
    private TestProtocol technicalTest;
    private TestProtocol professionalTest;

    public TestProtocolSet(Map<String, Object> data) {
        if (data != null) {
            if (data.containsKey("technicalTest")) {
                technicalTest = new TestProtocol((Map<String, Object>) data.get("technicalTest"));
            }
            if (data.containsKey("professionalTest")) {
                professionalTest = new TestProtocol((Map<String, Object>) data.get("professionalTest"));
            }
        }
    }

    @Nullable
    public TestProtocol getTechnicalTest() {
        return technicalTest;
    }

    public void setTechnicalTest(TestProtocol technicalTest) {
        this.technicalTest = technicalTest;
    }

    @Nullable
    public TestProtocol getProfessionalTest() {
        return professionalTest;
    }

    public void setProfessionalTest(TestProtocol professionalTest) {
        this.professionalTest = professionalTest;
    }
}
