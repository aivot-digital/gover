-- Code list columns are referenced through stable keys instead of their position, so a source that reorders its
-- columns no longer changes which cells a reference reads. Value and label columns are chosen per element.
drop view if exists v_code_list_items;

alter table code_lists
    add column if not exists column_mappings jsonb default '[]'::jsonb not null,
    add column if not exists key_column_key  text;

-- Every existing column becomes a mapping whose key is the trimmed source column name. Blank names receive a
-- positional name. A key that is already used gets the suffix _2, _3, ..., like new columns during a synchronization,
-- so all keys of a list are unique.
do
$$
    declare
        code_list record;
        position  integer;
        name      text;
        key       text;
        suffix    integer;
        used_keys text[];
        mappings  jsonb;
    begin
        for code_list in select id, columns from code_lists where column_mappings = '[]'::jsonb
            loop
                used_keys := array []::text[];
                mappings := '[]'::jsonb;
                for position in 1 .. coalesce(array_length(code_list.columns, 1), 0)
                    loop
                        name := coalesce(nullif(trim(code_list.columns[position]), ''), 'COL' || (position - 1));
                        key := name;
                        suffix := 2;
                        while key = any (used_keys)
                            loop
                                key := name || '_' || suffix;
                                suffix := suffix + 1;
                            end loop;
                        used_keys := used_keys || key;
                        mappings := mappings || jsonb_build_object('key', key, 'name', name, 'index', position);
                    end loop;
                update code_lists set column_mappings = mappings where id = code_list.id;
            end loop;
    end
$$;

-- The former value column identifies the rows, e.g. when outdated rows are kept during a synchronization.
update code_lists cl
set key_column_key = (select mapping ->> 'key'
                      from jsonb_array_elements(cl.column_mappings) mapping
                      where (mapping ->> 'index')::integer = cl.value_column_index + 1)
where cl.key_column_key is null;

alter table code_lists
    drop column if exists value_column_index,
    drop column if exists label_column_index;
