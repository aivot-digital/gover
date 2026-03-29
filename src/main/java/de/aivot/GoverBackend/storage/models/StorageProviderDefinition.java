package de.aivot.GoverBackend.storage.models;

import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugin.enums.PluginComponentType;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.storage.exceptions.StorageException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Optional;

/**
 * Das Interface StorageProviderDefinition definiert die Struktur und das Verhalten eines Speicheranbieters innerhalb des Systems. Es erweitert das
 * {@link PluginComponent}-Interface, was bedeutet, dass jede Implementierung von StorageProviderDefinition als Plugin-Komponente betrachtet wird und somit in das Plugin-System
 * integriert werden kann.
 * <p>
 * Speicheranbieter interagieren mit Speichersystemen, um Dokument und Ordner zu erstellen, zu lesen, zu aktualisieren und zu löschen. Sie bieten eine Abstraktionsebene über
 * verschiedenen Speichersystemen, sodass das System flexibel und erweiterbar bleibt.
 * <p>
 * Speicheranbieter definieren ihr eigenes Dateisystem, welches innerhalb des Dateisystems des Speichersystems organisiert ist. Dieses Dateisystem besteht aus Ordnern und
 * Dokumenten, die durch Pfade identifiziert werden, die von einem Stammverzeichnis ausgehen. So ist beispielsweise ein lokaler Speicheranbieter definiert, welcher als
 * Stammverzeichnis den Ordner {@code "C:\\GoverStorage"} hat. Pfade innerhalb dieses Speicheranbieters werden mit {@code "/pfad/zum/dokument.pdf"} angegeben. Der Speicheranbieter
 * organisiert diese dann innerhalb des Ordners {@code "C:\\GoverStorage\pfad\zum\dokument.pdf"} im Dateisystem des Speichersystems.
 * <p>
 * Jede Implementierung von StorageProviderDefinition muss die Methoden definieren, um die Verbindung zum Speichersystem zu testen, Dokumente und Ordner zu verwalten und die
 * Unterstützung von Metadatenattributen anzugeben. Darüber hinaus müssen sie die Konfigurationsklasse und das Layout für die Konfiguration bereitstellen, um die Integration in das
 * System zu ermöglichen.
 *
 * @param <T> Der Typ der Konfigurationsdaten, die für die Initialisierung und Verwaltung des Speicheranbieters verwendet werden. Diese Konfigurationsdaten werden aus
 *            Gover-UI-Definitionen abgeleitet und enthalten alle notwendigen Informationen, um eine Verbindung zum Speichersystem herzustellen und es zu verwalten.
 */
public interface StorageProviderDefinition<T> extends PluginComponent {
    /**
     * Gibt den Typ der Komponente zurück, in diesem Fall immer {@link PluginComponentType#StorageProviderDefinition}.
     *
     * @return Der Typ der Komponente, immer {@link PluginComponentType#StorageProviderDefinition}.
     */
    @Nonnull
    @Override
    default PluginComponentType getComponentType() {
        return PluginComponentType.StorageProviderDefinition;
    }

    /**
     * Gibt an, ob der Speicheranbieter die Angabe von Metadatenattributen unterstützt. Metadatenattribute sind zusätzliche Felder, welche beim Speichern und Lesen von Dokumenten
     * an das Speichersystem übergeben werden, bzw. ausgelesen werden.
     *
     * @return true, wenn die Angabe von Metadatenattributen unterstützt wird, andernfalls false.
     */
    @Nonnull
    Boolean getSupportsMetadataAttributes();

    /**
     * Gibt das Layout für die Konfiguration des Speicheranbieters zurück. Dieses Layout definiert, wie die Konfigurationsdaten für den Speicheranbieter in der Benutzeroberfläche
     * dargestellt und eingegeben werden sollen.
     *
     * @return Das Layout-Element, das das Layout für die Konfiguration des Speicheranbieters definiert, oder {@code null}, wenn kein Layout definiert ist.
     * @throws ResponseException Wenn ein Fehler bei der Erstellung des Layouts auftritt, z.B. wenn erforderliche Informationen fehlen oder ungültig sind.
     */
    @Nullable
    ConfigLayoutElement getProviderConfigLayout() throws ResponseException;

    /**
     * Gibt die Klasse zurück, die die Konfigurationsdaten für den Speicheranbieter repräsentiert. Diese Klasse muss die Struktur der Konfigurationsdaten definieren, die für die
     * Initialisierung und Verwaltung des Speicheranbieters erforderlich sind.
     *
     * @return Die Klasse, die die Konfigurationsdaten für den Speicheranbieter repräsentiert.
     */
    Class<T> getConfigClass();

    /**
     * Initialisiert den Speicheranbieter mit den gegebenen Konfigurationsdaten. Diese Methode wird während der Synchronisation aufgerufen, bevor der Speicheranbieter verwendet
     * wird, um sicherzustellen, dass er korrekt konfiguriert und bereit ist, mit dem Speichersystem zu interagieren.
     *
     * @param config Die Konfigurationsdaten, die für die Initialisierung des Speicheranbieters verwendet werden. Diese Daten müssen der Struktur entsprechen, die durch die Klasse
     *               definiert ist, die von {@link #getConfigClass()} zurückgegeben wird.
     * @throws StorageException Wird geworfen, wenn ein Fehler bei der Initialisierung des Speicheranbieters auftritt, z.B. wenn die Konfigurationsdaten ungültig sind oder wenn
     *                          eine Verbindung zum Speichersystem nicht hergestellt werden kann.
     */
    void initializeProvider(@Nonnull T config) throws StorageException;

    /**
     * Bestimmt, ob eine Resynchronisation des Speicheranbieters erforderlich ist, basierend auf den alten und neuen Konfigurationsdaten. Diese Methode wird aufgerufen, wenn die
     * Konfiguration des Speicheranbieters aktualisiert wird, um zu entscheiden, ob der Speicheranbieter neu initialisiert werden muss, um die Änderungen in der Konfiguration zu
     * berücksichtigen.
     *
     * @param oldConfig Die alten Konfigurationsdaten, die vor der Aktualisierung gültig waren. Diese Daten können {@code null} sein, wenn der Speicheranbieter zuvor nicht
     *                  konfiguriert war.
     * @param newConfig Die neuen Konfigurationsdaten, die nach der Aktualisierung gültig sind. Diese Daten dürfen nicht {@code null} sein.
     * @return true, wenn eine Resynchronisation des Speicheranbieters erforderlich ist, andernfalls false. Eine Resynchronisation ist erforderlich, wenn die Änderungen in der
     * Konfiguration Auswirkungen auf die Funktionsweise des Speicheranbieters haben, z.B. wenn sich die Verbindungsinformationen zum Speichersystem geändert haben oder wenn neue
     * Funktionen aktiviert wurden, die eine Neuinitialisierung erfordern.
     */
    boolean shouldResync(@Nullable T oldConfig, @Nonnull T newConfig);

    /**
     * Testet die Verbindung zum Speichersystem mit den gegebenen Konfigurationsdaten. Wenn {@code mustCheckWritable} {@code true} ist, soll zusätzlich geprüft werden, ob das
     * Speichersystem beschreibbar ist. Wenn diese Flag nicht gesetzt ist, <u>muss die Prüfung auf Schreibrechte nicht zwingend erfolgen</u>, es sei denn, es ist für die Verbindung
     * zum Speicher erforderlich.
     * </p>
     * Wenn die Verbindung nicht erfolgreich hergestellt werden kann, soll eine {@link StorageException} mit einer aussagekräftigen Fehlermeldung geworfen werden.
     *
     * @param config            Die Konfigurationsdaten für die Verbindung zum Speichersystem.
     * @param mustCheckWritable Gibt an, ob zusätzlich geprüft werden <u>muss</u>, ob das Speichersystem beschreibbar ist.
     * @throws StorageException Wird geworfen, wenn die Verbindung zum Speichersystem nicht erfolgreich hergestellt werden kann oder wenn mustCheckWritable true ist und der
     *                          Speicher nicht beschreibbar ist.
     */
    void testConnection(@Nonnull T config, @Nonnull Boolean mustCheckWritable) throws StorageException;

    /**
     * Gibt das Stammverzeichnis des Speicheranbieters zurück. Das Stammverzeichnis ist der oberste Ordner im Speicheranbieter, von dem aus alle anderen Ordner und Dokumente
     * organisiert sind. Die Methode ruft intern {@link #retrieveFolder(T, String)} mit dem Pfad {@code "/"} auf, um das Stammverzeichnis zu erhalten. Wenn das Stammverzeichnis
     * nicht existiert, wird eine {@link StorageException} mit einer aussagekräftigen Fehlermeldung geworfen.
     *
     * @param config Die Konfigurationsdaten für die Verbindung zum Speichersystem, die benötigt werden, um das Stammverzeichnis zu identifizieren und darauf zuzugreifen.
     * @return Das {@link StorageFolder}-Objekt, das das Stammverzeichnis des Speicheranbieters repräsentiert.
     * @throws StorageException Wird geworfen, wenn das Stammverzeichnis nicht existiert oder wenn ein Fehler bei der Verbindung zum Speichersystem auftritt, der das Abrufen des
     *                          Stammverzeichnisses verhindert. Die Fehlermeldung sollte klar angeben, dass das Stammverzeichnis nicht gefunden wurde oder nicht zugänglich ist.
     */
    @Nonnull
    default StorageFolder rootFolder(@Nonnull T config) throws StorageException {
        return rootFolder(config, false);
    }

    /**
     * Gibt das Stammverzeichnis des Speicheranbieters zurück. Das Stammverzeichnis ist der oberste Ordner im Speicheranbieter, von dem aus alle anderen Ordner und Dokumente
     * organisiert sind. Die Methode ruft intern {@link #retrieveFolder(T, String)} mit dem Pfad {@code "/"} auf, um das Stammverzeichnis zu erhalten. Wenn das Stammverzeichnis
     * nicht existiert, wird eine {@link StorageException} mit einer aussagekräftigen Fehlermeldung geworfen.
     *
     * @param config    Die Konfigurationsdaten für die Verbindung zum Speichersystem, die benötigt werden, um das Stammverzeichnis zu identifizieren und darauf zuzugreifen.
     * @param recursive Gibt an, ob die Methode ebenfalls rekursiv die Inhalte alle Unterordner im Stammverzeichnis abrufen soll. Wenn true ist, werden die Inhalte aller Unterorder
     *                  im Stammverzeichnis ebenfalls abgerufen und in der Rückgabe des StorageFolder-Objekts enthalten sein. Wenn false ist, werden nur die Informationen über das
     *                  Stammverzeichnis selbst abgerufen, sowie die darin enthaltenen Ordner und Dokumente. Die Inhalte der Unterordner und deren Dokumente werden <u>nicht</u>
     *                  abgerufen.
     * @return Das {@link StorageFolder}-Objekt, das das Stammverzeichnis des Speicheranbieters repräsentiert.
     * @throws StorageException Wird geworfen, wenn das Stammverzeichnis nicht existiert oder wenn ein Fehler bei der Verbindung zum Speichersystem auftritt, der das Abrufen des
     *                          Stammverzeichnisses verhindert. Die Fehlermeldung sollte klar angeben, dass das Stammverzeichnis nicht gefunden wurde oder nicht zugänglich ist.
     */
    @Nonnull
    default StorageFolder rootFolder(@Nonnull T config, boolean recursive) throws StorageException {
        return retrieveFolder(config, "/", recursive)
                .orElseThrow(() -> new StorageException("Das Stammverzeichnis existiert nicht."));
    }

    /**
     * Erstellt einen neuen Ordner im Speicheranbieter an dem angegebenen Pfad. Der Pfad ist relativ zum Stammverzeichnis des Speicheranbieters und muss mit einem Schrägstrich
     * ("/") beginnen. Beispielsweise würde der Pfad {@code "/neuerOrdner"} einen neuen Ordner namens "neuerOrdner" im Stammverzeichnis erstellen, während der Pfad
     * {@code "/ordner1/ordner2"} einen neuen Ordner namens "ordner2" innerhalb von "ordner1" erstellen würde.
     * <p>
     * Wenn der Pfad ungültig ist, z.B. wenn er nicht mit einem Schrägstrich beginnt oder wenn er ungültige Zeichen enthält, soll eine {@link StorageException} mit einer
     * aussagekräftigen Fehlermeldung geworfen werden.
     * <p>
     * Wenn ein Ordner bereits existiert, soll <u>keine</u> Fehlermeldung geworfen werden, sondern stattdessen das bestehende {@link StorageFolder}-Objekt zurückgegeben werden.
     * <p>
     * Tiefe Ordnerstrukturen sollten <u>nicht</u> unterstützt werden, d.h. wenn der Pfad {@code "/ordner1/ordner2"} angegeben wird, sollte zuerst überprüft werden, ob "ordner1"
     * existiert, und wenn nicht, sollte eine {@link StorageException} mit einer aussagekräftigen Fehlermeldung geworfen werden, die klar angibt, dass der übergeordnete Ordner
     * "ordner1" nicht existiert. Wenn "ordner1" existiert, sollte dann "ordner2" erstellt werden.
     *
     * @param config       Die Konfigurationsdaten für die Verbindung zum Speichersystem, die benötigt werden, um den Ordner zu erstellen und darauf zuzugreifen.
     * @param pathFromRoot Der Pfad, relativ zum Stammverzeichnis des Speicheranbieters, an dem der neue Ordner erstellt werden soll. Der Pfad muss mit einem Schrägstrich ("/")
     *                     beginnen und darf keine ungültigen Zeichen enthalten. Beispielsweise würde der Pfad {@code "/neuerOrdner"} einen neuen Ordner namens "neuerOrdner" im
     *                     Stammverzeichnis erstellen, während der Pfad {@code "/ordner1/ordner2"} einen neuen Ordner namens "ordner2" innerhalb von "ordner1" erstellen würde.
     * @return Das {@link StorageFolder}-Objekt, das den neu erstellten Ordner repräsentiert.
     * @throws StorageException Wird geworfen, wenn ein Fehler bei der Erstellung des Ordners auftritt, z.B. wenn der Pfad ungültig ist oder wenn ein Fehler bei der Verbindung zum
     *                          Speichersystem auftritt, der die Erstellung des Ordners verhindert.
     */
    @Nonnull
    StorageFolder createFolder(@Nonnull T config, @Nonnull String pathFromRoot) throws StorageException;

    /**
     * Ruft den Ordner im Speicheranbieter ab, der durch den angegebenen Pfad identifiziert wird. Der Pfad ist relativ zum Stammverzeichnis des Speicheranbieters und muss mit einem
     * Schrägstrich ("/") beginnen. Beispielsweise würde der Pfad {@code "/ordner1/ordner2"} den Ordner namens "ordner2" innerhalb von "ordner1" abrufen.
     * <p>
     * Wenn der Pfad ungültig ist, z.B. wenn er nicht mit einem Schrägstrich beginnt oder wenn er ungültige Zeichen enthält, soll eine {@link StorageException} mit einer
     * aussagekräftigen Fehlermeldung geworfen werden.
     * <p>
     * Wenn der Ordner nicht existiert, soll ein leeres {@link Optional} zurückgegeben werden.
     * <p>
     * Wenn der Ordner existiert, soll ein {@link Optional} mit einem {@link StorageFolder}-Objekt zurückgegeben werden, das den Ordner repräsentiert.
     *
     * @param config       Die Konfigurationsdaten für die Verbindung zum Speichersystem, die benötigt werden, um den Ordner zu identifizieren und darauf zuzugreifen.
     * @param pathFromRoot Der Pfad, relativ zum Stammverzeichnis des Speicheranbieters, der den Ordner identifiziert, der abgerufen werden soll. Der Pfad muss mit einem
     *                     Schrägstrich ("/") beginnen
     * @return Ein Optional, das ein {@link StorageFolder}-Objekt enthält, das den abgerufenen Ordner repräsentiert, wenn er existiert, oder leer ist, wenn der Ordner nicht
     * existiert.
     * @throws StorageException Wird geworfen, wenn ein Fehler bei der Verbindung zum Speichersystem auftritt, der das Abrufen des Ordners verhindert, oder wenn der Pfad ungültig
     *                          ist. Die Fehlermeldung sollte klar angeben, dass der Ordner nicht gefunden wurde oder dass der Pfad ungültig ist.
     */
    @Nonnull
    default Optional<StorageFolder> retrieveFolder(@Nonnull T config, @Nonnull String pathFromRoot) throws StorageException {
        return retrieveFolder(config, pathFromRoot, false);
    }

    /**
     * Ruft den Ordner im Speicheranbieter ab, der durch den angegebenen Pfad identifiziert wird. Der Pfad ist relativ zum Stammverzeichnis des Speicheranbieters und muss mit einem
     * Schrägstrich ("/") beginnen. Beispielsweise würde der Pfad {@code "/ordner1/ordner2"} den Ordner namens "ordner2" innerhalb von "ordner1" abrufen.
     * <p>
     * Wenn der Pfad ungültig ist, z.B. wenn er nicht mit einem Schrägstrich beginnt oder wenn er ungültige Zeichen enthält, soll eine {@link StorageException} mit einer
     * aussagekräftigen Fehlermeldung geworfen werden.
     * <p>
     * Wenn der Ordner nicht existiert, soll ein leeres {@link Optional} zurückgegeben werden.
     * <p>
     * Wenn der Ordner existiert, soll ein {@link Optional} mit einem {@link StorageFolder}-Objekt zurückgegeben werden, das den Ordner repräsentiert.
     * <p>
     * Wenn die Flag {@code recursive} auf {@code true} gesetzt ist, sollten zusätzlich die Inhalte aller Unterordner und Dokumente im Ordner abgerufen und in der Rückgabe des
     * {@link StorageFolder}-Objekts enthalten sein. Wenn die Flag {@code recursive} auf {@code false} gesetzt ist, sollten nur die Informationen über den Ordner selbst, sowie die
     * direkten Inhalte (Dokumente und Ordner) abgerufen werden, ohne die Inhalte aller Unterordner zu berücksichtigen.
     *
     * @param config       Die Konfigurationsdaten für die Verbindung zum Speichersystem, die benötigt werden, um den Ordner zu identifizieren und darauf zuzugreifen.
     * @param pathFromRoot Der Pfad, relativ zum Stammverzeichnis des Speicheranbieters, der den Ordner identifiziert, der abgerufen werden soll. Der Pfad muss mit einem
     *                     Schrägstrich ("/") beginnen
     * @param recursive    Gibt an, ob die Methode ebenfalls rekursiv die Inhalte alle Unterordner im Ordner abrufen soll. Wenn true ist, werden die Inhalte aller Unterorder im
     *                     Ordner ebenfalls abgerufen und in der Rückgabe des StorageFolder-Objekts enthalten sein. Wenn false ist, werden nur die Informationen über den Ordner
     *                     selbst, sowie die direkten Inhalte (Dokumente und Ordner) abgerufen, ohne die Inhalte aller Unterordner zu berücksichtigen.
     * @return Ein Optional, das ein {@link StorageFolder}-Objekt enthält, das den abgerufenen Ordner repräsentiert, wenn er existiert, oder leer ist, wenn der Ordner nicht
     * existiert.
     * @throws StorageException Wird geworfen, wenn ein Fehler bei der Verbindung zum Speichersystem auftritt, der das Abrufen des Ordners verhindert, oder wenn der Pfad ungültig
     *                          ist. Die Fehlermeldung sollte klar angeben, dass der Ordner nicht gefunden wurde oder dass der Pfad ungültig ist.
     */
    @Nonnull
    Optional<StorageFolder> retrieveFolder(@Nonnull T config, @Nonnull String pathFromRoot, boolean recursive) throws StorageException;

    /**
     * Bestimmt, ob ein Ordner im Speicheranbieter existiert, der durch den angegebenen Pfad identifiziert wird. Der Pfad ist relativ zum Stammverzeichnis des Speicheranbieters und
     * muss mit einem Schrägstrich ("/") beginnen. Beispielsweise würde der Pfad {@code "/ordner1/ordner2"} überprüfen, ob ein Ordner namens "ordner2" innerhalb von "ordner1"
     * existiert.
     * <p>
     * Wenn der Pfad ungültig ist, z.B. wenn er nicht mit einem Schrägstrich beginnt oder wenn er ungültige Zeichen enthält, soll eine {@link StorageException} mit einer
     * aussagekräftigen Fehlermeldung geworfen werden.
     * <p>
     * Wenn der Ordner existiert, soll {@code true} zurückgegeben werden, andernfalls {@code false}.
     *
     * @param config       Die Konfigurationsdaten für die Verbindung zum Speichersystem, die benötigt werden, um den Ordner zu identifizieren und darauf zuzugreifen.
     * @param pathFromRoot Der Pfad, relativ zum Stammverzeichnis des Speicheranbieters, der den Ordner identifiziert, dessen Existenz überprüft werden soll. Der Pfad muss mit
     *                     einem Schrägstrich ("/") beginnen
     * @return true, wenn ein Ordner existiert, der durch den angegebenen Pfad identifiziert wird, andernfalls false.
     * @throws StorageException Wird geworfen, wenn ein Fehler bei der Verbindung zum Speichersystem auftritt, der die Überprüfung der Existenz des Ordners verhindert, oder wenn
     *                          der Pfad ungültig ist. Die Fehlermeldung sollte klar angeben, dass die Überprüfung der Existenz des Ordners nicht durchgeführt werden konnte oder
     *                          dass der Pfad ungültig ist.
     */
    boolean folderExists(@Nonnull T config, @Nonnull String pathFromRoot) throws StorageException;

    /**
     * Verschiebt einen Ordner innerhalb des Speicheranbieters von einem Quellpfad zu einem Zielpfad. Quell- und Zielpfad sind relativ zum Stammverzeichnis und müssen jeweils mit
     * einem Schrägstrich ("/") beginnen.
     * <p>
     * Wenn der Quellordner nicht existiert, soll eine {@link StorageException} geworfen werden.
     *
     * @param config             Die Konfigurationsdaten für die Verbindung zum Speichersystem.
     * @param sourcePathFromRoot Der Quellpfad des zu verschiebenden Ordners, relativ zum Stammverzeichnis.
     * @param targetPathFromRoot Der Zielpfad, relativ zum Stammverzeichnis.
     * @return Das {@link StorageFolder}-Objekt des verschobenen Ordners am Zielpfad.
     * @throws StorageException Wird geworfen, wenn der Ordner nicht verschoben werden konnte, z.B. bei ungültigen Pfaden, fehlendem Quellordner oder Verbindungsfehlern.
     */
    @Nonnull
    StorageFolder moveFolder(@Nonnull T config,
                             @Nonnull String sourcePathFromRoot,
                             @Nonnull String targetPathFromRoot) throws StorageException;

    /**
     * Kopiert einen Ordner innerhalb des Speicheranbieters von einem Quellpfad zu einem Zielpfad. Quell- und Zielpfad sind relativ zum Stammverzeichnis und müssen jeweils mit
     * einem Schrägstrich ("/") beginnen.
     * <p>
     * Wenn der Quellordner nicht existiert, soll eine {@link StorageException} geworfen werden.
     *
     * @param config             Die Konfigurationsdaten für die Verbindung zum Speichersystem.
     * @param sourcePathFromRoot Der Quellpfad des zu kopierenden Ordners, relativ zum Stammverzeichnis.
     * @param targetPathFromRoot Der Zielpfad, relativ zum Stammverzeichnis.
     * @return Das {@link StorageFolder}-Objekt des kopierten Ordners am Zielpfad.
     * @throws StorageException Wird geworfen, wenn der Ordner nicht kopiert werden konnte, z.B. bei ungültigen Pfaden, fehlendem Quellordner oder Verbindungsfehlern.
     */
    @Nonnull
    StorageFolder copyFolder(@Nonnull T config,
                             @Nonnull String sourcePathFromRoot,
                             @Nonnull String targetPathFromRoot) throws StorageException;

    /**
     * Löscht den Ordner im Speicheranbieter, der durch den angegebenen Pfad identifiziert wird. Der Pfad ist relativ zum Stammverzeichnis des Speicheranbieters und muss mit einem
     * Schrägstrich ("/") beginnen. Beispielsweise würde der Pfad {@code "/ordner1/ordner2"} den Ordner namens "ordner2" innerhalb von "ordner1" löschen.
     * <p>
     * Wenn der Pfad ungültig ist, z.B. wenn er nicht mit einem Schrägstrich beginnt oder wenn er ungültige Zeichen enthält, soll eine {@link StorageException} mit einer
     * aussagekräftigen Fehlermeldung geworfen werden.
     * <p>
     * Nicht existierende Ordner werden als bereits gelöscht betrachtet, d.h. wenn der Ordner nicht existiert, soll <u>keine</u> Fehlermeldung geworfen werden, sondern die Methode
     * soll einfach ohne Aktion zurückkehren.
     *
     * @param config       Die Konfigurationsdaten für die Verbindung zum Speichersystem, die benötigt werden, um den Ordner zu identifizieren und darauf zuzugreifen.
     * @param pathFromRoot Der Pfad, relativ zum Stammverzeichnis des Speicheranbieters, der den Ordner identifiziert, der gelöscht werden soll. Der Pfad muss mit einem
     *                     Schrägstrich ("/") beginnen
     * @throws StorageException Wird geworfen, wenn ein Fehler bei der Verbindung zum Speichersystem auftritt, der das Löschen des Ordners verhindert, oder wenn der Pfad ungültig
     *                          ist. Die Fehlermeldung sollte klar angeben, dass der Ordner nicht gelöscht werden konnte oder dass der Pfad ungültig ist.
     */
    void deleteFolder(@Nonnull T config, @Nonnull String pathFromRoot) throws StorageException;

    /**
     * Speichert ein Dokument im Speicheranbieter an dem angegebenen Pfad. Der Pfad ist relativ zum Stammverzeichnis des Speicheranbieters und muss mit einem Schrägstrich ("/")
     * beginnen. Beispielsweise würde der Pfad {@code "/ordner1/dokument.pdf"} ein neues Dokument namens "dokument.pdf" innerhalb von "ordner1" erstellen oder aktualisieren.
     * <p>
     * Wenn der Pfad ungültig ist, z.B. wenn er nicht mit einem Schrägstrich beginnt oder wenn er ungültige Zeichen enthält, soll eine {@link StorageException} mit einer
     * aussagekräftigen Fehlermeldung geworfen werden.
     * <p>
     * Wenn ein Dokument bereits existiert, soll es überschrieben und mit den neuen Daten und Metadaten aktualisiert werden.
     * <p>
     * Nicht existierende Ordnerstrukturen sollten <u>nicht</u> unterstützt werden, d.h. wenn der Pfad {@code "/ordner1/ordner2/dokument.pdf"} angegeben wird, sollte zuerst
     * überprüft werden, ob "ordner1/ordner2" existiert, und wenn nicht, sollte eine {@link StorageException} mit einer aussagekräftigen Fehlermeldung geworfen werden, die klar
     * angibt, dass der übergeordnete Ordner "ordner1/ordner2" nicht existiert. Wenn "ordner1/ordner2" existiert, sollte dann "dokument.pdf" erstellt oder aktualisiert werden.
     *
     * @param config       Die Konfigurationsdaten für die Verbindung zum Speichersystem, die benötigt werden, um das Dokument zu speichern und darauf zuzugreifen.
     * @param pathFromRoot Der Pfad, relativ zum Stammverzeichnis des Speicheranbieters, an dem das Dokument erstellt oder aktualisiert werden soll. Der Pfad muss mit einem
     *                     Schrägstrich ("/") beginnen
     * @param data         Der Inhalt des Dokuments als InputStream, der gespeichert werden soll.
     * @param metadata     Die Metadaten, die mit dem Dokument gespeichert werden sollen. Diese Metadaten können zusätzliche Informationen über das Dokument enthalten. Die
     *                     Unterstützung von Metadatenattributen hängt von der Implementierung des Speicheranbieters ab, und es sollte die Methode
     *                     {@link #getSupportsMetadataAttributes()} überprüft werden, um festzustellen, ob die Angabe von Metadatenattributen unterstützt wird. Wenn die Angabe von
     *                     Metadatenattributen nicht unterstützt wird, sollten die übergebenen Metadaten ignoriert werden.
     * @return Das {@link StorageDocument}-Objekt, das das gespeicherte Dokument repräsentiert, einschließlich aller relevanten Informationen wie Pfad, Name, Größe und Metadaten.
     * @throws StorageException Wird geworfen, wenn ein Fehler bei der Speicherung des Dokuments auftritt, z.B. wenn der Pfad ungültig ist, wenn die übergeordneten Ordner nicht
     *                          existieren oder wenn ein Fehler bei der Verbindung zum Speichersystem auftritt, der die Speicherung des Dokuments verhindert. Die Fehlermeldung
     *                          sollte klar angeben, dass das Dokument nicht gespeichert werden konnte oder dass der Pfad ungültig ist.
     */
    @Nonnull
    StorageDocument storeDocument(@Nonnull T config,
                                  @Nonnull String pathFromRoot,
                                  @Nonnull InputStream data,
                                  @Nonnull StorageItemMetadata metadata) throws StorageException;

    /**
     * Ruft das Dokument im Speicheranbieter ab, das durch den angegebenen Pfad identifiziert wird. Der Pfad ist relativ zum Stammverzeichnis des Speicheranbieters und muss mit
     * einem Schrägstrich ("/") beginnen. Beispielsweise würde der Pfad {@code "/ordner1/dokument.pdf"} das Dokument namens "dokument.pdf" innerhalb von "ordner1" abrufen.
     * <p>
     * Wenn der Pfad ungültig ist, z.B. wenn er nicht mit einem Schrägstrich beginnt oder wenn er ungültige Zeichen enthält, soll eine {@link StorageException} mit einer
     * aussagekräftigen Fehlermeldung geworfen werden.
     * <p>
     * Wenn das Dokument nicht existiert, soll ein leeres {@link Optional} zurückgegeben werden.
     * <p>
     * Wenn das Dokument existiert, soll ein {@link Optional} mit einem {@link StorageDocument}-Objekt zurückgegeben werden, das das Dokument repräsentiert, einschließlich aller
     * relevanten Informationen wie Pfad, Name, Größe und Metadaten.
     *
     * @param config       Die Konfigurationsdaten für die Verbindung zum Speichersystem, die benötigt werden, um das Dokument zu identifizieren und darauf zuzugreifen.
     * @param pathFromRoot Der Pfad, relativ zum Stammverzeichnis des Speicheranbieters, der das Dokument identifiziert, das abgerufen werden soll. Der Pfad muss mit einem
     *                     Schrägstrich ("/")
     * @return Ein Optional, das ein {@link StorageDocument}-Objekt enthält, das das abgerufene Dokument repräsentiert, wenn es existiert, oder leer ist, wenn das Dokument nicht
     * existiert.
     * @throws StorageException Wird geworfen, wenn ein Fehler bei der Verbindung zum Speichersystem auftritt, der das Abrufen des Dokuments verhindert, oder wenn der Pfad ungültig
     *                          ist. Die Fehlermeldung sollte klar angeben, dass das Dokument nicht gefunden wurde oder dass der Pfad ungültig ist.
     */
    @Nonnull
    Optional<StorageDocument> retrieveDocument(@Nonnull T config, @Nonnull String pathFromRoot) throws StorageException;

    /**
     * Ruft den Inhalt des Dokuments im Speicheranbieter ab, das durch den angegebenen Pfad identifiziert wird. Der Pfad ist relativ zum Stammverzeichnis des Speicheranbieters und
     * muss mit einem Schrägstrich ("/") beginnen. Beispielsweise würde der Pfad {@code "/ordner1/dokument.pdf"} den Inhalt des Dokuments namens "dokument.pdf" innerhalb von
     * "ordner1" abrufen.
     * <p>
     * Wenn der Pfad ungültig ist, z.B. wenn er nicht mit einem Schrägstrich beginnt oder wenn er ungültige Zeichen enthält, soll eine {@link StorageException} mit einer
     * aussagekräftigen Fehlermeldung geworfen werden.
     * <p>
     * Wenn das Dokument nicht existiert, soll eine {@link StorageException} mit einer aussagekräftigen Fehlermeldung geworfen werden, die klar angibt, dass das Dokument nicht
     * gefunden wurde.
     * <p>
     * Wenn das Dokument existiert, soll ein {@link InputStream} zurückgegeben werden, der den Inhalt des Dokuments repräsentiert. Es ist wichtig, dass der Aufrufer diesen
     * InputStream ordnungsgemäß schließt, um Ressourcen freizugeben und potenzielle Speicherlecks zu vermeiden.
     *
     * @param config       Die Konfigurationsdaten für die Verbindung zum Speichersystem, die benötigt werden, um das Dokument zu identifizieren und darauf zuzugreifen.
     * @param pathFromRoot Der Pfad, relativ zum Stammverzeichnis des Speicheranbieters, der das Dokument identifiziert, dessen Inhalt abgerufen werden soll. Der Pfad muss mit
     *                     einem
     * @return Ein InputStream, der den Inhalt des abgerufenen Dokuments repräsentiert. Der Aufrufer ist verantwortlich für das ordnungsgemäße Schließen dieses InputStreams, um
     * Ressourcen
     * @throws StorageException Wird geworfen, wenn ein Fehler bei der Verbindung zum Speichersystem auftritt, der das Abrufen des Dokumenteninhalts verhindert, oder wenn der Pfad
     *                          ungültig ist, oder wenn das Dokument nicht existiert. Die Fehlermeldung sollte klar angeben, dass der Inhalt des Dokuments nicht abgerufen werden
     *                          konnte, dass das Dokument nicht gefunden wurde oder dass der Pfad ungültig ist.
     */
    @Nonnull
    InputStream retrieveDocumentContent(@Nonnull T config, @Nonnull String pathFromRoot) throws StorageException;

    /**
     * Bestimmt, ob ein Dokument im Speicheranbieter existiert, das durch den angegebenen Pfad identifiziert wird. Der Pfad ist relativ zum Stammverzeichnis des Speicheranbieters
     * und muss mit einem Schrägstrich ("/") beginnen. Beispielsweise würde der Pfad {@code "/ordner1/dokument.pdf"} überprüfen, ob ein Dokument namens "dokument.pdf" innerhalb von
     * "ordner1" existiert.
     * <p>
     * Wenn der Pfad ungültig ist, z.B. wenn er nicht mit einem Schrägstrich beginnt oder wenn er ungültige Zeichen enthält, soll eine {@link StorageException} mit einer
     * aussagekräftigen Fehlermeldung geworfen werden.
     * <p>
     * Wenn das Dokument existiert, soll {@code true} zurückgegeben werden, andernfalls {@code false}.
     *
     * @param config       Die Konfigurationsdaten für die Verbindung zum Speichersystem, die benötigt werden, um das Dokument zu identifizieren und darauf zuzugreifen.
     * @param pathFromRoot Der Pfad, relativ zum Stammverzeichnis des Speicheranbieters, der das Dokument identifiziert, dessen Existenz überprüft werden soll. Der Pfad muss mit
     *                     einem Schrägstrich ("/") beginnen
     * @return true, wenn ein Dokument existiert, das durch den angegebenen Pfad identifiziert wird, andernfalls false.
     * @throws StorageException Wird geworfen, wenn ein Fehler bei der Verbindung zum Speichersystem auftritt, der die Überprüfung der Existenz des Dokuments verhindert, oder wenn
     *                          der Pfad ungültig ist. Die Fehlermeldung sollte klar angeben, dass die Überprüfung der Existenz des Dokuments nicht durchgeführt werden konnte oder
     *                          dass der Pfad ungültig ist.
     */
    boolean documentExists(@Nonnull T config, @Nonnull String pathFromRoot) throws StorageException;

    /**
     * Verschiebt ein Dokument innerhalb des Speicheranbieters von einem Quellpfad zu einem Zielpfad. Quell- und Zielpfad sind relativ zum Stammverzeichnis und müssen jeweils mit
     * einem Schrägstrich ("/") beginnen.
     * <p>
     * Wenn das Quelldokument nicht existiert, soll eine {@link StorageException} geworfen werden.
     * <p>
     * Wenn am Zielpfad bereits ein Dokument existiert, darf dieses überschrieben werden.
     *
     * @param config             Die Konfigurationsdaten für die Verbindung zum Speichersystem.
     * @param sourcePathFromRoot Der Quellpfad des zu verschiebenden Dokuments, relativ zum Stammverzeichnis.
     * @param targetPathFromRoot Der Zielpfad, relativ zum Stammverzeichnis.
     * @return Das {@link StorageDocument}-Objekt des verschobenen Dokuments am Zielpfad.
     * @throws StorageException Wird geworfen, wenn das Dokument nicht verschoben werden konnte, z.B. bei ungültigen Pfaden, fehlendem Quelldokument oder Verbindungsfehlern.
     */
    @Nonnull
    StorageDocument moveDocument(@Nonnull T config,
                                 @Nonnull String sourcePathFromRoot,
                                 @Nonnull String targetPathFromRoot) throws StorageException;

    /**
     * Kopiert ein Dokument innerhalb des Speicheranbieters von einem Quellpfad zu einem Zielpfad. Quell- und Zielpfad sind relativ zum Stammverzeichnis und müssen jeweils mit
     * einem Schrägstrich ("/") beginnen.
     * <p>
     * Wenn das Quelldokument nicht existiert, soll eine {@link StorageException} geworfen werden.
     * <p>
     * Wenn am Zielpfad bereits ein Dokument existiert, darf dieses überschrieben werden.
     *
     * @param config             Die Konfigurationsdaten für die Verbindung zum Speichersystem.
     * @param sourcePathFromRoot Der Quellpfad des zu kopierenden Dokuments, relativ zum Stammverzeichnis.
     * @param targetPathFromRoot Der Zielpfad, relativ zum Stammverzeichnis.
     * @return Das {@link StorageDocument}-Objekt des kopierten Dokuments am Zielpfad.
     * @throws StorageException Wird geworfen, wenn das Dokument nicht kopiert werden konnte, z.B. bei ungültigen Pfaden, fehlendem Quelldokument oder Verbindungsfehlern.
     */
    @Nonnull
    StorageDocument copyDocument(@Nonnull T config,
                                 @Nonnull String sourcePathFromRoot,
                                 @Nonnull String targetPathFromRoot) throws StorageException;

    /**
     * Löscht das Dokument im Speicheranbieter, das durch den angegebenen Pfad identifiziert wird. Der Pfad ist relativ zum Stammverzeichnis des Speicheranbieters und muss mit
     * einem Schrägstrich ("/") beginnen. Beispielsweise würde der Pfad {@code "/ordner1/dokument.pdf"} das Dokument namens "dokument.pdf" innerhalb von "ordner1" löschen.
     * <p>
     * Wenn der Pfad ungültig ist, z.B. wenn er nicht mit einem Schrägstrich beginnt oder wenn er ungültige Zeichen enthält, soll eine {@link StorageException} mit einer
     * aussagekräftigen Fehlermeldung geworfen werden.
     * <p>
     * Nicht existierende Dokumente werden als bereits gelöscht betrachtet, d.h. wenn das Dokument nicht existiert, soll <u>keine</u> Fehlermeldung geworfen werden, sondern die
     * Methode soll einfach ohne Aktion zurückkehren.
     *
     * @param config       Die Konfigurationsdaten für die Verbindung zum Speichersystem, die benötigt werden, um das Dokument zu identifizieren und darauf zuzugreifen.
     * @param pathFromRoot Der Pfad, relativ zum Stammverzeichnis des Speicheranbieters, der das Dokument identifiziert, das gelöscht werden soll. Der Pfad muss mit einem
     *                     Schrägstrich ("/")
     * @throws StorageException Wird geworfen, wenn ein Fehler bei der Verbindung zum Speichersystem auftritt, der das Löschen des Dokuments verhindert, oder wenn der Pfad ungültig
     *                          ist. Die Fehlermeldung sollte klar angeben, dass das Dokument nicht gelöscht werden konnte oder dass der Pfad ungültig ist.
     */
    void deleteDocument(@Nonnull T config, @Nonnull String pathFromRoot) throws StorageException;
}
