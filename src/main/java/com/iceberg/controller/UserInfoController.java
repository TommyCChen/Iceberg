package com.iceberg.controller;

import com.iceberg.entity.Privilege;
import com.iceberg.entity.UserInfo;
import com.iceberg.service.PrivilegeService;
import com.iceberg.service.UserInfoService;
import com.iceberg.utils.Config;
import com.iceberg.utils.Result;
import com.iceberg.utils.ResultUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * description: this class is mainly focus on user's account information
 */



@Controller
public class UserInfoController {
    @Resource
    private UserInfoService userInfoService;
    @Resource
    private PrivilegeService privilegeService;

    @RequestMapping(value = {"/", "login.html"})
    public String toLogin(HttpServletRequest request, HttpServletResponse response){
        HttpSession session = request.getSession();
        if(session.getAttribute(Config.CURRENT_USERNAME)==null){
            return "login";
        }else {
            try {
                response.sendRedirect("/pages/index");
            } catch (IOException e) {
                e.printStackTrace();
                return "login";
            }
            return null;
        }

    }

    @RequestMapping(value = "/login.do")
    @ResponseBody
    public Result getUserInfo(UserInfo userInfo, HttpServletRequest request, HttpServletResponse response){
        boolean userIsExisted = userInfoService.userIsExisted(userInfo);
        System.out.println(userIsExisted + " - " + request.getHeader("token"));
        userInfo = getUserInfo(userInfo);
        if("client".equals(request.getHeader("token")) && !userIsExisted){
            //user doesn't exist
            return  ResultUtil.success(-1);
        }
        if (userIsExisted && userInfo == null){
            return  ResultUtil.unSuccess("wrong username or password！");
        }else {
            // save user info in session
            userInfo = setSessionUserInfo(userInfo,request.getSession());
            //save user info in cookie
            setCookieUser(request,response);
            return ResultUtil.success("login successful", userInfo);
        }
    }

    @RequestMapping("/getSessionUser")
    @ResponseBody
    public UserInfo getSessionUser(HttpSession session){
        UserInfo sessionUser = (UserInfo) session.getAttribute(Config.CURRENT_USERNAME);
        sessionUser.setPassword(null);
        return sessionUser;
    }

    @RequestMapping("/pages/{page}")
    public String toPage(@PathVariable String page) {
        return page.replace("_", "/");
    }


    /**
     * get user privilege info through user info and save it in session
     * @param userInfo
     * @param session
     * @return
     */
    public UserInfo setSessionUserInfo(UserInfo userInfo, HttpSession session){
        List<Privilege> privileges = privilegeService.getPrivilegeByRoleid(userInfo.getRoleid());
        userInfo.setPrivileges(privileges);
        session.setAttribute(Config.CURRENT_USERNAME,userInfo);
        return userInfo;

    }
    /**
     * save user info in Cookie
     * @param response
     */
    private void setCookieUser(HttpServletRequest request, HttpServletResponse response){
        UserInfo user = getSessionUser(request.getSession());
        Cookie cookie = new Cookie(Config.CURRENT_USERNAME,user.getUsername()+"_"+user.getId());
        //cookie valid time interval
        cookie.setMaxAge(60*60*24*7);
        response.addCookie(cookie);
    }

    private UserInfo getUserInfo(UserInfo userInfo) {
        return userInfoService.getUserInfo(userInfo);
    }
}