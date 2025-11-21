create view v_organizational_units_shadowed as
with recursive org_unit_shadow as (select id,
                                          name,
                                          address,
                                          imprint,
                                          common_privacy,
                                          common_accessibility,
                                          technical_support_address,
                                          special_support_address,
                                          created,
                                          updated,
                                          department_mail,
                                          theme_id,
                                          technical_support_phone,
                                          technical_support_info,
                                          special_support_phone,
                                          special_support_info,
                                          additional_info,
                                          depth,
                                          parent_org_unit_id
                                   from organizational_units

                                   union all

                                   select child.id,
                                          child.name,
                                          coalesce(child.address, parent.address),
                                          coalesce(child.imprint, parent.imprint),
                                          coalesce(child.common_privacy, parent.common_privacy),
                                          coalesce(child.common_accessibility, parent.common_accessibility),
                                          coalesce(child.technical_support_address, parent.technical_support_address),
                                          coalesce(child.special_support_address, parent.special_support_address),
                                          child.created,
                                          child.updated,
                                          coalesce(child.department_mail, parent.department_mail),
                                          coalesce(child.theme_id, parent.theme_id),
                                          coalesce(child.technical_support_phone, parent.technical_support_phone),
                                          coalesce(child.technical_support_info, parent.technical_support_info),
                                          coalesce(child.special_support_phone, parent.special_support_phone),
                                          coalesce(child.special_support_info, parent.special_support_info),
                                          coalesce(child.additional_info, parent.additional_info),
                                          child.depth,
                                          child.parent_org_unit_id
                                   from org_unit_shadow parent
                                            join organizational_units child on child.parent_org_unit_id = parent.id
                                   where child.address is null
                                      or child.imprint is null
                                      or child.common_privacy is null
                                      or child.common_accessibility is null
                                      or child.technical_support_address is null
                                      or child.special_support_address is null
                                      or child.technical_support_phone is null
                                      or child.technical_support_info is null
                                      or child.special_support_phone is null
                                      or child.special_support_info is null
                                      or child.additional_info is null
                                      or child.department_mail is null)
select distinct on (id) id,
                        name,
                        address,
                        imprint,
                        common_privacy,
                        common_accessibility,
                        technical_support_address,
                        special_support_address,
                        created,
                        updated,
                        department_mail,
                        theme_id,
                        technical_support_phone,
                        technical_support_info,
                        special_support_phone,
                        special_support_info,
                        additional_info,
                        depth,
                        parent_org_unit_id
from org_unit_shadow
order by id, parent_org_unit_id nulls first;