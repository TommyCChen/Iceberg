package com.iceberg.service.impl;

import com.iceberg.dao.PrivilegeMapper;
import com.iceberg.entity.Privilege;
import com.iceberg.service.PrivilegeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class PrivilegeServiceImpl implements PrivilegeService {

  @Resource
  private PrivilegeMapper mapper;

  @Override
  public List<Privilege> getPrivilegeByRoleid(int roleid) {
    return this.mapper.getPrivilegeByRoleid(roleid);
  }

  @Override
  public int addDefaultPrivilegesWhenAddRole(String roleid) {
    if(StringUtils.isNumeric(roleid)) {
      return mapper.addDefaultPrivilegesWhenAddRole(roleid);
    } else {
      return 0;
    }
  }

  @Override
  public int delPrivilegesWenDelRole(String roleid) {
    if(StringUtils.isNumeric(roleid)) {
      return mapper.delPrivilegesWenDelRole(roleid);
    } else {
      return 0;
    }
  }
}
