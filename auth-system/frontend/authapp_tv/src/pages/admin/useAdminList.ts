import { useCallback, useMemo, useState } from "react";
import type { PageResponse } from "@/models/AdminModels";
import type { PageState, SortState } from "@/pages/admin/adminUi";

const defaultPageState: PageState = {
  page: 0,
  size: 10,
  totalPages: 1,
  totalElements: 0,
};

export function toPageState<T>(page: PageResponse<T>): PageState {
  return {
    page: page.page,
    size: page.size,
    totalPages: page.totalPages,
    totalElements: page.totalElements,
  };
}

export function useBulkSelection<TId>(getSelectableIds: () => TId[]) {
  const [selectedIds, setSelectedIds] = useState<TId[]>([]);

  const selectableIds = getSelectableIds();
  const selectedCount = selectedIds.length;
  const allPageSelected =
    selectableIds.length > 0 && selectableIds.every((id) => selectedIds.includes(id));

  const isSelected = useCallback((id: TId) => selectedIds.includes(id), [selectedIds]);

  const toggleItem = useCallback((id: TId, checked: boolean) => {
    setSelectedIds((current) =>
      checked ? Array.from(new Set([...current, id])) : current.filter((item) => item !== id),
    );
  }, []);

  const togglePage = useCallback(
    (checked: boolean) => {
      setSelectedIds(checked ? getSelectableIds() : []);
    },
    [getSelectableIds],
  );

  const clearSelection = useCallback(() => setSelectedIds([]), []);

  return {
    selectedIds,
    setSelectedIds,
    selectedCount,
    allPageSelected,
    isSelected,
    toggleItem,
    togglePage,
    clearSelection,
  };
}

export function useAdminClientList<TItem>({
  items,
  filter,
  sort,
  initialSort,
  pageSize = 10,
}: {
  items: TItem[];
  filter: (item: TItem) => boolean;
  sort: (left: TItem, right: TItem, sortState: SortState) => number;
  initialSort: SortState;
  pageSize?: number;
}) {
  const [pageState, setPageState] = useState<PageState>({ ...defaultPageState, size: pageSize });
  const [sortState, setSortState] = useState<SortState>(initialSort);

  const filteredItems = useMemo(() => {
    return items.filter(filter).sort((left, right) => sort(left, right, sortState));
  }, [items, filter, sort, sortState]);

  const listPageState = useMemo(
    () => ({
      ...pageState,
      totalElements: filteredItems.length,
      totalPages: Math.max(Math.ceil(filteredItems.length / pageState.size), 1),
    }),
    [filteredItems.length, pageState],
  );

  const pagedItems = useMemo(
    () => filteredItems.slice(pageState.page * pageState.size, pageState.page * pageState.size + pageState.size),
    [filteredItems, pageState.page, pageState.size],
  );

  const handleSort = useCallback((column: string) => {
    setSortState((current) => ({
      sort: column,
      direction: current.sort === column && current.direction === "ASC" ? "DESC" : "ASC",
    }));
    setPageState((current) => ({ ...current, page: 0 }));
  }, []);

  const resetPage = useCallback(() => {
    setPageState((current) => ({ ...current, page: 0 }));
  }, []);

  const setPage = useCallback((page: number) => {
    setPageState((current) => ({ ...current, page }));
  }, []);

  return {
    filteredItems,
    pagedItems,
    pageState,
    listPageState,
    sortState,
    setPageState,
    setSortState,
    handleSort,
    resetPage,
    setPage,
  };
}

export function useAdminServerList<TItem, TFilters extends Record<string, string>>({
  initialFilters,
  initialSort,
  fetchPage,
  pageSize = 10,
}: {
  initialFilters: TFilters;
  initialSort: SortState;
  fetchPage: (params: TFilters & { page: number; size: number; sort: string; direction: SortState["direction"] }) => Promise<PageResponse<TItem>>;
  pageSize?: number;
}) {
  const [items, setItems] = useState<TItem[]>([]);
  const [filters, setFilters] = useState<TFilters>(initialFilters);
  const [pageState, setPageState] = useState<PageState>({ ...defaultPageState, size: pageSize });
  const [sortState, setSortState] = useState<SortState>(initialSort);

  const load = useCallback(
    async (nextPage = pageState.page, nextSort = sortState, nextFilters = filters) => {
      const page = await fetchPage({
        ...nextFilters,
        page: nextPage,
        size: pageState.size,
        sort: nextSort.sort,
        direction: nextSort.direction,
      });
      setItems(page.content);
      setPageState(toPageState(page));
      return page;
    },
    [fetchPage, filters, pageState.page, pageState.size, sortState],
  );

  const handleFilterChange = useCallback((name: string, value: string) => {
    setFilters((current) => ({ ...current, [name]: value }));
  }, []);

  const handleSort = useCallback(
    (column: string) => {
      const nextSort: SortState = {
        sort: column,
        direction: sortState.sort === column && sortState.direction === "DESC" ? "ASC" : "DESC",
      };
      setSortState(nextSort);
      void load(0, nextSort).catch(() => undefined);
    },
    [load, sortState],
  );

  const resetFilters = useCallback(async () => {
    setFilters(initialFilters);
    await load(0, sortState, initialFilters);
  }, [initialFilters, load, sortState]);

  return {
    items,
    setItems,
    filters,
    setFilters,
    pageState,
    setPageState,
    sortState,
    setSortState,
    load,
    handleFilterChange,
    handleSort,
    resetFilters,
  };
}
