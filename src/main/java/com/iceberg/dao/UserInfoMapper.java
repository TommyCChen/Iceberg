package com.iceberg.dao;

import com.iceberg.entity.Group;
import com.iceberg.entity.Role;
import com.iceberg.entity.UserInfo;
import com.iceberg.utils.PageModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("UserInfoMapper")
public interface UserInfoMapper {


  UserInfo getUserInfo(UserInfo userInfo);


  int addUser(UserInfo userInfo);

  /**
   * 通过username判断该用户是否存在
   * 
   * @param userInfo
   * @return
   */
  int userIsExisted(UserInfo userInfo);

  /**
   * 通过条件获取符合条件的优化信息 -- 分页
   * 
   * @param model
   * @return
   */
  List<UserInfo> getUsersByWhere(PageModel<UserInfo> model);

  int getToatlByWhere(PageModel<UserInfo> model);

  int add(UserInfo userInfo);

  int update(UserInfo userInfo);

  int delete(String id);

  List<Role> getAllRoles();

  int addRole(Role role);

  int updateRole(Role role);

  int deleteRole(String id);

  Role getRoleById(String id);

  int addGroupId(Group group);
}
