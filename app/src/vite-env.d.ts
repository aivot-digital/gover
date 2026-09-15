/// <reference types="vite-plugin-svgr/client" />
/// <reference types="vite/client" />

interface ViteTypeOptions {
    // By adding this line, you can make the type of ImportMetaEnv strict
    // to disallow unknown keys.
    // strictImportMetaEnv: unknown
}

interface ImportMetaEnv {
    readonly VITE_APP_MODE: 'customer' | 'staff';
}

interface ImportMeta {
    readonly env: ImportMetaEnv
}
