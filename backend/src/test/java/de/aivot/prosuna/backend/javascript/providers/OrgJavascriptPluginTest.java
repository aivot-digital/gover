package de.aivot.prosuna.backend.javascript.providers;

import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngine;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.plugins.core.v1.javascript.OrgJavascriptV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrgJavascriptPluginTest {
    private VDepartmentShadowedService departmentService;
    private OrgJavascriptV1 provider;

    @BeforeEach
    void setUp() {
        departmentService = mock(VDepartmentShadowedService.class);
        provider = new OrgJavascriptV1(departmentService);
    }

    @Test
    void metadataAndTypeDefinitions() {
        assertEquals(CorePlugin.PLUGIN_KEY, provider.getParentPluginKey());
        assertEquals("org", provider.getComponentKey());
        assertEquals("1.0.0", provider.getComponentVersion());
        assertEquals("Organisationseinheiten", provider.getName());
        assertEquals("Dieses Paket enth\u00e4lt Funktionen f\u00fcr Organisationseinheiten.", provider.getDescription());

        var methodDefinitions = List.of(provider.getMethodTypeDefinitions());

        assertEquals(10, methodDefinitions.size());
        assertFalse(methodDefinitions.stream().anyMatch(definition -> definition.contains("TODO")));
        assertTrue(methodDefinitions.contains("getName(id: number | null): string | null;"));
        assertTrue(methodDefinitions.contains("getPostalAddress(id: number | null): string | null;"));
        assertTrue(methodDefinitions.contains("getTechnicalSupportEmail(id: number | null): string | null;"));
        assertTrue(methodDefinitions.contains("getTechnicalSupportPhone(id: number | null): string | null;"));
        assertTrue(methodDefinitions.contains("getTechnicalSupportInfo(id: number | null): string | null;"));
        assertTrue(methodDefinitions.contains("getSpecialSupportEmail(id: number | null): string | null;"));
        assertTrue(methodDefinitions.contains("getSpecialSupportPhone(id: number | null): string | null;"));
        assertTrue(methodDefinitions.contains("getSpecialSupportInfo(id: number | null): string | null;"));
        assertTrue(methodDefinitions.contains("getDefaultMailSignature(id: number | null): string | null;"));

        var typeDefinition = provider.getTypeDefinition();

        assertTrue(typeDefinition.contains("declare interface I__org_v1"));
        assertTrue(typeDefinition.contains("get(id: number | null): {id: number; name: string; postalAddress: string | null;"));
        assertTrue(typeDefinition.contains("technicalSupportEmail: string | null;"));
        assertTrue(typeDefinition.contains("specialSupportInfo: string | null;"));
        assertTrue(typeDefinition.contains("defaultMailSignature: string | null;"));
        assertTrue(typeDefinition.contains("declare var _org_v1: I__org_v1;"));
    }

    @Test
    void getReturnsDepartmentFieldsToJavascript() {
        when(departmentService.retrieve(42))
                .thenReturn(Optional.of(department()));

        try (var jsService = new JavascriptEngine(provider)) {
            var result = jsService.evaluateCode(new JavascriptCode().setCode("""
                    const org = _org_v1.get(42);
                    ({
                        id: org.id,
                        name: org.name,
                        postalAddress: org.postalAddress,
                        technicalSupportEmail: org.technicalSupportEmail,
                        specialSupportEmail: org.specialSupportEmail,
                        technicalSupportPhone: org.technicalSupportPhone,
                        technicalSupportInfo: org.technicalSupportInfo,
                        specialSupportPhone: org.specialSupportPhone,
                        specialSupportInfo: org.specialSupportInfo,
                        defaultMailSignature: org.defaultMailSignature
                    });
                    """));
            var org = assertInstanceOf(Map.class, result.asObject());

            assertEquals(42, org.get("id"));
            assertEquals("Test Department", org.get("name"));
            assertEquals("Test Street 1", org.get("postalAddress"));
            assertEquals("tech@example.org", org.get("technicalSupportEmail"));
            assertEquals("special@example.org", org.get("specialSupportEmail"));
            assertEquals("+49 123", org.get("technicalSupportPhone"));
            assertEquals("Technical support info", org.get("technicalSupportInfo"));
            assertEquals("+49 456", org.get("specialSupportPhone"));
            assertEquals("Special support info", org.get("specialSupportInfo"));
            assertEquals("Default mail signature", org.get("defaultMailSignature"));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void individualGettersReturnDepartmentValuesToJavascript() {
        when(departmentService.retrieve(42))
                .thenReturn(Optional.of(department()));

        try (var jsService = new JavascriptEngine(provider)) {
            var result = jsService.evaluateCode(new JavascriptCode().setCode("""
                    [
                        _org_v1.getName(42),
                        _org_v1.getPostalAddress(42),
                        _org_v1.getTechnicalSupportEmail(42),
                        _org_v1.getTechnicalSupportPhone(42),
                        _org_v1.getTechnicalSupportInfo(42),
                        _org_v1.getSpecialSupportEmail(42),
                        _org_v1.getSpecialSupportPhone(42),
                        _org_v1.getSpecialSupportInfo(42),
                        _org_v1.getDefaultMailSignature(42)
                    ];
                    """));
            var values = assertInstanceOf(List.class, result.asObject());

            assertEquals("Test Department", values.get(0));
            assertEquals("Test Street 1", values.get(1));
            assertEquals("tech@example.org", values.get(2));
            assertEquals("+49 123", values.get(3));
            assertEquals("Technical support info", values.get(4));
            assertEquals("special@example.org", values.get(5));
            assertEquals("+49 456", values.get(6));
            assertEquals("Special support info", values.get(7));
            assertEquals("Default mail signature", values.get(8));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void returnsNullForNullInputWithoutDepartmentLookup() {
        assertNull(provider.get(null));
        assertNull(provider.getName(null));
        assertNull(provider.getPostalAddress(null));
        assertNull(provider.getTechnicalSupportEmail(null));
        assertNull(provider.getTechnicalSupportPhone(null));
        assertNull(provider.getTechnicalSupportInfo(null));
        assertNull(provider.getSpecialSupportEmail(null));
        assertNull(provider.getSpecialSupportPhone(null));
        assertNull(provider.getSpecialSupportInfo(null));
        assertNull(provider.getDefaultMailSignature(null));

        verifyNoInteractions(departmentService);
    }

    @Test
    void returnsNullForMissingDepartment() {
        when(departmentService.retrieve(404))
                .thenReturn(Optional.empty());

        try (var jsService = new JavascriptEngine(provider)) {
            var departmentResult = jsService.evaluateCode(new JavascriptCode().setCode("_org_v1.get(404);"));
            var getterResult = jsService.evaluateCode(new JavascriptCode().setCode("_org_v1.getDefaultMailSignature(404);"));

            assertTrue(departmentResult.isNull());
            assertTrue(getterResult.isNull());
        } catch (Exception e) {
            fail(e);
        }
    }

    private static VDepartmentShadowedEntity department() {
        return new VDepartmentShadowedEntity()
                .setId(42)
                .setName("Test Department")
                .setPostalAddress("Test Street 1")
                .setTechnicalSupportEmail("tech@example.org")
                .setSpecialSupportEmail("special@example.org")
                .setTechnicalSupportPhone("+49 123")
                .setTechnicalSupportInfo("Technical support info")
                .setSpecialSupportPhone("+49 456")
                .setSpecialSupportInfo("Special support info")
                .setDefaultMailSignature("Default mail signature");
    }
}
