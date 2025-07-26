package de.aivot.GoverBackend.models.lib;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class TestProtocolSet implements Serializable {
    @Nullable
    private TestProtocol technicalTest;
    @Nullable
    private TestProtocol professionalTest;

    public TestProtocolSet() {}

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        TestProtocolSet that = (TestProtocolSet) o;
        return Objects.equals(technicalTest, that.technicalTest) && Objects.equals(professionalTest, that.professionalTest);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(technicalTest);
        result = 31 * result + Objects.hashCode(professionalTest);
        return result;
    }

    @Nullable
    public TestProtocol getTechnicalTest() {
        return technicalTest;
    }

    public TestProtocolSet setTechnicalTest(@Nullable TestProtocol technicalTest) {
        this.technicalTest = technicalTest;
        return this;
    }

    @Nullable
    public TestProtocol getProfessionalTest() {
        return professionalTest;
    }

    public TestProtocolSet setProfessionalTest(@Nullable TestProtocol professionalTest) {
        this.professionalTest = professionalTest;
        return this;
    }
}
