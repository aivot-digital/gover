-- Staff members with the default system role must not create, change or delete assets. Assets are served on the
-- application origin and can be selected as templates for documents that contain submitted data. Asset management
-- stays with the administrative roles; the default role keeps read access.
update system_roles
set permissions = array_remove(
        array_remove(
                array_remove(permissions, 'asset.create'),
                'asset.update'
        ),
        'asset.delete'
                  )
where id = 3
  and permissions && array [
    'asset.create',
    'asset.update',
    'asset.delete'
    ]::varchar[];
