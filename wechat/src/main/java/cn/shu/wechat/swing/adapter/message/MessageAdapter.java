package cn.shu.wechat.swing.adapter.message;

import cn.shu.wechat.api.ContactsTools;
import cn.shu.wechat.api.DownloadTools;
import cn.shu.wechat.beans.pojo.Contacts;
import cn.shu.wechat.core.Core;
import cn.shu.wechat.swing.ImageViewer.ImageViewerFrame;
import cn.shu.wechat.swing.adapter.BaseAdapter;
import cn.shu.wechat.swing.adapter.ViewHolder;
import cn.shu.wechat.swing.components.RCListView;
import cn.shu.wechat.swing.components.RCProgressBar;
import cn.shu.wechat.swing.components.UserInfoPopup;
import cn.shu.wechat.swing.components.message.*;
import cn.shu.wechat.swing.db.model.Message;
import cn.shu.wechat.swing.entity.FileAttachmentItem;
import cn.shu.wechat.swing.entity.MessageItem;
import cn.shu.wechat.swing.entity.VideoAttachmentItem;
import cn.shu.wechat.swing.helper.AttachmentIconHelper;
import cn.shu.wechat.swing.helper.MessageViewHolderCacheHelper;
import cn.shu.wechat.swing.panels.ChatPanel;
import cn.shu.wechat.swing.panels.RoomChatPanel;
import cn.shu.wechat.swing.utils.*;
import cn.shu.wechat.utils.ExecutorServiceUtil;
import cn.shu.wechat.utils.SleepUtils;
import com.sun.media.jfxmedia.MediaPlayer;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.DateUtils;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by 舒新胜 on 17-6-2.
 */
public class MessageAdapter extends BaseAdapter<BaseMessageViewHolder> {
    private List<MessageItem> messageItems;
    private RCListView listView;
    private AttachmentIconHelper attachmentIconHelper = new AttachmentIconHelper();
    private ImageCache imageCache;

    private FileCache fileCache;
    private MessagePopupMenu popupMenu = new MessagePopupMenu();


    MessageViewHolderCacheHelper messageViewHolderCacheHelper;

    public MessageAdapter(List<MessageItem> messageItems, RCListView listView, MessageViewHolderCacheHelper messageViewHolderCacheHelper) {
        this.messageItems = messageItems;
        this.listView = listView;

        // currentUser = currentUserService.findAll().get(0);
        imageCache = new ImageCache();
        fileCache = new FileCache();
        this.messageViewHolderCacheHelper = messageViewHolderCacheHelper;
    }

    @Override
    public int getItemViewType(int position) {
        return messageItems.get(position).getMessageType();
    }

    @Override
    public boolean isGroup(int position) {
        return messageItems.get(position).getSenderId().startsWith("@@");
    }

    @Override
    public BaseMessageViewHolder onCreateViewHolder(int viewType, int position) {
        switch (viewType) {
            case MessageItem.SYSTEM_MESSAGE: {
                MessageSystemMessageViewHolder holder = messageViewHolderCacheHelper.tryGetSystemMessageViewHolder();
                if (holder == null) {
                    holder = new MessageSystemMessageViewHolder();
                }

                return holder;
            }
            case MessageItem.RIGHT_TEXT: {
                MessageRightTextViewHolder holder = messageViewHolderCacheHelper.tryGetRightTextViewHolder();
                if (holder == null) {
                    holder = new MessageRightTextViewHolder();
                }

                return holder;
            }
            case MessageItem.LEFT_TEXT: {
                MessageLeftTextViewHolder holder = messageViewHolderCacheHelper.tryGetLeftTextViewHolder();
                if (holder == null) {
                    holder = new MessageLeftTextViewHolder(messageItems.get(position).isGroupable());
                }

                return holder;
            }
            case MessageItem.RIGHT_IMAGE: {
                MessageRightImageViewHolder holder = messageViewHolderCacheHelper.tryGetRightImageViewHolder();
                if (holder == null) {
                    holder = new MessageRightImageViewHolder();
                }

                return holder;
            }
            case MessageItem.LEFT_IMAGE: {
                MessageLeftImageViewHolder holder = messageViewHolderCacheHelper.tryGetLeftImageViewHolder();
                if (holder == null) {
                    holder = new MessageLeftImageViewHolder(messageItems.get(position).isGroupable());
                }

                return holder;
            }
            case MessageItem.LEFT_VIDEO: {
                MessageLeftVideoViewHolder holder = messageViewHolderCacheHelper.tryGetLeftVideoViewHolder();
                if (holder == null) {
                    MessageItem messageItem = messageItems.get(position);
                    holder = new MessageLeftVideoViewHolder(messageItem.isGroupable(),
                            ImageUtil.getScaleDimen(messageItem.getVideoAttachmentItem().getSalveImgWidth()
                                    , messageItem.getVideoAttachmentItem().getSalveImgHeight()));
                }

                return holder;
            }
            case MessageItem.RIGHT_VIDEO: {
                MessageRightVideoViewHolder holder = messageViewHolderCacheHelper.tryGetRightVideoViewHolder();
                if (holder == null) {
                    MessageItem messageItem = messageItems.get(position);
                    holder = new MessageRightVideoViewHolder(messageItem.isGroupable(),
                            ImageUtil.getScaleDimen(messageItem.getVideoAttachmentItem().getSalveImgWidth()
                                    , messageItem.getVideoAttachmentItem().getSalveImgHeight()));
                }

                return holder;
            }
            case MessageItem.LEFT_VOICE: {
                MessageLeftVoiceViewHolder holder = messageViewHolderCacheHelper.tryGetLeftVoiceViewHolder();
                if (holder == null) {
                    MessageItem messageItem = messageItems.get(position);
                    holder = new MessageLeftVoiceViewHolder(messageItem.isGroupable());
                }

                return holder;
            }
            case MessageItem.RIGHT_VOICE: {
                MessageRightVoiceViewHolder holder = messageViewHolderCacheHelper.tryGetRightVoiceViewHolder();
                if (holder == null) {
                    MessageItem messageItem = messageItems.get(position);
                    holder = new MessageRightVoiceViewHolder(messageItem.isGroupable());
                }

                return holder;
            }
            case MessageItem.RIGHT_ATTACHMENT: {
                MessageRightAttachmentViewHolder holder = messageViewHolderCacheHelper.tryGetRightAttachmentViewHolder();
                if (holder == null) {
                    holder = new MessageRightAttachmentViewHolder();
                }

                return holder;
            }
            case MessageItem.LEFT_ATTACHMENT: {
                MessageLeftAttachmentViewHolder holder = messageViewHolderCacheHelper.tryGetLeftAttachmentViewHolder();
                if (holder == null) {
                    holder = new MessageLeftAttachmentViewHolder(messageItems.get(position).isGroupable());
                }

                return holder;
            }
        }

        return null;
    }

    @Override
    public void onBindViewHolder(BaseMessageViewHolder viewHolder, int position) {
        if (viewHolder == null) {
            return;
        }

        final MessageItem item = messageItems.get(position);
        MessageItem preItem = position == 0 ? null : messageItems.get(position - 1);

        processTimeAndAvatar(item, preItem, viewHolder);

        if (viewHolder instanceof MessageSystemMessageViewHolder) {
            processSystemMessage(viewHolder, item);
        } else if (viewHolder instanceof MessageRightTextViewHolder) {
            processRightTextMessage(viewHolder, item);
        } else if (viewHolder instanceof MessageLeftTextViewHolder) {
            processLeftTextMessage(viewHolder, item);
        } else if (viewHolder instanceof MessageRightImageViewHolder) {
            processRightImageMessage(viewHolder, item);
        } else if (viewHolder instanceof MessageLeftVideoViewHolder) {
            processLeftVideoMessage(viewHolder, item);
        } else if (viewHolder instanceof MessageRightVideoViewHolder) {
            processRightVideoMessage(viewHolder, item);
        } else if (viewHolder instanceof MessageLeftVoiceViewHolder) {
            processLeftVoiceMessage(viewHolder, item);
        }  else if (viewHolder instanceof MessageRightVoiceViewHolder) {
            processRightVoiceMessage(viewHolder, item);
        } else if (viewHolder instanceof MessageLeftImageViewHolder) {
            processLeftImageMessage(viewHolder, item);
        } else if (viewHolder instanceof MessageRightAttachmentViewHolder) {
            processRightAttachmentMessage(viewHolder, item);
        } else if (viewHolder instanceof MessageLeftAttachmentViewHolder) {
            processLeftAttachmentMessage(viewHolder, item);
        }
    }

    private void processSystemMessage(ViewHolder viewHolder, MessageItem item) {
        MessageSystemMessageViewHolder holder = (MessageSystemMessageViewHolder) viewHolder;
        holder.text.setText(item.getMessageContent());
    }

    private void processLeftAttachmentMessage(ViewHolder viewHolder, MessageItem item) {
        MessageLeftAttachmentViewHolder holder = (MessageLeftAttachmentViewHolder) viewHolder;
        holder.attachmentTitle.setText(item.getMessageContent());
        String filename = item.getFileAttachment().getTitle();
        Map map = new HashMap();
        map.put("attachmentId", item.getFileAttachment().getId());
        map.put("name", filename);
        map.put("messageId", item.getId());
        map.put("filepath", item.getFileAttachment().getLink());
        holder.attachmentPanel.setTag(map);
        //如果为视频，则显示视频缩略图
        String mime = MimeTypeUtil.getMime(filename.substring(filename.lastIndexOf(".") + 1));
        mime = attachmentIconHelper.parseMimeType(mime);
        if ("video".equals(mime) && StringUtils.isNotEmpty(item.getFileAttachment().getSlavePath())) {
            ImageIcon attachmentTypeIcon = null;
            String path = item.getFileAttachment().getSlavePath();
            try {
                BufferedImage read = ImageIO.read(new File(path == null ? item.getFileAttachment().getLink() : path));
                attachmentTypeIcon = new ImageIcon(read);
                attachmentTypeIcon = ImageUtil.preferredImageSize(attachmentTypeIcon);
                holder.attachmentIcon.setIcon(attachmentTypeIcon);
            } catch (IOException e) {
                attachmentTypeIcon = attachmentIconHelper.getImageIcon(filename);
                holder.attachmentIcon.setIcon(attachmentTypeIcon);
                e.printStackTrace();
            }

        } else {
            ImageIcon attachmentTypeIcon = attachmentIconHelper.getImageIcon(filename);
            holder.attachmentIcon.setIcon(attachmentTypeIcon);
        }

        holder.sender.setText(item.getSenderUsername());

        setAttachmentClickListener(holder, item);
        processAttachmentSize(holder, item);

        listView.setScrollHiddenOnMouseLeave(holder.attachmentPanel);
        listView.setScrollHiddenOnMouseLeave(holder.messageBubble);
        listView.setScrollHiddenOnMouseLeave(holder.attachmentTitle);

        // 绑定右键菜单
        attachPopupMenu(viewHolder, MessageItem.LEFT_ATTACHMENT);
    }

    private void processRightAttachmentMessage(ViewHolder viewHolder, MessageItem item) {
        MessageRightAttachmentViewHolder holder = (MessageRightAttachmentViewHolder) viewHolder;
        holder.attachmentTitle.setText(item.getMessageContent());

        Map map = new HashMap();
        map.put("attachmentId", item.getFileAttachment().getId());
        String filename = item.getFileAttachment().getTitle();
        map.put("name", filename);
        map.put("messageId", item.getId());
        map.put("filepath", item.getFileAttachment().getLink());
        holder.attachmentPanel.setTag(map);
        //如果为视频，则显示视频缩略图
        String mime = MimeTypeUtil.getMime(filename.substring(filename.lastIndexOf(".") + 1));
        mime = attachmentIconHelper.parseMimeType(mime);
        if ("video".equals(mime) && StringUtils.isNotEmpty(item.getFileAttachment().getSlavePath())) {
            ImageIcon attachmentTypeIcon = null;
            String path = item.getFileAttachment().getSlavePath();
            try {
                BufferedImage read = ImageIO.read(new File(path == null ? item.getFileAttachment().getLink() : path));
                attachmentTypeIcon = new ImageIcon(read);
                attachmentTypeIcon = ImageUtil.preferredImageSize(attachmentTypeIcon);
                holder.attachmentIcon.setIcon(attachmentTypeIcon);
            } catch (IOException e) {
                attachmentTypeIcon = attachmentIconHelper.getImageIcon(filename);
                holder.attachmentIcon.setIcon(attachmentTypeIcon);
                e.printStackTrace();
            }

        } else {
            ImageIcon attachmentTypeIcon = attachmentIconHelper.getImageIcon(filename);
            holder.attachmentIcon.setIcon(attachmentTypeIcon);
        }


        if (item.getProgress() != 0 && item.getProgress() != 100) {
            Message msg = null;//= messageService.findById(item.getId());
            if (msg != null) {
                item.setProgress(msg.getProgress());

                holder.progressBar.setVisible(true);
                holder.progressBar.setValue(item.getProgress());

                if (item.getProgress() == 100) {
                    holder.progressBar.setVisible(false);
                } else {
       /*             if (!ChatPanel.getContext().uploadingOrDownloadingFiles.contains(item.getFileAttachment().getId())) {
                        item.setNeedToResend(true);
                    }*/
                }
            }
        } else {
            holder.progressBar.setVisible(false);
        }


        // 判断是否显示重发按钮
        if (item.isNeedToResend()) {
            holder.sizeLabel.setVisible(false);
            holder.progressBar.setVisible(false);
            holder.resend.setVisible(true);
        } else {
            holder.resend.setVisible(false);
        }

        holder.resend.addMouseListener(new MessageMouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
               /* if (item.getUpdatedAt() > 0) {
                    holder.resend.setVisible(false);
                    System.out.println("这条消息其实已经发送出去了");
                    return;
                }*/

                //ChatPanel.getContext().resendFileMessage(item.getId(), "file");

                super.mouseClicked(e);
            }
        });

        setAttachmentClickListener(holder, item);

        if (item.getProgress() > 0) {
            processAttachmentSize(holder, item);
        } else {
            holder.sizeLabel.setText("等待上传...");
        }

        // 绑定右键菜单
        attachPopupMenu(viewHolder, MessageItem.RIGHT_ATTACHMENT);

        listView.setScrollHiddenOnMouseLeave(holder.attachmentPanel);
        listView.setScrollHiddenOnMouseLeave(holder.messageBubble);
        listView.setScrollHiddenOnMouseLeave(holder.attachmentTitle);
    }

    /**
     * 设置附件点击监听
     *
     * @param viewHolder
     * @param item
     */
    private void setAttachmentClickListener(MessageAttachmentViewHolder viewHolder, MessageItem item) {
        MessageMouseListener listener = new MessageMouseListener() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    ChatPanel.downloadOrOpenFile(item.getId());
                }
            }
        };


        viewHolder.attachmentPanel.addMouseListener(listener);
        viewHolder.attachmentTitle.addMouseListener(listener);
    }

    private void processAttachmentSize(MessageAttachmentViewHolder viewHolder, MessageItem item) {
        FileAttachmentItem attachment = item.getFileAttachment();
        String path;
        // 远程服务器文件
        if (attachment.getLink().startsWith("/file-upload")) {
            path = fileCache.tryGetFileCache(item.getFileAttachment().getId(), item.getFileAttachment().getTitle());
        }
        // 我自己上传的文件
        else {
            path = attachment.getLink();
        }

        if (path != null) {
            viewHolder.sizeLabel.setVisible(true);
            viewHolder.sizeLabel.setText(fileCache.fileSizeString(path));
        }
    }

    /**
     * 对方发送的图片
     *
     * @param viewHolder
     * @param item
     */
    private void processLeftImageMessage(ViewHolder viewHolder, MessageItem item) {
        MessageLeftImageViewHolder holder = (MessageLeftImageViewHolder) viewHolder;
        holder.sender.setText(item.getSenderUsername());

        processImage(item, holder.image, holder);

        listView.setScrollHiddenOnMouseLeave(holder.image);
        listView.setScrollHiddenOnMouseLeave(holder.imageBubble);

        // 绑定右键菜单
        attachPopupMenu(viewHolder, MessageItem.LEFT_IMAGE);
    }

    /**
     * 处理 对方 发送的语音消息
     *
     * @param viewHolder
     * @param item
     */
    private void processLeftVoiceMessage(ViewHolder viewHolder, MessageItem item) {
        MessageLeftVoiceViewHolder holder = (MessageLeftVoiceViewHolder) viewHolder;
        processVoice(item,holder,holder.getMessageBubble());
        holder.getSender().setText(item.getSenderUsername());
        listView.setScrollHiddenOnMouseLeave(holder.getMessageBubble());
        attachPopupMenu(viewHolder, MessageItem.LEFT_VOICE);

    }

    /**
     *
     *
     * @param viewHolder
     * @param item
     */
    private void processRightVoiceMessage(ViewHolder viewHolder, MessageItem item) {
        MessageRightVoiceViewHolder holder = (MessageRightVoiceViewHolder) viewHolder;
        processVoice(item,holder,holder.getMessageBubble());
        listView.setScrollHiddenOnMouseLeave(holder.getMessageBubble());
        attachPopupMenu(viewHolder, MessageItem.RIGHT_VOICE);

    }
    private void processVoice(MessageItem item, MessageVoiceViewHolder holder, RCAttachmentMessageBubble messageBubble){
        holder.getContentTagPanel().setTag(item.getVoiceAttachmentItem());
        double len = item.getVoiceAttachmentItem().getVoiceLength() * 1.0;
        len = len / 1000;
        long round = Math.round(len);
        StringBuilder t = new StringBuilder();
        for (long i = 0; i < round / 2; i++) {
            t.append(" ");
        }
        if (holder instanceof MessageLeftVoiceViewHolder){
            holder.getDurationText().setText(round +"''"+t.toString());
        }else{
            holder.getDurationText().setText( t.toString()+round +"''");
        }

        //holder.getDurationText().setTag(item.getId());


        //播放语音
        messageBubble.addMouseListener(new MessageMouseListener() {
            private void closePlayer() {
                if (player != null) {
                    player.close();
                    player = null;
                }
            }
            private Player player = null;
            @Override
            public void mouseReleased(MouseEvent e) {

                if (e.getButton() == MouseEvent.BUTTON1) {
                    closePlayer();
                    String voicePath = item.getVoiceAttachmentItem().getVoicePath();
                    File file = new File(voicePath);
                    if (!file.exists()) {
                        Boolean aBoolean = DownloadTools.FILE_DOWNLOAD_STATUS.get(voicePath);
                        if (aBoolean == null) {
                            JOptionPane.showMessageDialog(null, "下载失败", "打开失败", JOptionPane.ERROR_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(null, "下载中...", "打开失败", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        holder.removeUnreadPoint();
                        RCProgressBar progressBar = holder.getProgressBar();
                        progressBar.setVisible(true);
                        progressBar.setMaximum((int) item.getVoiceAttachmentItem().getVoiceLength());
                        new SwingWorker<Object, Integer>() {
                            @Override
                            protected Object doInBackground() throws Exception {
                                player = new Player(new BufferedInputStream(new FileInputStream(file)));
                                //新线程更新进度条
                                ExecutorServiceUtil.getGlobalExecutorService().submit(new Runnable() {
                                    @Override
                                    public void run() {
                                        while (player != null) {
                                            if (player.isComplete()) {
                                                publish((int) item.getVoiceAttachmentItem().getVoiceLength());
                                                break;
                                            } else {
                                                publish(player.getPosition());
                                            }
                                        }
                                    }
                                });
                                player.play();
                                return null;
                            }

                            @Override
                            protected void process(List<Integer> chunks) {
                                Integer integer = chunks.get(chunks.size() - 1);
                                progressBar.setValue(integer);
                                super.process(chunks);
                            }

                            @Override
                            protected void done() {
                                closePlayer();
                                progressBar.setValue(0);
                                progressBar.setVisible(false);
                            }
                        }.execute();


                    }

                }
                super.mouseReleased(e);
            }
        });
    }
    /**
     * 对方发送的图片
     *
     * @param viewHolder
     * @param item
     */
    private void processLeftVideoMessage(ViewHolder viewHolder, MessageItem item) {
        MessageLeftVideoViewHolder holder = (MessageLeftVideoViewHolder) viewHolder;
        holder.getSender().setText(item.getSenderUsername());

        try {
            processVideo(item
                    , holder.getTimeLabel()
                    ,holder.getPlayImgLabel()
                    ,holder.getSlaveImgLabel()
                    ,holder.getVideoComponent());
        } catch (IOException e) {
            e.printStackTrace();
        }
        holder.getVideoComponent().setTag(item.getVideoAttachmentItem());
        listView.setScrollHiddenOnMouseLeave(holder.getVideoComponent());
        listView.setScrollHiddenOnMouseLeave(holder.getImageBubble());

        // 绑定右键菜单
        attachPopupMenu(viewHolder, MessageItem.LEFT_VIDEO);
    }

    /**
     * 对方发送的图片
     *
     * @param viewHolder
     * @param item
     */
    private void processRightVideoMessage(ViewHolder viewHolder, MessageItem item) {
        MessageRightVideoViewHolder holder = (MessageRightVideoViewHolder) viewHolder;
        holder.getSender().setText(item.getSenderUsername());

        try {
            processVideo(item
                    , holder.getTimeLabel()
                    ,holder.getPlayImgLabel()
                    ,holder.getSlaveImgLabel()
                    ,holder.getVideoComponent());
        } catch (IOException e) {
            e.printStackTrace();
        }
        holder.getVideoComponent().setTag(item.getVideoAttachmentItem());
        listView.setScrollHiddenOnMouseLeave(holder.getVideoComponent());
        listView.setScrollHiddenOnMouseLeave(holder.getImageBubble());

        // 绑定右键菜单
        attachPopupMenu(viewHolder, MessageItem.RIGHT_VIDEO);
    }

    /**
     * 我发送的图片
     *
     * @param viewHolder
     * @param item
     */
    private void processRightImageMessage(ViewHolder viewHolder, MessageItem item) {
        MessageRightImageViewHolder holder = (MessageRightImageViewHolder) viewHolder;

        processImage(item, holder.image, holder);
        if (item.getProgress() != 100) {
            // Message msg = messageService.findById(item.getId());
            //if (msg != null) {
            // item.setProgress(msg.getProgress());

            /*        if (!ChatPanel.uploadingOrDownloadingFiles.contains(item.getImageAttachment().getId())) {
                        item.setNeedToResend(true);
                    }*/
            //}
            holder.sendingProgress.setVisible(true);
        } else {
            holder.sendingProgress.setVisible(false);
        }


        // 判断是否显示重发按钮
        if (item.isNeedToResend()) {
            holder.resend.setVisible(true);
        } else {
            holder.resend.setVisible(false);
        }

        holder.resend.addMouseListener(new MessageMouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
             /*   if (item.getUpdatedAt() > 0) {
                    holder.resend.setVisible(false);
                    System.out.println("这条消息其实已经发送出去了");
                    return;
                }*/

                //ChatPanel.getContext().resendFileMessage(item.getId(), "image");

                super.mouseClicked(e);
            }
        });

        // 绑定右键菜单
        attachPopupMenu(viewHolder, MessageItem.RIGHT_IMAGE);

        listView.setScrollHiddenOnMouseLeave(holder.image);
        listView.setScrollHiddenOnMouseLeave(holder.imageBubble);
    }

    /**
     * 返回时间格式化后的表示
     *
     * @param lengthSec 秒
     */
    private static String getSecString(long lengthSec) {
        long hour, minute;
        hour = lengthSec / 3600;
        minute = (lengthSec - hour * 3600) / 60;
        lengthSec = lengthSec - hour * 300 - minute * 60;

        return (hour < 10 && hour > 0 ? "0" + hour : hour) + ":"
                + (minute < 10 && minute > 0 ? "0" + minute : minute) + ":"
                + (lengthSec < 10 && lengthSec > 0 ? "0" + lengthSec : lengthSec);
    }

    /**
     * 处理视频
     *
     * @param item   消息项
     * @throws IOException 视频缩略图读取异常
     */
    private void processVideo(MessageItem item, JLabel timeLabel,JLabel playImgLabel,JLabel slaveImgLabel,JComponent videoComponent) throws IOException {
        VideoAttachmentItem videoItem = item.getVideoAttachmentItem();
        //#############判断缩略图是否下载完成#########################
        String slaveImgPath = videoItem.getSlaveImgPath();
        Boolean aBoolean = DownloadTools.FILE_DOWNLOAD_STATUS.get(slaveImgPath);
        timeLabel.setText(getSecString(videoItem.getVideoLength()));
        if (aBoolean == null) {
            //缩略图下载失败
            //  holder.getSlaveImgLabel().setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/image/image_error.png"))));
            playImgLabel.setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/image/play48.png"))));
        } else if (aBoolean) {
            File file = new File(slaveImgPath);
            slaveImgLabel.setIcon(ImageUtil.preferredImageSize(new ImageIcon(ImageIO.read(file))));
            playImgLabel.setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/image/play48.png"))));
        } else {
            // holder.getPlayImgLabel().setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/image/image_loading.gif"))));
            // holder.getPlayImgLabel().repaint();

            new SwingWorker<Object, Object>() {
                private final long startTime = System.currentTimeMillis();
                private boolean status = false;

                @Override
                protected Object doInBackground() throws Exception {
                    //等待下载完成
                    while (!status) {
                        SleepUtils.sleep(100);
                        status = DownloadTools.FILE_DOWNLOAD_STATUS.get(slaveImgPath);
                        //三分钟还未下载完成
                        if (System.currentTimeMillis() - startTime >= 1000 * 60 * 3) {
                            //下载超时
                            break;
                        }
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        if (status) {
                            File file = new File(slaveImgPath);
                            slaveImgLabel.setIcon(ImageUtil.preferredImageSize(new ImageIcon(ImageIO.read(file))));
                        } else {
                            //缩略图下载失败
                            // holder.getSlaveImgLabel().setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/image/image_error.png"))));
                        }
                        playImgLabel.setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/image/play48.png"))));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.execute();
        }

        // 当点击视频时，使用默认程序打开图片
        videoComponent.addMouseListener(new MessageMouseListener() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    ChatPanel.openFile(videoItem.getVideoPath());
                }
                super.mouseReleased(e);
            }
        });
    }

    private void processImage(MessageItem item, MessageImageLabel imageLabel, ViewHolder holder) {
       /* String url;
        if (imageUrl.startsWith("/file-upload"))
        {
            //url = Launcher.HOSTNAME + imageUrl + ".jpg?rc_uid=" + currentUser.getUserId() + "&rc_token=" + currentUser.getAuthToken();
            url = Launcher.HOSTNAME + imageUrl + "?rc_uid=" + currentUser.getUserId() + "&rc_token=" + currentUser.getAuthToken();
        }
        else
        {
            url = "file://" + imageUrl;
        }*/

        loadImageThumb(holder, item, imageLabel);

        // 当点击图片时，使用默认程序打开图片
        imageLabel.addMouseListener(new MessageMouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (item.getImageAttachment().isVideo()) {
                        ChatPanel.openFile(item.getImageAttachment().getImagePath());
                        return;
                    }

                    //Desktop.getDesktop().open(new File(path));
                    // final ImageViewerFrame frame = new ImageViewerFrame(this.getClass().getClassLoader().getResource("/image/image_loading.png").getPath());
                    // frame.setVisible(true);
       /*             new SwingWorker<Object,Object>(){

                        @Override
                        protected Object doInBackground() throws Exception {
                            //循环等待下载完成
                            Boolean aBoolean = DownloadTools.FILE_DOWNLOAD_STATUS.get( item.getImageAttachment().getImagePath());
                            while(aBoolean!=null &&  !aBoolean){
                                aBoolean = DownloadTools.FILE_DOWNLOAD_STATUS.get( item.getImageAttachment().getImagePath());
                            }
                            if ( DownloadTools.FILE_DOWNLOAD_STATUS.contains( item.getImageAttachment().getImagePath())){
                                DownloadTools.FILE_DOWNLOAD_STATUS.remove( item.getImageAttachment().getImagePath());
                            }
                            return null;
                        }

                        @Override
                        protected void done() {
                            //frame.dispose();
                            ImageViewerFrame frame = new ImageViewerFrame(item.getImageAttachment().getImagePath());
                            frame.setVisible(true);
                        }
                    }.execute();*/
                    imageCache.requestOriginalAsynchronously(item.getImageAttachment().getId(), item.getImageAttachment().getImagePath(), new ImageCache.ImageCacheRequestListener() {
                        @Override
                        public void onSuccess(ImageIcon icon, String path) {
                            try {
                                //Desktop.getDesktop().open(new File(path));
                                File file = new File(path);
                                if (file.length() > 1024 * 1024) {
                                    ChatPanel.openFile(path);
                                    //Desktop.getDesktop().open(new File(path));
                                    return;
                                }
                                ImageViewerFrame frame = new ImageViewerFrame(path);
                                frame.setVisible(true);

                                // 如果图片获取成功，则重新加载缩略图
                                ImageIcon thumbIcon = (ImageIcon) imageLabel.getIcon();
                                if (thumbIcon.getDescription().endsWith("image_error.png")) {
                                    loadImageThumb(holder, item, imageLabel);
                                }
                            } catch (NullPointerException e1) {
                                // JOptionPane.showMessageDialog(null, "图像不存在", "图像不存在", JOptionPane.ERROR_MESSAGE);
                                e1.printStackTrace();
                            } catch (Exception e1) {
                                JOptionPane.showMessageDialog(null, "图像不存在", "图像不存在", JOptionPane.ERROR_MESSAGE);
                                e1.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailed(String why) {
                            // 图片不存在，显示错误图片
                            //ImageViewerFrame frame = new ImageViewerFrame(getClass().getResource("/image/image_error.png").getPath());
                            // frame.setVisible(true);
                        }
                    });

                }
                super.mouseClicked(e);
            }
        });
    }

    private void loadImageThumb(ViewHolder holder, MessageItem item, MessageImageLabel imageLabel) {
        //可能文件还没下载完成

        //显示加载中
        ImageIcon imageIcon = new ImageIcon(getClass().getResource("/image/image_loading.gif"));
        // preferredImageSize(imageIcon);
        imageLabel.setIcon(imageIcon);
        String slavePath = item.getImageAttachment().getSlavePath();
        if (StringUtils.isEmpty(slavePath)) {
            slavePath = item.getImageAttachment().getImagePath();
        }
        final String finalPath = slavePath;
        //标志
        Map<String, Object> map = new HashMap<>();
        map.put("attachmentId", item.getImageAttachment().getId());
        map.put("url", finalPath);
        map.put("messageId", item.getId());
        imageLabel.setTag(map);

        // ImageIcon imageIcon = imageCache.tryGetThumbCache(item.getImageAttachment().getId());
        new SwingWorker<Object, Object>() {

            @Override
            protected Object doInBackground() throws Exception {
                //循环等待下载完成
                Boolean aBoolean = DownloadTools.FILE_DOWNLOAD_STATUS.get(finalPath);
                while (aBoolean != null && !aBoolean) {
                    Thread.sleep(100);
                    aBoolean = DownloadTools.FILE_DOWNLOAD_STATUS.get(finalPath);
                }
                if (DownloadTools.FILE_DOWNLOAD_STATUS.contains(finalPath)) {
                    DownloadTools.FILE_DOWNLOAD_STATUS.remove(finalPath);
                }
                return null;
            }

            @Override
            protected void done() {
                if (finalPath == null) {
                    return;
                }
                ImageIcon imageIcon = imageCache.tryGetThumbCache(new File(finalPath));
                if (imageIcon == null) {
                     /*   imageLabel.setIcon(IconUtil.getIcon(this, "/image/image_loading.gif"));

                        imageCache.requestThumbAsynchronously(item.getImageAttachment().getId(), finalPath, new ImageCache.ImageCacheRequestListener() {
                            @Override
                            public void onSuccess(ImageIcon icon, String path) {
                                preferredImageSize(icon);
                                imageLabel.setIcon(icon);
                                holder.revalidate();
                                holder.repaint();
                            }

                            @Override
                            public void onFailed(String why) {
                                imageLabel.setIcon(IconUtil.getIcon(this, "/image/image_error.png", 64, 64));
                                holder.revalidate();
                                holder.repaint();
                            }
                        });*/
                } else {
                    // item.getMessageType()
                    ImageUtil.preferredImageSize(imageIcon);
                    imageLabel.setIcon(imageIcon);
                }
            }
        }.execute();


    }


    /**
     * 处理 我发送的文本消息
     *
     * @param viewHolder
     * @param item
     */
    private void processRightTextMessage(ViewHolder viewHolder, final MessageItem item) {
        MessageRightTextViewHolder holder = (MessageRightTextViewHolder) viewHolder;

        holder.text.setText(item.getMessageContent());

        holder.text.setTag(item.getId());

        //holder.text.setCaretPosition(holder.text.getDocument().getLength());
        //holder.text.insertIcon(IconUtil.getIcon(this, "/image/smile.png", 18,18));

        //processMessageContent(holder.messageText, item);
        //registerMessageTextListener(holder.messageText, item);

        // 判断是否显示重发按钮
        boolean needToUpdateResendStatus = !item.isNeedToResend() && System.currentTimeMillis() - item.getTimestamp() > 10 * 1000;

        if (item.isNeedToResend()) {
            if (needToUpdateResendStatus) {
                //messageService.updateNeedToResend(item.getId(), true);
            }


            holder.sendingProgress.setVisible(false);
            holder.resend.setVisible(true);
        } else {
            holder.resend.setVisible(false);
            // 如果是刚发送的消息，显示正在发送进度条
            if (item.getProgress() != 100) {
                holder.sendingProgress.setVisible(true);
            } else {
                holder.sendingProgress.setVisible(false);
            }
        }


        holder.resend.addMouseListener(new MessageMouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            /*    if (item.getUpdatedAt() > 0) {
                    holder.resend.setVisible(false);
                    return;
                }*/

                System.out.println("重发消消息：" + item.getMessageContent());

                // TODO: 向服务器重新发送消息
                Message message = null;//= messageService.findById(item.getId());
                message.setUpdatedAt(System.currentTimeMillis());
                message.setNeedToResend(false);
                //messageService.update(message);

                super.mouseClicked(e);
            }
        });

        // 绑定右键菜单
        attachPopupMenu(viewHolder, MessageItem.RIGHT_TEXT);

        listView.setScrollHiddenOnMouseLeave(holder.messageBubble);
        listView.setScrollHiddenOnMouseLeave(holder.text);
    }

    /**
     * 处理 对方 发送的文本消息
     *
     * @param viewHolder
     * @param item
     */
    private void processLeftTextMessage(ViewHolder viewHolder, final MessageItem item) {
        MessageLeftTextViewHolder holder = (MessageLeftTextViewHolder) viewHolder;

        holder.text.setText(item.getMessageContent() == null ? "[空消息]" : item.getMessageContent());
        holder.text.setTag(item.getId());

        holder.sender.setText(item.getSenderUsername());

        listView.setScrollHiddenOnMouseLeave(holder.messageBubble);
        listView.setScrollHiddenOnMouseLeave(holder.text);
        attachPopupMenu(viewHolder, MessageItem.LEFT_TEXT);
    }

    /**
     * 处理消息发送时间 以及 消息发送者头像
     *
     * @param item
     * @param preItem
     * @param holder
     */
    private void processTimeAndAvatar(MessageItem item, MessageItem preItem, BaseMessageViewHolder holder) {
        // 如果当前消息的时间与上条消息时间相差大于1分钟，则显示当前消息的时间
        if (preItem != null) {
            if (TimeUtil.inTheSameMinute(item.getTimestamp(), preItem.getTimestamp())) {
                holder.time.setVisible(false);
            } else {
                holder.time.setVisible(true);
                holder.time.setText(TimeUtil.diff(item.getTimestamp(), true));
            }
        } else {
            holder.time.setVisible(true);
            holder.time.setText(TimeUtil.diff(item.getTimestamp(), true));
        }

        if (holder.avatar != null) {
            ImageIcon icon = new ImageIcon();
            Image image = AvatarUtil.createOrLoadUserAvatar(item.getSenderId()).getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            icon.setImage(image);
            holder.avatar.setIcon(icon);

            //弹窗
            if (item.getMessageType() == MessageItem.LEFT_ATTACHMENT
                    || item.getMessageType() == MessageItem.LEFT_IMAGE
                    || item.getMessageType() == MessageItem.LEFT_TEXT) {

                bindAvatarAction(holder.avatar, item);
            }
        }


        /*
        {
            holder.avatar.setImageBitmap(AvatarUtil.createOrLoadUserAvatar(this.activity, item.getSenderUsername()));
        }*/
    }


    private void bindAvatarAction(JLabel avatarLabel, MessageItem item) {

        avatarLabel.addMouseListener(new MessageMouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Contacts contacts = null;
                if (item.isGroupable()) {
                    contacts = ContactsTools.getMemberOfGroup(item.getRoomId(), item.getSenderId());
                } else {
                    contacts = Core.getMemberMap().get(item.getSenderId());
                }
                if (contacts == null) {
                    RoomChatPanel.getContext().get(RoomChatPanel.getContext().getCurrRoomId())
                            .getTipPanel().setText("成员信息加载中...");
                    return;
                }
                contacts.setGroupName(item.getRoomId());
                UserInfoPopup popup = new UserInfoPopup(contacts);
                popup.show(e.getComponent(), e.getX(), e.getY());

                super.mouseClicked(e);
            }
        });
    }

    @Override
    public int getCount() {
        return messageItems.size();
    }

    private void attachPopupMenu(ViewHolder viewHolder, int messageType) {
        JComponent contentComponent = null;
        RCMessageBubble messageBubble = null;

        switch (messageType) {
            case MessageItem.RIGHT_TEXT: {
                MessageRightTextViewHolder holder = (MessageRightTextViewHolder) viewHolder;
                contentComponent = holder.text;
                messageBubble = holder.messageBubble;

                break;
            }
            case MessageItem.LEFT_TEXT: {
                MessageLeftTextViewHolder holder = (MessageLeftTextViewHolder) viewHolder;
                contentComponent = holder.text;
                messageBubble = holder.messageBubble;
                break;
            }
            case MessageItem.RIGHT_IMAGE: {
                MessageRightImageViewHolder holder = (MessageRightImageViewHolder) viewHolder;
                contentComponent = holder.image;
                messageBubble = holder.imageBubble;
                break;
            }
            case MessageItem.LEFT_IMAGE: {
                MessageLeftImageViewHolder holder = (MessageLeftImageViewHolder) viewHolder;
                contentComponent = holder.image;
                messageBubble = holder.imageBubble;
                break;
            }
            case MessageItem.LEFT_VIDEO: {
                MessageLeftVideoViewHolder holder = (MessageLeftVideoViewHolder) viewHolder;
                contentComponent = holder.getVideoComponent();
                messageBubble = holder.getImageBubble();
                break;
            }
            case MessageItem.RIGHT_VIDEO: {
                MessageRightVideoViewHolder holder = (MessageRightVideoViewHolder) viewHolder;
                contentComponent = holder.getVideoComponent();
                messageBubble = holder.getImageBubble();
                break;
            }
            case MessageItem.LEFT_VOICE: {
                MessageLeftVoiceViewHolder holder = (MessageLeftVoiceViewHolder) viewHolder;
                contentComponent = holder.getContentTagPanel();
                messageBubble = holder.getMessageBubble();
                break;
            }
            case MessageItem.RIGHT_VOICE: {
                MessageRightVoiceViewHolder holder = (MessageRightVoiceViewHolder) viewHolder;
                contentComponent = holder.getContentTagPanel();
                messageBubble = holder.getMessageBubble();
                break;
            }
            case MessageItem.RIGHT_ATTACHMENT: {
                MessageRightAttachmentViewHolder holder = (MessageRightAttachmentViewHolder) viewHolder;
                contentComponent = holder.attachmentPanel;
                messageBubble = holder.messageBubble;

                holder.attachmentTitle.addMouseListener(new MessageMouseListener() {
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON3) {
                            // 通过holder.attachmentPane.getTag()可以获取文件附件信息
                            popupMenu.show(holder.attachmentPanel, e.getX(), e.getY(), MessageItem.RIGHT_ATTACHMENT);
                        }
                    }
                });
                break;
            }
            case MessageItem.LEFT_ATTACHMENT: {
                MessageLeftAttachmentViewHolder holder = (MessageLeftAttachmentViewHolder) viewHolder;
                contentComponent = holder.attachmentPanel;
                messageBubble = holder.messageBubble;

                holder.attachmentTitle.addMouseListener(new MessageMouseListener() {
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON3) {
                            popupMenu.show(holder.attachmentPanel, e.getX(), e.getY(), MessageItem.LEFT_ATTACHMENT);
                        }
                    }
                });
                break;
            }
        }

        JComponent finalContentComponent = contentComponent;
        RCMessageBubble finalMessageBubble = messageBubble;

        contentComponent.addMouseListener(new MessageMouseListener() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (e.getX() > finalContentComponent.getWidth() || e.getY() > finalContentComponent.getHeight()) {
                    finalMessageBubble.setBackgroundIcon(finalMessageBubble.getBackgroundNormalIcon());
                }
                super.mouseExited(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                finalMessageBubble.setBackgroundIcon(finalMessageBubble.getBackgroundActiveIcon());
                super.mouseEntered(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    popupMenu.show((Component) e.getSource(), e.getX(), e.getY(), messageType);
                }

                super.mouseReleased(e);
            }
        });

        messageBubble.addMouseListener(new MessageMouseListener() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    popupMenu.show(finalContentComponent, e.getX(), e.getY(), messageType);
                }
            }
        });
    }


}
