export function downloadTextFile(filename: string, content: string, mimetype: string): void {
    const a = document.createElement('a');
    const file = new Blob([content], {type: mimetype});
    a.href = URL.createObjectURL(file);
    a.download = filename;
    a.click();
    a.remove();
}
