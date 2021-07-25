package cn.shu.wechat.swing.components.message;

import cn.shu.wechat.api.MessageTools;
import cn.shu.wechat.api.WeChatTool;
import cn.shu.wechat.beans.msg.send.WebWXSendMsgResponse;
import cn.shu.wechat.beans.pojo.Message;
import cn.shu.wechat.mapper.MessageMapper;
import cn.shu.wechat.swing.components.Colors;
import cn.shu.wechat.swing.components.RCMenuItemUI;
import cn.shu.wechat.swing.components.SizeAutoAdjustTextArea;
import cn.shu.wechat.swing.entity.MessageItem;
import cn.shu.wechat.swing.frames.MainFrame;
import cn.shu.wechat.swing.panels.ChatPanel;
import cn.shu.wechat.swing.utils.ClipboardUtil;
import cn.shu.wechat.swing.utils.FileCache;
import cn.shu.wechat.swing.utils.ImageCache;
import cn.shu.wechat.utils.SpringContextHolder;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Map;

/**
 * Created by 舒新胜 on 2017/6/5.
 */
public class MessagePopupMenu extends JPopupMenu {
    private int messageType;
    private ImageCache imageCache = new ImageCache();
    private FileCache fileCache = new FileCache();

    public MessagePopupMenu() {
        initMenuItem();
    }

    private void initMenuItem() {
        JMenuItem item1 = new JMenuItem("复制");
        JMenuItem item2 = new JMenuItem("删除");
        JMenuItem item3 = new JMenuItem("转发");
        JMenuItem item4 = new JMenuItem("撤回");
        item1.setUI(new RCMenuItemUI());
        item1.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switch (messageType) {
                    case MessageItem.RIGHT_TEXT:
                    case MessageItem.LEFT_TEXT: {
                        SizeAutoAdjustTextArea textArea = (SizeAutoAdjustTextArea) getInvoker();
                        String text = textArea.getSelectedText() == null ? textArea.getText() : textArea.getSelectedText();
                        if (text != null) {
                            ClipboardUtil.copyString(text);
                        }
                        break;
                    }
                    case (MessageItem.RIGHT_IMAGE):
                    case (MessageItem.LEFT_IMAGE): {
                        MessageImageLabel imageLabel = (MessageImageLabel) getInvoker();
                        Object obj = imageLabel.getTag();
                        if (obj != null) {
                            Map map = (Map) obj;
                            String id = (String) map.get("attachmentId");
                            String url = (String) map.get("url");
                            imageCache.requestOriginalAsynchronously(id, url, new ImageCache.ImageCacheRequestListener() {
                                @Override
                                public void onSuccess(ImageIcon icon, String path) {
                                    if (path != null && !path.isEmpty()) {
                                        ClipboardUtil.copyImage(path);
                                    } else {
                                        System.out.println("图片不存在，复制失败");
                                    }
                                }

                                @Override
                                public void onFailed(String why) {
                                    System.out.println("图片不存在，复制失败");
                                }
                            });
                        }
                        break;
                    }

                    case (MessageItem.RIGHT_ATTACHMENT):
                    case (MessageItem.LEFT_ATTACHMENT): {
                        AttachmentPanel attachmentPanel = (AttachmentPanel) getInvoker();
                        Object obj = attachmentPanel.getTag();
                        if (obj != null) {
                            Map map = (Map) obj;
                            String id = (String) map.get("attachmentId");
                            String name = (String) map.get("name");

                            String path = fileCache.tryGetFileCache(id, name);
                            if (path != null && !path.isEmpty()) {
                                ClipboardUtil.copyFile(path);
                            } else {
                                Object filepath = map.get("filepath");

                                if (filepath == null) {
                                    JOptionPane.showMessageDialog(MainFrame.getContext(), "文件不存在", "文件不存在", JOptionPane.WARNING_MESSAGE);
                                    return;
                                }

                                String link = filepath.toString();
                                if (link.startsWith("/file-upload")) {
                                    JOptionPane.showMessageDialog(MainFrame.getContext(), "请先下载文件", "请先下载文件", JOptionPane.WARNING_MESSAGE);
                                    return;
                                } else {
                                    File file = new File(link);
                                    if (!file.exists()) {
                                        JOptionPane.showMessageDialog(MainFrame.getContext(), "文件不存在，可能已被删除", "文件不存在", JOptionPane.WARNING_MESSAGE);
                                        return;
                                    }
                                    ClipboardUtil.copyFile(link);
                                }
                            }
                        }
                        break;

                    }
                    default:
                        break;
                }

            }
        });


        item2.setUI(new RCMenuItemUI());
        item2.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String messageId = null;
                switch (messageType) {
                    case MessageItem.RIGHT_TEXT:
                    case MessageItem.LEFT_TEXT: {
                        SizeAutoAdjustTextArea textArea = (SizeAutoAdjustTextArea) getInvoker();
                        messageId = textArea.getTag().toString();
                        break;
                    }
                    case (MessageItem.RIGHT_IMAGE):
                    case (MessageItem.LEFT_IMAGE): {
                        MessageImageLabel imageLabel = (MessageImageLabel) getInvoker();
                        Object obj = imageLabel.getTag();
                        if (obj != null) {
                            Map map = (Map) obj;
                            messageId = (String) map.get("messageId");
                        }
                        break;
                    }
                    case (MessageItem.RIGHT_ATTACHMENT):
                    case (MessageItem.LEFT_ATTACHMENT): {
                        AttachmentPanel attachmentPanel = (AttachmentPanel) getInvoker();
                        Object obj = attachmentPanel.getTag();
                        if (obj != null) {
                            Map map = (Map) obj;
                            messageId = (String) map.get("messageId");
                        }
                        break;
                    }
                    default:
                }

                if (messageId != null && !messageId.isEmpty()) {
                    //ChatPanel.getContext().deleteMessage(messageId);
                }
            }
        });

        item3.setUI(new RCMenuItemUI());
        item3.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("转发");
            }
        });
        item4.setUI(new RCMenuItemUI());
        item4.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String messageId = null;
                switch (messageType) {
                    case MessageItem.RIGHT_TEXT:
                    case MessageItem.LEFT_TEXT: {
                        SizeAutoAdjustTextArea textArea = (SizeAutoAdjustTextArea) getInvoker();
                        messageId = textArea.getTag().toString();
                        break;
                    }
                    case (MessageItem.RIGHT_IMAGE):
                    case (MessageItem.LEFT_IMAGE): {
                        MessageImageLabel imageLabel = (MessageImageLabel) getInvoker();
                        Object obj = imageLabel.getTag();
                        if (obj != null) {
                            Map map = (Map) obj;
                            messageId = (String) map.get("messageId");
                        }
                        break;
                    }
                    case (MessageItem.RIGHT_ATTACHMENT):
                    case (MessageItem.LEFT_ATTACHMENT): {
                        AttachmentPanel attachmentPanel = (AttachmentPanel) getInvoker();
                        Object obj = attachmentPanel.getTag();
                        if (obj != null) {
                            Map map = (Map) obj;
                            messageId = (String) map.get("messageId");
                        }
                        break;
                    }
                    default:
                }

                if (!StringUtils.isEmpty(messageId)) {
                    MessageMapper bean = SpringContextHolder.getBean(MessageMapper.class);
                    Message message = bean.selectByPrimaryKey(messageId);
                    String response = message.getResponse();
                    WebWXSendMsgResponse webWXSendMsgResponse = JSON.parseObject(response, WebWXSendMsgResponse.class);
                    if (webWXSendMsgResponse.getBaseResponse().getRet() == 0){
                        boolean b = MessageTools.sendRevokeMsgByUserId(message.getToUsername(), webWXSendMsgResponse.getLocalID(), webWXSendMsgResponse.getMsgID());
                    }
                }
            }
        });
        this.add(item1);
        this.add(item2);
        this.add(item4);
       // this.add(item3);

        setBorder(new LineBorder(Colors.SCROLL_BAR_TRACK_LIGHT));
        setBackground(Colors.FONT_WHITE);
    }

    @Override
    public void show(Component invoker, int x, int y) {
        throw new RuntimeException("此方法不会弹出菜单，请调用 show(Component invoker, int x, int y, int messageType) ");
        //super.show(invoker, x, y);
    }

    public void show(Component invoker, int x, int y, int messageType) {
        this.messageType = messageType;
        super.show(invoker, x, y);
    }
}
