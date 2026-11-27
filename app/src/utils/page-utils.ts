import {Page} from '../models/dtos/page';

export async function listAllPages<T>(
    fetchPage: (page: number) => Promise<Page<T>>,
): Promise<T[]> {
    const items: T[] = [];
    let currentPage = 0;
    let hasMore = true;

    while (hasMore) {
        const response = await fetchPage(currentPage);
        items.push(...response.content);
        hasMore = isNotLastPage(response);
        currentPage += 1;
    }

    return items;
}

export function isNotLastPage(page: Page<unknown>): boolean {
    return page.page.number <= page.page.totalPages;
}