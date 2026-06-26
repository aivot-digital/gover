package de.aivot.GoverBackend.av.exceptions;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.utils.StringUtils;
import org.springframework.http.HttpStatus;

public class AVVirusFoundException extends ResponseException {
    public AVVirusFoundException(String filename) {
        super(
                HttpStatus.BAD_REQUEST,
                "In der hochgeladenen Datei wurde Schadsoftware erkannt. Der Upload wurde aus Sicherheitsgründen blockiert. Bitte bereinigen oder ersetzen Sie die Datei und laden Sie sie erneut hoch.",
                String.format("Die Datei %s wurde vom Virenscanner als unsicher eingestuft.", StringUtils.quote(filename))
        );
    }

    public AVVirusFoundException(String filename, String scannerResponse) {
        super(
                HttpStatus.BAD_REQUEST,
                "In der hochgeladenen Datei wurde Schadsoftware erkannt. Der Upload wurde aus Sicherheitsgründen blockiert. Bitte bereinigen oder ersetzen Sie die Datei und laden Sie sie erneut hoch.",
                String.format("Die Datei %s wurde vom Virenscanner als unsicher eingestuft. Scanner-Antwort: %s", StringUtils.quote(filename), scannerResponse)
        );
    }
}
