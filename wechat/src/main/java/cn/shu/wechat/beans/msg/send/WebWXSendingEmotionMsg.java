package cn.shu.wechat.beans.msg.send;

import cn.shu.wechat.enums.WXSendMsgCodeEnum;

/**
 * @作者 舒新胜
 * @项目 AutoWeChat
 * @创建时间 3/10/2021 2:49 PM
 *
 * 图片消息
 */

public class WebWXSendingEmotionMsg extends WebWXSendingMsg {
    public Integer EmojiFlag = 2;
    public String EMoticonMd5;
    public WebWXSendingEmotionMsg() {
        super( WXSendMsgCodeEnum.EMOTION.getCode());
        super.Content = null;
    }
}