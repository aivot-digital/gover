export function readFile<T>(file: File): Promise<T> {
    return new Promise<T>((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = (evt) => {
            const value = evt.target?.result;
            if (value != null) {
                try {
                    const parsedData = JSON.parse(value.toString()) as T;
                    resolve(parsedData);
                } catch (err) {
                    reject(err);
                }
            } else {
                reject(new Error('Empty reader result'));
            }
        };
        reader.onerror = reject;
        reader.readAsText(file, 'UTF-8');
    });
}
