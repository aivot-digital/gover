const sbomBasePath = '/sbom/';
const sbomManifestPath = `${sbomBasePath}manifest.json`;

export interface SbomManifest {
    schemaVersion?: number;
    name: string;
    version: string;
    commit: string;
    buildDate: string;
    generatedAt?: string;
    sbomFormat: string;
    sbomSpecVersion: string;
    componentCount?: number;
    sboms: {
        frontend: string;
        mails: string;
        backend: string;
        merged: string;
    };
    reports: string[];
}

export class SbomUnavailableError extends Error {
    public constructor() {
        super('No SBOM bundle is available for this build.');
        this.name = 'SbomUnavailableError';
    }
}

function isRecord(value: unknown): value is Record<string, unknown> {
    return value != null && typeof value === 'object' && !Array.isArray(value);
}

function readString(record: Record<string, unknown>, key: string): string {
    const value = record[key];
    if (typeof value !== 'string' || value.trim().length === 0) {
        throw new Error(`Invalid SBOM manifest property: ${key}`);
    }

    return value;
}

function readOptionalString(record: Record<string, unknown>, key: string): string | undefined {
    const value = record[key];
    if (value == null) {
        return undefined;
    }
    if (typeof value !== 'string' || value.trim().length === 0) {
        throw new Error(`Invalid SBOM manifest property: ${key}`);
    }

    return value;
}

function readOptionalNonNegativeInteger(record: Record<string, unknown>, key: string): number | undefined {
    const value = record[key];
    if (value == null) {
        return undefined;
    }
    if (typeof value !== 'number' || !Number.isInteger(value) || value < 0) {
        throw new Error(`Invalid SBOM manifest property: ${key}`);
    }

    return value;
}

// Manifest values become public download links, so they must stay below the SBOM root.
function isSafeSbomFilePath(path: string): boolean {
    if (path.trim().length === 0 || path.trim() !== path) {
        return false;
    }

    try {
        const baseUrl = new URL(sbomBasePath, window.location.origin);
        const resolvedUrl = new URL(path, baseUrl);
        return !path.startsWith('/') &&
            resolvedUrl.origin === baseUrl.origin &&
            resolvedUrl.pathname.startsWith(baseUrl.pathname) &&
            resolvedUrl.search.length === 0 &&
            resolvedUrl.hash.length === 0;
    } catch {
        return false;
    }
}

function readSbomFilePath(record: Record<string, unknown>, key: string): string {
    const path = readString(record, key);
    if (!isSafeSbomFilePath(path)) {
        throw new Error(`Invalid SBOM file path: ${key}`);
    }

    return path;
}

export function parseSbomManifest(value: unknown): SbomManifest {
    if (!isRecord(value)) {
        throw new Error('Invalid SBOM manifest.');
    }

    const sboms = value.sboms;
    if (!isRecord(sboms)) {
        throw new Error('Invalid SBOM manifest property: sboms');
    }

    const reports = value.reports;
    if (!Array.isArray(reports) || !reports.every((report) => typeof report === 'string' && isSafeSbomFilePath(report))) {
        throw new Error('Invalid SBOM manifest property: reports');
    }

    const schemaVersion = readOptionalNonNegativeInteger(value, 'schemaVersion');
    if (schemaVersion != null && schemaVersion !== 1) {
        throw new Error('Unsupported SBOM manifest schema version.');
    }

    return {
        schemaVersion,
        name: readString(value, 'name'),
        version: readString(value, 'version'),
        commit: readString(value, 'commit'),
        buildDate: readString(value, 'buildDate'),
        generatedAt: readOptionalString(value, 'generatedAt'),
        sbomFormat: readString(value, 'sbomFormat'),
        sbomSpecVersion: readString(value, 'sbomSpecVersion'),
        componentCount: readOptionalNonNegativeInteger(value, 'componentCount'),
        sboms: {
            frontend: readSbomFilePath(sboms, 'frontend'),
            mails: readSbomFilePath(sboms, 'mails'),
            backend: readSbomFilePath(sboms, 'backend'),
            merged: readSbomFilePath(sboms, 'merged'),
        },
        reports,
    };
}

export function createSbomFileUrl(path: string): string {
    if (!isSafeSbomFilePath(path)) {
        throw new Error('Invalid SBOM file path.');
    }

    return new URL(path, new URL(sbomBasePath, window.location.origin)).pathname;
}

export async function loadSbomManifest(signal?: AbortSignal): Promise<SbomManifest> {
    const response = await fetch(sbomManifestPath, {
        headers: {
            Accept: 'application/json',
        },
        cache: 'no-store',
        signal,
    });

    if (response.status === 404) {
        throw new SbomUnavailableError();
    }
    if (!response.ok) {
        throw new Error(`Could not load SBOM manifest (${response.status}).`);
    }
    const contentType = response.headers.get('Content-Type');
    if (contentType?.includes('text/html') === true) {
        throw new SbomUnavailableError();
    }

    let manifest: unknown;
    try {
        manifest = await response.json();
    } catch {
        throw new Error('Could not parse SBOM manifest.');
    }

    return parseSbomManifest(manifest);
}
