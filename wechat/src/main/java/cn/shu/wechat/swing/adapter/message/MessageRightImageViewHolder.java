package cn.shu.wechat.swing.adapter.message;

import cn.shu.wechat.swing.components.Colors;
import cn.shu.wechat.swing.components.GBC;
import cn.shu.wechat.swing.components.message.MessageImageLabel;
import cn.shu.wechat.swing.components.message.MessagePopupMenu;
import cn.shu.wechat.swing.components.message.RCRightImageMessageBubble;
import cn.shu.wechat.swing.utils.FontUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Created by 舒新胜 on 17-6-3.
 */
public class MessageRightImageViewHolder extends BaseMessageViewHolder {
    public MessageImageLabel image = new MessageImageLabel();
    //public JLabel avatar = new JLabel();
    //public JLabel size = new JLabel();
    public JLabel resend = new JLabel(); // 重发按钮
    public JLabel sendingProgress = new JLabel(); // 正在发送

    public RCRightImageMessageBubble imageBubble = new RCRightImageMessageBubble();
    private JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.CENTER,5,0));
    private JPanel messageAvatarPanel = new JPanel();
    private MessagePopupMenu popupMenu = new MessagePopupMenu();

    public MessageRightImageViewHolder() {
        initComponents();
        initView();
    }

    private void initComponents() {
        timePanel.setBackground(Colors.WINDOW_BACKGROUND);
        messageAvatarPanel.setBackground(Colors.WINDOW_BACKGROUND);

        //imageBubble.add(image);

        time.setForeground(Colors.FONT_GRAY);
        time.setFont(FontUtil.getDefaultFont(12));

        ImageIcon resendIcon = new ImageIcon(getClass().getResource("/image/resend.png"));
        resendIcon.setImage(resendIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
        resend.setIcon(resendIcon);
        resend.setVisible(false);
        resend.setToolTipText("图片发送失败，点击重新发送");
        resend.setCursor(new Cursor(Cursor.HAND_CURSOR));

        ImageIcon sendingIcon = new ImageIcon(getClass().getResource("/image/sending.gif"));
        sendingProgress.setIcon(sendingIcon);
        sendingProgress.setVisible(false);
    }

    private void initView() {
        setLayout(new BorderLayout());
        timePanel.add(time);

        JPanel resendImagePanel = new JPanel(new BorderLayout());
        resendImagePanel.setBackground(Colors.WINDOW_BACKGROUND);
        resendImagePanel.add(resend, BorderLayout.WEST);
        resendImagePanel.add(sendingProgress, BorderLayout.WEST);
        resendImagePanel.add(image, BorderLayout.CENTER);

        messageAvatarPanel.setLayout(new GridBagLayout());
        messageAvatarPanel.add(resendImagePanel, new GBC(1, 0).setWeight(1000, 1).setAnchor(GBC.EAST).setInsets(0, 0, 0, 5));
        messageAvatarPanel.add(avatar, new GBC(2, 0).setWeight(1, 1).setAnchor(GBC.NORTH).setInsets(0, 0, 0, 5));

        add(timePanel, BorderLayout.NORTH);
        add(messageAvatarPanel, BorderLayout.CENTER);
    }
}
