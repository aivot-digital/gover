package de.aivot.prosuna.backend.codeLists.controllers;

import de.aivot.prosuna.backend.elements.services.CodeListElementOptionsService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicCodeListControllerTest {
    @Test
    void exposesChipInputSuggestionsByCodeListKey() throws Exception {
        var service = mock(CodeListElementOptionsService.class);
        when(service.listAsChipInputSuggestions("cities")).thenReturn(List.of("Berlin", "Hamburg"));
        var mockMvc = MockMvcBuilders
                .standaloneSetup(new PublicCodeListController(service))
                .build();

        mockMvc
                .perform(get("/api/public/code-lists/cities/chip-input/"))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"Berlin\",\"Hamburg\"]"));

        verify(service).listAsChipInputSuggestions("cities");
    }
}
