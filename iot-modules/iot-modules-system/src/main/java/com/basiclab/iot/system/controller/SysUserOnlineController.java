package com.basiclab.iot.system.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.basiclab.iot.common.core.constant.CacheConstants;
import com.basiclab.iot.common.core.utils.StringUtils;
import com.basiclab.iot.common.core.web.controller.BaseController;
import com.basiclab.iot.common.core.web.domain.AjaxResult;
import com.basiclab.iot.common.core.web.page.TableDataInfo;
import com.basiclab.iot.common.log.annotation.Log;
import com.basiclab.iot.common.log.enums.BusinessType;
import com.basiclab.iot.common.redis.service.RedisService;
import com.basiclab.iot.common.security.annotation.PreAuthorize;
import com.basiclab.iot.system.api.model.LoginUser;
import com.basiclab.iot.system.domain.SysUserOnline;
import com.basiclab.iot.system.service.ISysUserOnlineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在线用户监控
 * 
 * @author BasicLab基础架构实验室
 */
@RestController
@RequestMapping("/online")
public class SysUserOnlineController extends BaseController
{
    @Autowired
    private ISysUserOnlineService userOnlineService;

    @Autowired
    private RedisService redisService;

    @PreAuthorize(hasPermi = "monitor:online:list")
    @GetMapping("/list")
    public TableDataInfo list(String ipaddr, String userName)
    {
        Collection<String> keys = redisService.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
        List<SysUserOnline> userOnlineList = new ArrayList<SysUserOnline>();
        for (String key : keys)
        {
            LoginUser user = redisService.getCacheObject(key);
            if (StringUtils.isNotEmpty(ipaddr) && StringUtils.isNotEmpty(userName))
            {
                if (StringUtils.equals(ipaddr, user.getIpaddr()) && StringUtils.equals(userName, user.getUsername()))
                {
                    userOnlineList.add(userOnlineService.selectOnlineByInfo(ipaddr, userName, user));
                }
            }
            else if (StringUtils.isNotEmpty(ipaddr))
            {
                if (StringUtils.equals(ipaddr, user.getIpaddr()))
                {
                    userOnlineList.add(userOnlineService.selectOnlineByIpaddr(ipaddr, user));
                }
            }
            else if (StringUtils.isNotEmpty(userName))
            {
                if (StringUtils.equals(userName, user.getUsername()))
                {
                    userOnlineList.add(userOnlineService.selectOnlineByUserName(userName, user));
                }
            }
            else
            {
                userOnlineList.add(userOnlineService.loginUserToUserOnline(user));
            }
        }
        Collections.reverse(userOnlineList);
        userOnlineList.removeAll(Collections.singleton(null));
        return getDataTable(userOnlineList);
    }

    /**
     * 强退用户
     */
    @PreAuthorize(hasPermi = "monitor:online:forceLogout")
    @Log(title = "在线用户", businessType = BusinessType.FORCE)
    @DeleteMapping("/{tokenId}")
    public AjaxResult forceLogout(@PathVariable String tokenId)
    {
        redisService.deleteObject(CacheConstants.LOGIN_TOKEN_KEY + tokenId);
        return AjaxResult.success();
    }
}
