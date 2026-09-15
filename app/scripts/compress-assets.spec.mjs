import {strict as assert} from 'node:assert';
import {mkdtemp, readFile, readdir, rm, stat, utimes, writeFile} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {test} from 'node:test';
import {brotliDecompressSync} from 'node:zlib';
import {compressAssets} from './compress-assets.mjs';

test('compresses hashed JS/CSS losslessly, preserves originals and excludes stable public files', async (t) => {
    const directory = await mkdtemp(join(tmpdir(), 'prosuna-assets-'));
    t.after(() => rm(directory, {recursive: true, force: true}));
    const source = 'console.log("frontend asset");\n'.repeat(200);
    const hashed = ['index-Ab12_-34.js', 'editor.worker-Ab12_-34.js', 'index-Ab12_-34.css'];
    const stable = ['runtime.js', 'index.html', 'font-Ab12_-34.woff2', 'data-Ab12_-34.json'];
    for (const name of [...hashed, ...stable]) await writeFile(join(directory, name), source);
    const date = new Date('2025-01-01T00:00:00Z');
    await utimes(join(directory, hashed[0]), date, date);
    assert.equal((await compressAssets(directory)).count, hashed.length);
    for (const name of hashed) {
        assert.equal(await readFile(join(directory, name), 'utf8'), source);
        assert.equal(brotliDecompressSync(await readFile(join(directory, `${name}.br`))).toString(), source);
    }
    assert.equal((await stat(join(directory, `${hashed[0]}.br`))).mtimeMs, date.getTime());
    assert.deepEqual((await readdir(directory)).sort(), [...hashed, ...stable, ...hashed.map((name) => `${name}.br`)].sort());

    await writeFile(join(directory, hashed[0]), 'x');
    await compressAssets(directory);
    assert.equal((await readdir(directory)).includes(`${hashed[0]}.br`), false);
});

test('fails the build when the asset directory is missing', async () => {
    const directory = await mkdtemp(join(tmpdir(), 'prosuna-missing-assets-'));
    await rm(directory, {recursive: true});
    await assert.rejects(compressAssets(directory), {code: 'ENOENT'});
});
