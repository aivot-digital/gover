package de.aivot.prosuna.backend.nocode.controllers;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.nocode.models.NoCodeEditorMetadata;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.nocode.services.NoCodeEditorMetadataCleaner;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NoCodePublicResponseAdviceTest {
    @Test
    void stripsMetadataAtThePublicHttpBoundaryIncludingContextPathsButKeepsStaffResponses() throws Exception {
        var mapper = JsonMapperTestUtils.createMapper();
        var controller = new ExampleController();
        var mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new JacksonJsonHttpMessageConverter(mapper))
                .setControllerAdvice(new NoCodePublicResponseAdvice(new NoCodeEditorMetadataCleaner(mapper))).build();
        for (var path : List.of("/api/public/form/test/test/", "/api/public/processes/access/tasks/task/")) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("secret"))))
                    .andExpect(content().string(not(containsString("editorMetadata"))))
                    .andExpect(content().string(containsString("retained")));
            mvc.perform(get("/prosuna" + path).contextPath("/prosuna")).andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("secret"))));
        }
        mvc.perform(get("/api/staff/example/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("secret")));
        assertNotNull(controller.operand.getEditorMetadata());
    }

    @RestController
    static class ExampleController {
        private final NoCodeStaticValue operand = new NoCodeStaticValue("retained");

        ExampleController() {
            operand.setEditorMetadata(new NoCodeEditorMetadata("secret", null));
        }

        @GetMapping({"/api/public/form/test/test/", "/api/public/processes/access/tasks/task/", "/api/staff/example/"})
        Object response() {
            return Map.of("layout", Map.of("children", List.of(Map.of("value", Map.of("noCode", operand)))));
        }
    }
}
