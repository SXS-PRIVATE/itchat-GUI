package cn.shu.wechat.swing.panels;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;

/**
 * 切换聊天房
 * @作者 舒新胜
 * @项目 AutoWechat
 * @创建时间 7/18/2021 11:40
 */
public class RoomChatPanel extends ParentAvailablePanel {
    private CardLayout cardLayout;

    private final LinkedHashMap<String, RoomChatPanelCard> cards = new LinkedHashMap<>(5);
    public String  getCurrRoomId() {
        return currRoomId;
    }

    private String currRoomId;
    public static RoomChatPanel getContext() {
        return context;
    }

    private static RoomChatPanel context;
    public RoomChatPanel(JPanel parent) {
        super(parent);
        context = this;
        init();
        createAndShow("filehelper");

    }
    private void init(){
        cardLayout = new CardLayout();
        this.setLayout(cardLayout);
    }

    /**
     * 添加层
     * @param roomId 房间id
     */
    public RoomChatPanelCard addPanel(String roomId){
        if (cards.containsKey(roomId)){
            return cards.get(roomId);
        }
        RoomChatPanelCard rightPanel = new RoomChatPanelCard(roomId);
        add(rightPanel,roomId);
        cards.put(roomId,rightPanel);
 /*       if (cards.size()>=5){
            cards.
        }*/
        return rightPanel;
    }


    /**
     * 显示对应层
     * @param roomId 房间id
     */
    public void show(String roomId){
        currRoomId = roomId;
        cardLayout.show(this,roomId);
    }

    /**
     * 创建显示
     * @param roomId 房间id
     */
    public RoomChatPanelCard createAndShow(String roomId){
        if (!cards.containsKey(roomId)){
            addPanel(roomId);
        }
        show(roomId);
        return get(roomId);
    }

    /**
     * 获取对应层
     * @param roomId roomId
     */
    public RoomChatPanelCard get(String roomId){
        return cards.get(roomId);
    }

    /**
     * 获取对应层
     *
     */
    public RoomChatPanelCard showCurr(){
        return cards.get(currRoomId);
    }

}