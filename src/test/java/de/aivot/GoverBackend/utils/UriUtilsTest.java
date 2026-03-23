package de.aivot.GoverBackend.utils;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class UriUtilsTest {

    @Test
    void testCreate_withHostnameOnly_noProtocol() {
        URI uri = UriUtils.create("example.com");
        assertEquals("http://example.com/", uri.toString());
    }

    @Test
    void testCreate_withHostnameOnly_withHttp() {
        URI uri = UriUtils.create("http://example.com");
        assertEquals("http://example.com/", uri.toString());
    }

    @Test
    void testCreate_withHostnameOnly_withHttps() {
        URI uri = UriUtils.create("https://example.com");
        assertEquals("https://example.com/", uri.toString());
    }

    @Test
    void testCreate_withHostnameAndPath() {
        URI uri = UriUtils.create("example.com", "foo");
        assertEquals("http://example.com/foo", uri.toString());
    }

    @Test
    void testCreate_withHostnameAndPath_withLeadingSlash() {
        URI uri = UriUtils.create("example.com", "/foo");
        assertEquals("http://example.com/foo", uri.toString());
    }

    @Test
    void testCreate_withHostnameWithSlashAndPath() {
        URI uri = UriUtils.create("example.com/", "foo");
        assertEquals("http://example.com/foo", uri.toString());
    }

    @Test
    void testCreate_withMultiplePathSegments() {
        URI uri = UriUtils.create("example.com", "foo", "bar");
        assertEquals("http://example.com/foo/bar", uri.toString());
    }

    @Test
    void testCreate_withMultiplePathSegmentsWithSlashes() {
        URI uri = UriUtils.create("example.com/", "/foo/", "/bar");
        assertNotEquals("http://example.com/foo//bar", uri.toString());
        assertEquals("http://example.com/foo/bar", uri.toString());
    }

    @Test
    void testCreate_withEmptyPath() {
        URI uri = UriUtils.create("example.com", "");
        assertEquals("http://example.com/", uri.toString());
    }

    @Test
    void testCreate_withEmptyHostname() {
        assertThrows(IllegalArgumentException.class, () -> UriUtils.create(""));
    }
}