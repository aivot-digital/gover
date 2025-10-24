import {createSlice, type PayloadAction} from '@reduxjs/toolkit';
import type {RootState} from '../store.staff';
import {ServerEntityType} from '../shells/staff/data/server-entity-type';
import {DEFAULT_SEARCH_SIZE} from '../modules/search/search-item-service';

interface EntityHistoryItem {
    type: ServerEntityType;
    link: string;
    title: string;
}

interface EntityHistoryState {
    history: EntityHistoryItem[];
}

const initialState: EntityHistoryState = {
    history: JSON.parse(localStorage.getItem('history') || '[]') as EntityHistoryItem[],
};

const entityHistorySlice = createSlice({
    name: 'entityHistory',
    initialState,
    reducers: {
        addEntityHistoryItem(state, action: PayloadAction<EntityHistoryItem>) {
            state.history = [
                action.payload,
                ...state.history.filter(item => item.link !== action.payload.link),
            ];

            if (state.history.length > DEFAULT_SEARCH_SIZE) {
                state.history = state.history.slice(0, DEFAULT_SEARCH_SIZE);
            }

            localStorage.setItem('history', JSON.stringify(state.history));
        },
    },
});

export const {
    addEntityHistoryItem,
} = entityHistorySlice.actions;

export const selectEntityHistory = (state: RootState) => state.entityHistory.history;

export const entityHistoryReducer = entityHistorySlice.reducer;
