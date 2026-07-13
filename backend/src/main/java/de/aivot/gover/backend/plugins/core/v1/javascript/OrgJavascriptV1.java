package de.aivot.gover.backend.plugins.core.v1.javascript;

import de.aivot.gover.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.gover.backend.department.services.VDepartmentShadowedService;
import de.aivot.gover.backend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.gover.backend.javascript.services.JavascriptEngine;
import de.aivot.gover.backend.plugins.core.CorePlugin;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * This class provides JavaScript functions for retrieving information about organizational units (departments) in the system.
 * It implements the JavascriptFunctionProvider interface, allowing it to be used as a component in the JavaScript engine.
 */
@Component
public class OrgJavascriptV1 implements JavascriptFunctionProvider {
    private final VDepartmentShadowedService departmentService;

    @Autowired
    public OrgJavascriptV1(VDepartmentShadowedService departmentService) {
        this.departmentService = departmentService;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return "org";
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Organisationseinheiten";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Dieses Paket enthält Funktionen für Organisationseinheiten.";
    }

    @Override
    public String[] getMethodTypeDefinitions() {
        var departmentType = "{id: number; name: string; postalAddress: string | null; technicalSupportEmail: string | null; specialSupportEmail: string | null; technicalSupportPhone: string | null; technicalSupportInfo: string | null; specialSupportPhone: string | null; specialSupportInfo: string | null; defaultMailSignature: string | null; }";

        return new String[]{
                "get(id: number | null): " + departmentType + " | null;",
                "getName(id: number | null): string | null;",
                "getPostalAddress(id: number | null): string | null;",
                "getTechnicalSupportEmail(id: number | null): string | null;",
                "getTechnicalSupportPhone(id: number | null): string | null;",
                "getTechnicalSupportInfo(id: number | null): string | null;",
                "getSpecialSupportEmail(id: number | null): string | null;",
                "getSpecialSupportPhone(id: number | null): string | null;",
                "getSpecialSupportInfo(id: number | null): string | null;",
                "getDefaultMailSignature(id: number | null): string | null;"
        };
    }

    @Nullable
    @HostAccess.Export
    public ProxyObject get(@Nullable Integer id) {
        if (id == null) {
            return null;
        }

        VDepartmentShadowedEntity org = departmentService
                .retrieve(id)
                .orElse(null);

        if (org == null) {
            return null;
        }

        return JavascriptEngine
                .mapToProxyObject(toDepartmentMap(org));
    }

    @Nullable
    @HostAccess.Export
    public String getName(@Nullable Integer id) {
        return getValue(id, VDepartmentShadowedEntity::getName);
    }

    @Nullable
    @HostAccess.Export
    public String getPostalAddress(@Nullable Integer id) {
        return getValue(id, VDepartmentShadowedEntity::getPostalAddress);
    }

    @Nullable
    @HostAccess.Export
    public String getTechnicalSupportEmail(@Nullable Integer id) {
        return getValue(id, VDepartmentShadowedEntity::getTechnicalSupportEmail);
    }

    @Nullable
    @HostAccess.Export
    public String getTechnicalSupportPhone(@Nullable Integer id) {
        return getValue(id, VDepartmentShadowedEntity::getTechnicalSupportPhone);
    }

    @Nullable
    @HostAccess.Export
    public String getTechnicalSupportInfo(@Nullable Integer id) {
        return getValue(id, VDepartmentShadowedEntity::getTechnicalSupportInfo);
    }

    @Nullable
    @HostAccess.Export
    public String getSpecialSupportEmail(@Nullable Integer id) {
        return getValue(id, VDepartmentShadowedEntity::getSpecialSupportEmail);
    }

    @Nullable
    @HostAccess.Export
    public String getSpecialSupportPhone(@Nullable Integer id) {
        return getValue(id, VDepartmentShadowedEntity::getSpecialSupportPhone);
    }

    @Nullable
    @HostAccess.Export
    public String getSpecialSupportInfo(@Nullable Integer id) {
        return getValue(id, VDepartmentShadowedEntity::getSpecialSupportInfo);
    }

    @Nullable
    @HostAccess.Export
    public String getDefaultMailSignature(@Nullable Integer id) {
        return getValue(id, VDepartmentShadowedEntity::getDefaultMailSignature);
    }

    private String getValue(@Nullable Integer id, @Nonnull Function<VDepartmentShadowedEntity, String> getter) {
        if (id == null) {
            return null;
        }

        return departmentService
                .retrieve(id)
                .map(getter)
                .orElse(null);
    }


    @Nonnull
    private static Map<String, Object> toDepartmentMap(@Nonnull VDepartmentShadowedEntity org) {
        var data = new LinkedHashMap<String, Object>();

        data.put("id", org.getId());
        data.put("name", org.getName());
        data.put("postalAddress", org.getPostalAddress());
        data.put("technicalSupportEmail", org.getTechnicalSupportEmail());
        data.put("specialSupportEmail", org.getSpecialSupportEmail());
        data.put("technicalSupportPhone", org.getTechnicalSupportPhone());
        data.put("technicalSupportInfo", org.getTechnicalSupportInfo());
        data.put("specialSupportPhone", org.getSpecialSupportPhone());
        data.put("specialSupportInfo", org.getSpecialSupportInfo());
        data.put("defaultMailSignature", org.getDefaultMailSignature());

        return data;
    }
}
