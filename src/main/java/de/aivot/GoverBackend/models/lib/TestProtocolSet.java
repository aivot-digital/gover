package de.aivot.GoverBackend.models.lib;

import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;
import java.util.Objects;

public class TestProtocolSet {
    private TestProtocol technicalTest;
    private TestProtocol professionalTest;

    public TestProtocolSet(Map<String, Object> data) {
        technicalTest = MapUtils.getApply(data, "technicalTest", Map.class, TestProtocol::new);
        professionalTest = MapUtils.getApply(data, "professionalTest", Map.class, TestProtocol::new);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestProtocolSet that = (TestProtocolSet) o;

        if (!Objects.equals(technicalTest, that.technicalTest))
            return false;
        return Objects.equals(professionalTest, that.professionalTest);
    }

    @Override
    public int hashCode() {
        int result = technicalTest != null ? technicalTest.hashCode() : 0;
        result = 31 * result + (professionalTest != null ? professionalTest.hashCode() : 0);
        return result;
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
