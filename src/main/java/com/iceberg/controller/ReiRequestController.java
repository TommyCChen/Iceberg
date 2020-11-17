package com.iceberg.controller;

import static com.iceberg.entity.ReimbursementRequest.TYPE.APPROVED;
import static com.iceberg.entity.ReimbursementRequest.TYPE.PROCESSING;

import com.iceberg.emailservice.EmailSendService;
import com.iceberg.entity.ReimbursementRequest;
import com.iceberg.entity.UserInfo;
import com.iceberg.externalapi.PayPalService;
import com.iceberg.service.ReiRequestService;
import com.iceberg.service.UserInfoService;
import com.iceberg.utils.Config;
import com.iceberg.utils.PageModel;
import com.iceberg.utils.Result;
import com.iceberg.utils.ResultUtil;
import com.iceberg.utils.Utils;
import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/reirequest")
public class ReiRequestController {

  private Logger logger = LoggerFactory.getLogger(ReiRequestController.class);
  @Resource
  private ReiRequestService reiRequestService;

  @Resource
  private UserInfoService userInfoService;

  @Resource
  private PayPalService payPalService;

//  @Resource
//  private EmailSendService emailSendService;

  @RequestMapping(value = "/addRequest", method = RequestMethod.POST)
  public Result add(ReimbursementRequest reimbursementRequest, HttpSession session) {
    if (Config.getSessionUser(session) != null) {
      reimbursementRequest.setUserid(Config.getSessionUser(session).getId());
    }
    Utils.log(reimbursementRequest.toString());
    try {
      reimbursementRequest.setTypeid(PROCESSING.value);
      logger.debug(reimbursementRequest.toString());
      int num = reiRequestService.add(reimbursementRequest);
      if (num > 0) {
        return ResultUtil.success("Reimbursement Request Successfully!",
          reimbursementRequest);
      } else {
        return ResultUtil.unSuccess();
      }
    } catch (Exception e) {
      return ResultUtil.error(e);
    }
  }

  /**
   * the owen of the reimbursement request can update the reimbursement
   *
   * @param reimbursementRequest reimbursement request
   * @param session http session
   * @return result information
   */
  @RequestMapping(value = "/updateRequest", method = RequestMethod.POST)
  public Result update(ReimbursementRequest reimbursementRequest, HttpSession session) {
    if (Config.getSessionUser(session) == null) {
      return ResultUtil.unSuccess("No user for current session");
    }
    Utils.log(reimbursementRequest.toString());
    int userId = Config.getSessionUser(session).getId();
    int roleId = Config.getSessionUser(session).getRoleid();
    ReimbursementRequest reimbursementRequestSearch = new ReimbursementRequest();
    reimbursementRequestSearch.setId(reimbursementRequest.getId());
    Result<ReimbursementRequest> result = reiRequestService
      .findByWhereNoPage(reimbursementRequestSearch);
    if (result.getDatas().size() == 0) {
      return ResultUtil.unSuccess("Such reimbursement request doesn't exist!");
    } else if (result.getDatas().size() > 2) {
      return ResultUtil.unSuccess("reimbursement request duplicate!");
    } else {
      if (roleId == 3 && result.getDatas().get(0).getUserid() != userId) {
        return ResultUtil.unSuccess("Permission denied!");
      } else if (result.getDatas().get(0).getTypeid() == APPROVED) {
        return ResultUtil.unSuccess("Request has already been approved!");
      }
      try {
        reimbursementRequest.setTypeid(PROCESSING.value);
        int num = reiRequestService.update(reimbursementRequest);
        if (num > 0) {
          return ResultUtil.success("Update successfully!", null);
        } else {
          return ResultUtil.unSuccess();
        }
      } catch (Exception e) {
        return ResultUtil.error(e);
      }
    }
  }

  /**
   * review reimbursement request only for admin and group manager
   *
   * @param reimbursementRequest reimbursement request
   * @param session http session
   * @return result information whether the request has been approved.
   */
  @RequestMapping(value = "/review", method = RequestMethod.POST)
  public Result review(ReimbursementRequest reimbursementRequest, HttpSession session) throws IOException {
    if (Config.getSessionUser(session) == null) {
      return ResultUtil.unSuccess("No user for current session");
    }
    if (Config.getSessionUser(session).getRoleid() > 2) {
      return ResultUtil.unSuccess("Permission denied. Don't have review access.");
    }
    // TODO: error handling and check details.
    if (reimbursementRequest.getTypeid() == APPROVED) {
      // TODO: approve such request

      // paypal send service
      Result<ReimbursementRequest> result=reiRequestService.findByWhereNoPage(reimbursementRequest);
      ReimbursementRequest completeReimbursementRequest=result.getDatas().get(0);
      String receiver=completeReimbursementRequest.getReceiveraccount();
      String money=completeReimbursementRequest.getMoney().toString();
      payPalService.createPayout(receiver,"USD",money);

//      // email send service
//      int id=reimbursementRequest.getUserid();
//      UserInfo userInfoWithOnlyID=new UserInfo();
//      userInfoWithOnlyID.setId(id);
//      UserInfo completeUserInfo=userInfoService.getUserInfo(userInfoWithOnlyID);
//      emailSendService.approveConfirm(completeUserInfo,completeReimbursementRequest);


    } else if (reimbursementRequest.getTypeid() == PROCESSING) {
      return ResultUtil.unSuccess("Not a valid review");
    }
    try {
      int num = reiRequestService.update(reimbursementRequest);
      if (num > 0) {
        return ResultUtil.success(String.format("Update successfully! Status : %s",
          reimbursementRequest.getTypeid()), null);
      } else {
        return ResultUtil.unSuccess();
      }
    } catch (Exception e) {
      return ResultUtil.error(e);
    }
  }

  @RequestMapping("/getReiRequestById/{id}")
  public Result getReiRequestById(@PathVariable Integer id) {
    ReimbursementRequest reimbursementRequestCondition = new ReimbursementRequest();
    reimbursementRequestCondition.setId(id);

    try {
      return reiRequestService.findByWhereNoPage(reimbursementRequestCondition);
    } catch (Exception e) {
      return ResultUtil.error(e);
    }
  }

  @RequestMapping("/getReiRequestByUserId/{userid}/{pageNo}/{pageSize}")
  public Result getReiRequestByUserId(@PathVariable int userid, @PathVariable int pageNo,
    @PathVariable int pageSize) {
    ReimbursementRequest reimbursementRequest = new ReimbursementRequest();
    reimbursementRequest.setId(userid);

    PageModel model = new PageModel<>(pageNo, reimbursementRequest);
    model.setPageSize(pageSize);

    return reiRequestService.findByWhere(model);
  }

  @RequestMapping("/getReiRequest/{pageNo}/{pageSize}")
  public Result getReiRequest(ReimbursementRequest reimbursementRequest, @PathVariable int pageNo,
    @PathVariable int pageSize, HttpSession session) {
    if (Config.getSessionUser(session) == null) {
      return ResultUtil.unSuccess("No user for current session");
    }
    UserInfo currentUser = Config.getSessionUser(session);
    ReimbursementRequest reimbursementRequestSearch = new ReimbursementRequest();
    if (reimbursementRequest.getTitle() != null) {
      reimbursementRequestSearch.setTitle(reimbursementRequest.getTitle());
    }
    if (reimbursementRequest.getRemark() != null) {
      reimbursementRequestSearch.setRemark(reimbursementRequest.getRemark());
    }
    if (reimbursementRequest.getTypeid() != null) {
      reimbursementRequestSearch.setTypeid(reimbursementRequest.getTypeid().value);
    }

    // search for group reimbursement information if group manager
    if (currentUser.getRoleid() == 2) {
      reimbursementRequestSearch.setGroupid(currentUser.getGroupid());
    } else if (currentUser.getRoleid() == 3) {
      reimbursementRequestSearch.setUserid(currentUser.getId());
    }
    PageModel model = new PageModel<>(pageNo, reimbursementRequestSearch);
    model.setPageSize(pageSize);

    return reiRequestService.findByWhere(model);
  }

  @RequestMapping("/getReiRequestByNoPage")
  public Result getReiRequestByNoPage(HttpSession session) {
    ReimbursementRequest reimbursementRequestSearch = new ReimbursementRequest();
    if (Config.getSessionUser(session) == null) {
      return ResultUtil.unSuccess("No user for current session");
    }
    UserInfo currentUser = Config.getSessionUser(session);
    // search for group reimbursement information if group manager
    if (currentUser.getRoleid() == 2) {
      reimbursementRequestSearch.setGroupid(currentUser.getGroupid());
    } else if (currentUser.getRoleid() == 3) {
      reimbursementRequestSearch.setUserid(currentUser.getId());
    }
    return reiRequestService.findByWhereNoPage(reimbursementRequestSearch);
  }

  @RequestMapping("/delReiRequest")
  public Result del(int id) {
    try {
      int num = reiRequestService.del(id);
      if (num > 0) {
        return ResultUtil.success("delete successfully!", null);
      } else {
        return ResultUtil.unSuccess();
      }
    } catch (Exception e) {
      return ResultUtil.error(e);
    }
  }

}
