-- update all submissions with status -2 (pending destination) to -1 (pending)
update submissions set status = -1 where status = -2;