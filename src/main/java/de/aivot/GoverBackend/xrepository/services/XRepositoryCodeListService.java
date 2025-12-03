package de.aivot.GoverBackend.xrepository.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.aivot.GoverBackend.core.exceptions.HttpConnectionException;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioFieldOption;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.xrepository.models.XRepositoryCodeList;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
public class XRepositoryCodeListService {
    private static final String XREPOSITORY_API_URL = "https://www.xrepository.de/api/xrepository/";
    private final HttpService httpService;

    public XRepositoryCodeListService(HttpService httpService) {
        this.httpService = httpService;
    }

    @Nonnull
    public XRepositoryCodeList getCodeList(@Nonnull String codeListUrn) throws ResponseException {
        var encodedCoreLisUrn = URLEncoder.encode(codeListUrn, StandardCharsets.UTF_8);
        var uri = URI
                .create(XREPOSITORY_API_URL + encodedCoreLisUrn + ":technischerBestandteilGenericode");

        HttpResponse<String> response;
        try {
            response = httpService
                    .get(uri);
        } catch (HttpConnectionException e) {
            throw ResponseException
                    .internalServerError(e, "Beim Abrufen der Codeliste mit der URN %s ist ein Fehler aufgetreten: %s", encodedCoreLisUrn, e.getMessage());
        }

        switch (response.statusCode()) {
            case 200:
                try {
                    return new XmlMapper()
                            .readValue(response.body(), XRepositoryCodeList.class);
                } catch (JsonProcessingException e) {
                    throw ResponseException.internalServerError(e, "Fehler beim Parsen der Codeliste mit der URN %s: %s", encodedCoreLisUrn, e.getMessage());
                }
            case 404:
                throw ResponseException.notFound("Die Codeliste mit der URN %s wurde nicht gefunden.", encodedCoreLisUrn);
            default:
                throw ResponseException.internalServerError("Fehler beim Abrufen der Codeliste mit der URN %s. Der Statuscode war %d", encodedCoreLisUrn, response.statusCode());
        }
    }

    public List<RadioFieldOption> getRadioFieldOptionCodeList(@Nonnull String codeListUrn) throws ResponseException {
        var codeList = getCodeList(codeListUrn);

        var labelColumnRef = codeList
                .getColumnSet()
                .getKey()
                .getColumnRef()
                .getRef();

        var options = new LinkedList<RadioFieldOption>();

        for (var row : codeList.getCodeList().getRow()) {
            for (var col : row.getValue()) {
                if (col.getColumnRef().equals(labelColumnRef)) {
                    options.add(RadioFieldOption.of(
                            col.getSimpleValue(),
                            col.getSimpleValue()
                    ));
                }
            }
        }

        return options;
    }

    public List<Map<String, String>> getReducedCodeList(@Nonnull String codeListUrn) throws ResponseException {
        var codeList = getCodeList(codeListUrn);

        var res = new LinkedList<Map<String, String>>();

        for (var row : codeList.getCodeList().getRow()) {
            var map = new HashMap<String, String>();

            for (var col : row.getValue()) {
                map.put(col.getColumnRef(), col.getSimpleValue());
            }

            res.add(map);
        }

        return res;
    }
}
