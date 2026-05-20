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
begin
    -- Case 1: Both are json Objects
    if jsonb_typeof(newVal) = 'object' and jsonb_typeof(oldVal) = 'object' then
        result = newVal;
        for v in select * from jsonb_each(oldVal) loop
                -- If values are identical, remove the key from the diff
                if result -> v.key = v.value then
                    result = result - v.key;
                    -- If key exists in both but values differ, recurse deeper
                elsif result ? v.key then
                    result = result || jsonb_build_object(v.key, jsonb_diff_val(result -> v.key, v.value));
                    -- Clean up empty objects/arrays resulting from the deep diff
                    if result -> v.key = '{}'::jsonb or result -> v.key = '[]'::jsonb then
                        result = result - v.key;
                    end if;
                    -- If key was deleted in newVal, set it to 'null'
                else
                    result = result || jsonb_build_object(v.key, 'null'::jsonb);
                end if;
            end loop;
        return result;

        -- Case 2: Both are json Arrays (Compare element-by-element by index)
    elsif jsonb_typeof(newVal) = 'array' and jsonb_typeof(oldVal) = 'array' then
        arr_result = '[]'::jsonb;
        for i in 0 .. jsonb_array_length(newVal) - 1 loop
                new_elem = newVal -> i;
                old_elem = oldVal -> i; -- Returns null if old array is shorter

                if old_elem is null then
                    -- Element is brand new, keep it entirely
                    arr_result = arr_result || jsonb_build_array(new_elem);
                else
                    -- Deep diff the elements at this index
                    diff_elem = jsonb_diff_val(new_elem, old_elem);
                    -- Only add to the array if there is an actual difference
                    if diff_elem is not null and diff_elem != '{}'::jsonb then
                        arr_result = arr_result || jsonb_build_array(diff_elem);
                    end if;
                end if;
            end loop;
        return arr_result;

        -- Case 3: Flat values (Scalars) or mismatched types
    else
        if newVal = oldVal then
            return null;
        else
            return newVal;
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
    when (NEW.status = 2) -- 2 is the status ProcessTaskStatus.Completed
execute function handle_process_task_completion();