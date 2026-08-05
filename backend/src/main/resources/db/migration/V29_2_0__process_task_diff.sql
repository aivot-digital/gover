alter table process_instance_tasks
    add column process_data_diff jsonb default '{}'::jsonb;

create or replace function jsonb_diff_val(newVal jsonb, oldVal jsonb)
    returns jsonb as $$
declare
    result jsonb;
    v record;
    i int;
    old_elem jsonb;
    new_elem jsonb;
    diff_elem jsonb;
    arr_result jsonb;
    max_len int;
begin
    -- Case 1: Both are json Objects
    if jsonb_typeof(newVal) = 'object' and jsonb_typeof(oldVal) = 'object' then
        result = '{}'::jsonb;

        -- 1. Check for changes and deletions based on keys that existed in oldVal
        for v in select * from jsonb_each(oldVal) loop
                if newVal ? v.key then
                    if newVal -> v.key = v.value then
                        continue; -- Identical, skip it
                    else
                        -- Value changed: Recurse to find the specific historical parts
                        diff_elem = jsonb_diff_val(newVal -> v.key, v.value);
                        if diff_elem is not null and diff_elem != '{}'::jsonb and diff_elem != '[]'::jsonb then
                            result = result || jsonb_build_object(v.key, diff_elem);
                        end if;
                    end if;
                else
                    -- Key was entirely deleted in newVal. Preserve its old value.
                    result = result || jsonb_build_object(v.key, v.value);
                end if;
            end loop;

        -- 2. Check for brand new keys added in newVal
        for v in select * from jsonb_each(newVal) loop
                if not (oldVal ? v.key) then
                    -- Key is brand new, meaning its previous value was non-existent ('null')
                    result = result || jsonb_build_object(v.key, 'null'::jsonb);
                end if;
            end loop;

        return result;

        -- Case 2: Both are json Arrays
    elsif jsonb_typeof(newVal) = 'array' and jsonb_typeof(oldVal) = 'array' then
        arr_result = '[]'::jsonb;
        max_len = greatest(jsonb_array_length(newVal), jsonb_array_length(oldVal));

        for i in 0 .. max_len - 1 loop
                new_elem = newVal -> i;
                old_elem = oldVal -> i;

                if old_elem is null then
                    -- Element was added out-of-bounds in newVal. It didn't exist before.
                    arr_result = arr_result || jsonb_build_array('null'::jsonb);
                elsif new_elem is null then
                    -- Element was truncated/deleted in newVal. Keep the old element.
                    arr_result = arr_result || jsonb_build_array(old_elem);
                else
                    -- Both exist. Check if they match.
                    if old_elem = new_elem then
                        -- To keep array positions aligned between diff and original array,
                        -- we leave an empty placeholder object '{}' for unchanged items.
                        arr_result = arr_result || jsonb_build_array('{}'::jsonb);
                    else
                        diff_elem = jsonb_diff_val(new_elem, old_elem);
                        arr_result = arr_result || jsonb_build_array(coalesce(diff_elem, '{}'::jsonb));
                    end if;
                end if;
            end loop;

        return arr_result;

        -- Case 3: Flat values (Scalars) or completely mismatched types
    else
        if newVal = oldVal then
            return null;
        else
            -- Core Change: Return oldVal instead of newVal to preserve history
            return oldVal;
        end if;
    end if;
end;
$$ language plpgsql;

create or replace function handle_process_task_completion()
    returns trigger as
$$
declare
    old_process_data jsonb;
begin
    if NEW.previous_process_instance_task_id is not null then
        old_process_data :=
                (select process_data
                 from process_instance_tasks
                 where id = NEW.previous_process_instance_task_id);
    end if;

    if old_process_data is null then
        old_process_data := '{}'::jsonb;
    end if;

    update process_instance_tasks
    set process_data_diff = jsonb_diff_val(NEW.process_data, old_process_data)
    where id = NEW.id;

    return NEW;
end
$$ language plpgsql;

create trigger trg_process_task_completion
    after insert or update of status
    on process_instance_tasks
    for each row
    when (NEW.status = 3) -- Stable database value of ProcessTaskStatus.Completed
execute function handle_process_task_completion();
