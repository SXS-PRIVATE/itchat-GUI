package cn.shu.wechat.api;


import cn.shu.wechat.beans.pojo.Contacts;
import cn.shu.wechat.core.Core;
import cn.shu.wechat.service.LoginService;
import cn.shu.wechat.utils.CommonTools;
import cn.shu.wechat.utils.SpringContextHolder;
import com.alibaba.fastjson.JSONArray;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 微信联系人工具，如获好友昵称、备注等
 *
 * @author SXS
 * @version 1.1
 * @date 创建时间：2017年5月4日 下午10:49:16
 */
@Log4j2
public class ContactsTools {


    /**
     * 根据用户名获取用户信息
     *
     * @param userName 用户UserName
     * @return 用户信息
     */
    private static Contacts getContactByUserName(String userName) {
        Map<String, Contacts> contactMap = Core.getMemberMap();
        return contactMap.getOrDefault(userName, null);

    }

    /**
     * 根据用户名获取用户显示名称
     * 有备注显示备注，无备注显示昵称
     * 群则直接显示昵称
     *
     * @param userName 用户UserName
     * @return 备注
     */
    public static String getContactDisplayNameByUserName(String userName) {
        String remarkNameByPersonUserName = getContactRemarkNameByUserName(userName);
        if (StringUtils.isNotEmpty(remarkNameByPersonUserName)) {
            return remarkNameByPersonUserName;
        }
        String nickNameByPersonUserName = getContactNickNameByUserName(userName);
        if (StringUtils.isNotEmpty(nickNameByPersonUserName )) {
            return nickNameByPersonUserName;
        }
        String groupDefaultName = getGroupDefaultName(userName);
        if (StringUtils.isNotEmpty(groupDefaultName )){
            return groupDefaultName;
        }
        return userName;
    }

    /**
     * @param userName 用户UserName
     * @return 默认名称，如果是群，则以群成员的名称开始
     */
    public static String getGroupDefaultName(String userName){

        if (userName.startsWith("@@") ){
            Contacts contactByUserName = getContactByUserName(userName);
            if (contactByUserName == null){
                return null;
            }
            StringBuilder name = new StringBuilder();
            List<Contacts> memberlist = contactByUserName.getMemberlist();
            if (memberlist !=null&& !memberlist.isEmpty()){
                for (int i = 0; i < Math.min(2, memberlist.size()); i++) {
                    Contacts contacts = memberlist.get(i);
                    name.append(contacts.getNickname()).append(",");
                }
                String string = name.toString();
                if (string.isEmpty()){
                    return string;
                }
                return string.substring(0,string.length()-1);
            }
        }
     return null;
    }
    /**
     * 根据用户名获取用户备注
     *
     * @param userName 用户UserName
     * @return 备注
     */
    public static String getContactRemarkNameByUserName(String userName) {
        if (userName == null){
            return "";
        }
        //群只有备注 没有昵称
        if (userName.startsWith("@@")) {
            return getContactNickNameByUserName(userName);
        }
        Contacts contactByUserName = getContactByUserName(userName);
        if (contactByUserName == null) {
            return null;
        }
        return CommonTools.emojiFormatter(contactByUserName.getRemarkname());
    }


    /**
     * 根据用户名获取普通用户昵称
     *
     * @param userName 用户UserName
     * @return 备注
     */
    public static String getContactNickNameByUserName(String userName) {
        if (userName == null){
            return null;
        }
        Contacts contactByUserName = getContactByUserName(userName);
        if (contactByUserName == null) {
            return null;
        }
        return CommonTools.emojiFormatter(contactByUserName.getNickname());
    }


    /**
     * 获取群成员
     *
     * @param groupName 群UserName
     * @param userName  成员UserName
     * @return 成员
     */
    public static Contacts getMemberOfGroup(String groupName, String userName) {
        if (userName == null){
            throw new RuntimeException("userName can not be null");
        }
        if (groupName == null){
            throw new RuntimeException("groupName can not be null");
        }
        //群成员为自己的好友
        if (Core.getMemberMap().containsKey(userName)){
            return Core.getMemberMap().get(userName);
        }
        Map<String, Contacts> groupMemberMap =Core.getMemberMap();
        Contacts group = groupMemberMap.getOrDefault(groupName, null);
        if (group == null) {
            return null;
        }
        List<Contacts> memberList = group.getMemberlist();
        if (memberList == null || memberList.size() <= 0) {
            LoginService bean = SpringContextHolder.getBean(LoginService.class);
            memberList = bean.WebWxBatchGetContact(groupName);
           if (memberList == null || memberList.size() <= 0){
               return null;
           }
        }
        try {

            for (Contacts contacts : memberList) {
                if (userName.equals(contacts.getUsername())) {
                    return contacts;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.warn(e.getMessage());
        }

        return null;
    }

    /**
     * 获取群成员昵称
     *
     * @param groupName 群UserName
     * @param userName  成员UserName
     * @return 成员昵称
     */
    public static String getMemberNickNameOfGroup(String groupName, String userName) {
        Contacts memberOfGroup = getMemberOfGroup(groupName, userName);
        return memberOfGroup != null
                ? CommonTools.emojiFormatter(memberOfGroup.getNickname())
                : null;


    }

    /**
     * 获取群成员显示名称
     *
     * @param groupName 群UserName
     * @param userName  成员UserName
     * @return 群成员显示名称
     */
    public static String getMemberDisplayNameOfGroup(String groupName, String userName) {
        Contacts memberOfGroup = getMemberOfGroup(groupName, userName);
        return getMemberDisplayNameOfGroup(memberOfGroup,userName);
    }

    /**
     * 获取群成员显示名称
     *
     * @param memberOfGroup 群
     * @param userName  成员UserName
     * @return 群成员显示名称
     */
    public static String getMemberDisplayNameOfGroup(Contacts memberOfGroup, String userName) {
        if (memberOfGroup == null || userName == null) {
            throw new NullPointerException("memberOfGroup or userName is null");
        }
            String displayName = memberOfGroup.getDisplayname();
            if (!StringUtils.isEmpty(displayName)) {
                return CommonTools.emojiFormatter(displayName);
            }
            displayName = memberOfGroup.getNickname();
            if (!StringUtils.isEmpty(displayName)) {
                return CommonTools.emojiFormatter(displayName);
            }
        return userName;
    }

}
