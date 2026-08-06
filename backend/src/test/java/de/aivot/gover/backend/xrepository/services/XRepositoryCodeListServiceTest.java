package de.aivot.gover.backend.xrepository.services;

import de.aivot.gover.backend.core.services.HttpService;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XRepositoryCodeListServiceTest {
    @Test
    void parsesCodeListWithAdditionalGenericodeMetadata() throws Exception {
        var xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <gc:CodeList xmlns:gc="http://docs.oasis-open.org/codelist/ns/genericode/1.0/"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:schemaLocation="http://docs.oasis-open.org/codelist/ns/genericode/1.0/ genericode.xsd">
                    <Identification>
                        <ShortName>Test</ShortName>
                        <AlternateFormatLocationUri>https://example.com/test</AlternateFormatLocationUri>
                    </Identification>
                    <ColumnSet>
                        <Column Id="code" Use="required">
                            <ShortName>Code</ShortName>
                            <LongName xml:lang="de-DE">Code</LongName>
                            <Data Type="string"/>
                        </Column>
                        <Key Id="code-key">
                            <ShortName>Code</ShortName>
                            <ColumnRef Ref="code"/>
                        </Key>
                    </ColumnSet>
                    <SimpleCodeList>
                        <Row>
                            <Value ColumnRef="code">
                                <SimpleValue>001</SimpleValue>
                            </Value>
                        </Row>
                    </SimpleCodeList>
                </gc:CodeList>
                """;
        var httpService = mock(HttpService.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(xml);
        when(httpService.get(any(URI.class))).thenReturn(response);

        var codeList = new XRepositoryCodeListService(httpService).getCodeList("urn:test");

        assertEquals("Test", codeList.getIdentification().getShortName());
        assertEquals("code", codeList.getColumnSet().getColumn().getFirst().getId());
        assertEquals("001", codeList.getCodeList().getRow().getFirst().getValue().getFirst().getSimpleValue());
    }
}
