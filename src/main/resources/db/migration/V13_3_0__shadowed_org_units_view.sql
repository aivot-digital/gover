CREATE OR REPLACE VIEW organizational_units_shadowed AS
WITH RECURSIVE org_unit_shadow AS (SELECT id,
                                          name,
                                          address,
                                          imprint,
                                          privacy,
                                          accessibility,
                                          technical_support_address,
                                          special_support_address,
                                          created,
                                          updated,
                                          department_mail,
                                          theme_id,
                                          contact_legal,
                                          contact_technical,
                                          additional_info,
                                          depth,
                                          parent_org_unit_id
                                   FROM organizational_units

                                   UNION ALL

                                   SELECT child.id,
                                          child.name,
                                          COALESCE(child.address, parent.address),
                                          COALESCE(child.imprint, parent.imprint),
                                          COALESCE(child.privacy, parent.privacy),
                                          COALESCE(child.accessibility, parent.accessibility),
                                          COALESCE(child.technical_support_address, parent.technical_support_address),
                                          COALESCE(child.special_support_address, parent.special_support_address),
                                          child.created,
                                          child.updated,
                                          COALESCE(child.department_mail, parent.department_mail),
                                          COALESCE(child.theme_id, parent.theme_id),
                                          COALESCE(child.contact_legal, parent.contact_legal),
                                          COALESCE(child.contact_technical, parent.contact_technical),
                                          COALESCE(child.additional_info, parent.additional_info),
                                          child.depth,
                                          child.parent_org_unit_id
                                   FROM org_unit_shadow parent
                                            JOIN organizational_units child ON child.parent_org_unit_id = parent.id
                                   WHERE child.address IS NULL
                                      OR child.imprint IS NULL
                                      OR child.privacy IS NULL
                                      OR child.accessibility IS NULL
                                      OR child.technical_support_address IS NULL
                                      OR child.special_support_address IS NULL
                                      OR child.contact_legal IS NULL
                                      OR child.contact_technical IS NULL
                                      OR child.additional_info IS NULL
                                      OR child.department_mail IS NULL)
SELECT DISTINCT ON (id) id,
                        name,
                        address,
                        imprint,
                        privacy,
                        accessibility,
                        technical_support_address,
                        special_support_address,
                        created,
                        updated,
                        department_mail,
                        theme_id,
                        contact_legal,
                        contact_technical,
                        additional_info,
                        depth,
                        parent_org_unit_id
FROM org_unit_shadow
ORDER BY id, parent_org_unit_id NULLS FIRST;