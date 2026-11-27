import {readdir, readFile, rm, stat, utimes, writeFile} from 'node:fs/promises';
import {resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
import {promisify} from 'node:util';
import {brotliCompress, constants} from 'node:zlib';

const compress = promisify(brotliCompress);

export async function compressAssets(directory) {
    // Match the immutable asset rule in container/nginx.conf; public files keep their stable URLs.
    const files = (await readdir(directory, {withFileTypes: true}))
        .filter((entry) => entry.isFile() && /-[A-Za-z0-9_-]{8}\.(js|css)$/.test(entry.name))
        .map((entry) => resolve(directory, entry.name));
    let count = 0;
    let bytes = 0;
    let index = 0;
    // Bound memory use when several large editor and worker bundles are compressed.
    await Promise.all(Array.from({length: 2}, async () => {
        while (index < files.length) {
            const file = files[index++];
            const original = await readFile(file);
            const compressed = await compress(original, {
                params: {
                    [constants.BROTLI_PARAM_QUALITY]: 11,
                    [constants.BROTLI_PARAM_MODE]: constants.BROTLI_MODE_TEXT,
                },
            });
            if (compressed.length >= original.length) {
                await rm(`${file}.br`, {force: true});
                continue;
            }
            const metadata = await stat(file);
            await writeFile(`${file}.br`, compressed);
            // Both representations belong to the same build and share Last-Modified.
            await utimes(`${file}.br`, metadata.atime, metadata.mtime);
            count += 1;
            bytes += compressed.length;
        }
    }));
    return {count, bytes};
}

if (process.argv[1] != null && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    if (process.argv.length !== 3) {
        throw new Error('Usage: node scripts/compress-assets.mjs <build-assets-directory>');
    }
    const {count, bytes} = await compressAssets(resolve(process.argv[2]));
    console.log(`Brotli: ${count} assets, ${(bytes / 1_000_000).toFixed(2)} MB precompressed`);
}
