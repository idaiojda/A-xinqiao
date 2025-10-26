package com.example.xinqiao.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.adapter.MsgAdapter;
import com.example.xinqiao.utils.DeepSeekClient;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.example.xinqiao.mysql.DBUtils;
import com.example.xinqiao.utils.AnalysisUtils;

import java.util.ArrayList;
import java.util.List;

public class AiView extends FrameLayout {

    private View mView;
    private EditText inputText;
    private ImageButton send;
    private Button btnNewChat; // 新对话按钮
    private ImageButton btnHistory; // 历史对话按钮
    private RecyclerView msgRecyclerView;
    private List<Msg> msgList = new ArrayList<>();
    private MsgAdapter adapter;
    private DeepSeekClient deepSeekClient;
    private Handler mainHandler;
    private Bitmap userAvatarBitmap; // 用户头像

    public AiView(Context context) {
        super(context);
        mainHandler = new Handler(Looper.getMainLooper());
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        mView = inflater.inflate(R.layout.main_view_ai, this, true);

        // 初始化视图组件
        inputText = mView.findViewById(R.id.input_text);
        send = mView.findViewById(R.id.send);
        msgRecyclerView = mView.findViewById(R.id.msg_recycler_view);
        btnNewChat = mView.findViewById(R.id.btn_new_chat); // 初始化新对话按钮
        btnHistory = mView.findViewById(R.id.btn_history); // 初始化历史对话按钮

        // 初始化RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        msgRecyclerView.setLayoutManager(layoutManager);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        adapter = new MsgAdapter(msgList, userAvatarBitmap);
        msgRecyclerView.setAdapter(adapter);

        // 初始化DeepSeek客户端
        deepSeekClient = new DeepSeekClient();

        // 初始化消息数据
        initMsgs();

        // 加载用户头像
        loadUserAvatar();

        // 设置新对话按钮点击事件
        btnNewChat.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearChat();
            }
        });

        // 设置历史对话按钮点击事件
        btnHistory.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistoryDialog();
            }
        });

        // 设置发送按钮点击事件
        send.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = inputText.getText().toString();
                if (!"".equals(content)) {
                    // 添加用户消息
                    Msg msg = new Msg(content, Msg.TYPE_SENT);
                    msgList.add(msg);
                    adapter.notifyItemInserted(msgList.size() - 1);
                    msgRecyclerView.scrollToPosition(msgList.size() - 1);
                    inputText.setText("");

                    // 显示加载消息
                    Msg loadingMsg = new Msg("正在思考...", Msg.TYPE_RECEIVED);
                    msgList.add(loadingMsg);
                    adapter.notifyItemInserted(msgList.size() - 1);
                    msgRecyclerView.scrollToPosition(msgList.size() - 1);

                    // 发送消息到DeepSeek
                    deepSeekClient.sendMessage(content, new DeepSeekClient.ChatCallback() {
                        @Override
                        public void onSuccess(String response) {
                            receiveDeepSeekResponse(response);
                        }

                        @Override
                        public void onFailure(String error) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    // 移除加载消息
                                    msgList.remove(msgList.size() - 1);
                                    adapter.notifyItemRemoved(msgList.size());

                                    // 显示错误消息
                                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                    
                    // 保存当前对话到历史记录
                    saveCurrentChat();
                } else {
                    Toast.makeText(getContext(), "请输入内容", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initMsgs() {
        Msg msg1 = new Msg("你好，欢迎来到心理咨询室。", Msg.TYPE_RECEIVED);
        msgList.add(msg1);
        Msg msg2 = new Msg("我是你的AI心理健康Advisor，我会以专业、温暖和理解的态度倾听你的心声。", Msg.TYPE_RECEIVED);
        msgList.add(msg2);
        Msg msg3 = new Msg("无论你遇到什么困扰，都可以和我分享。我们一起探讨，寻找解决方案。", Msg.TYPE_RECEIVED);
        msgList.add(msg3);
    }

    public View getView() {
        return mView;
    }

    public void showView() {
        // 确保消息列表不为空
        if (msgList.isEmpty()) {
            initMsgs();
        }
        // 确保视图可见
        if (mView != null) {
            mView.setVisibility(View.VISIBLE);
        }
        // 刷新用户头像，确保与个人中心同步
        loadUserAvatar();
        // 刷新消息列表
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        // 滚动到最新消息
        if (!msgList.isEmpty() && msgRecyclerView != null) {
            msgRecyclerView.scrollToPosition(msgList.size() - 1);
        }
    }

    public EditText getInputText() {
        return inputText;
    }

    public ImageButton getSendButton() {
        return send;
    }

    public RecyclerView getMsgRecyclerView() {
        return msgRecyclerView;
    }

    /**
     * 加载用户头像
     */
    private void loadUserAvatar() {
        if (!readLoginStatus()) {
            // 未登录时使用默认头像
            userAvatarBitmap = null;
            if (adapter != null) {
                adapter.setUserAvatarBitmap(null);
            }
            return;
        }

        String userName = AnalysisUtils.readLoginUserName(getContext());
        DBUtils dbUtils = null;
        try {
            dbUtils = DBUtils.getInstance(getContext());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (dbUtils != null) {
            dbUtils.getUserAvatarPath(userName, new DBUtils.AvatarPathCallback() {
                @Override
                public void onSuccess(String avatarBase64) {
                    if (avatarBase64 != null && avatarBase64.startsWith("data:image")) {
                        try {
                            String base64 = avatarBase64.substring(avatarBase64.indexOf(",") + 1);
                            // 使用BitmapFactory.Options进行内存优化
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            // 先只解码图片尺寸
                            options.inJustDecodeBounds = true;
                            byte[] avatarData = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                            BitmapFactory.decodeByteArray(avatarData, 0, avatarData.length, options);

                            // 计算合适的采样率
                            int targetWidth = 200;  // 目标宽度
                            int targetHeight = 200; // 目标高度
                            int scaleFactor = Math.min(options.outWidth / targetWidth, 
                                                     options.outHeight / targetHeight);
                            
                            // 实际解码图片
                            options.inJustDecodeBounds = false;
                            options.inSampleSize = scaleFactor;
                            options.inPreferredConfig = Bitmap.Config.RGB_565; // 使用16位图片格式
                            userAvatarBitmap = BitmapFactory.decodeByteArray(avatarData, 0, avatarData.length, options);

                            // 如果图片仍然太大，进行缩放
                            if (userAvatarBitmap != null && (userAvatarBitmap.getWidth() > targetWidth || 
                                userAvatarBitmap.getHeight() > targetHeight)) {
                                Bitmap scaledBitmap = Bitmap.createScaledBitmap(userAvatarBitmap, 
                                                                              targetWidth, 
                                                                              targetHeight, 
                                                                              true);
                                // 释放原始bitmap
                                if (userAvatarBitmap != scaledBitmap) {
                                    userAvatarBitmap.recycle();
                                }
                                userAvatarBitmap = scaledBitmap;
                            }

                            mainHandler.post(() -> {
                                if (adapter != null) {
                                    adapter.setUserAvatarBitmap(userAvatarBitmap);
                                }
                            });
                        } catch (OutOfMemoryError e) {
                            e.printStackTrace();
                            // 发生OOM时，释放bitmap并使用默认头像
                            if (userAvatarBitmap != null) {
                                userAvatarBitmap.recycle();
                                userAvatarBitmap = null;
                            }
                            mainHandler.post(() -> {
                                if (adapter != null) {
                                    adapter.setUserAvatarBitmap(null);
                                }
                                Toast.makeText(getContext(), "加载头像失败，使用默认头像", Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            mainHandler.post(() -> {
                                if (adapter != null) {
                                    adapter.setUserAvatarBitmap(null);
                                }
                            });
                        }
                    } else {
                        userAvatarBitmap = null;
                        mainHandler.post(() -> {
                            if (adapter != null) {
                                adapter.setUserAvatarBitmap(null);
                            }
                        });
                    }
                }

                @Override
                public void onError(java.sql.SQLException e) {
                    userAvatarBitmap = null;
                    mainHandler.post(() -> {
                        if (adapter != null) {
                            adapter.setUserAvatarBitmap(null);
                        }
                    });
                }
            });
        }
    }

    /**
     * 检查登录状态
     */
    private boolean readLoginStatus() {
        android.content.SharedPreferences sp = getContext().getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE);
        return sp.getBoolean("isLogin", false);
    }

    /**
     * 清空当前对话内容，开始新对话
     */
    public void clearChat() {
        // 清空消息列表
        msgList.clear();
        // 初始化欢迎消息
        initMsgs();
        // 通知适配器数据已更新
        adapter.notifyDataSetChanged();
        // 滚动到最新消息
        msgRecyclerView.scrollToPosition(msgList.size() - 1);
        // 清空输入框
        if (inputText != null) {
            inputText.setText("");
        }
        // 显示提示消息
        Toast.makeText(getContext(), "已开始新对话", Toast.LENGTH_SHORT).show();
    }

    public static class Msg {
        public static final int TYPE_RECEIVED = 0;
        public static final int TYPE_SENT = 1;
        private String content;
        private int type;

        public Msg(String content, int type) {
            this.content = content != null ? content : "";
            this.type = type;
        }

        public String getContent() {
            return content != null ? content : "";
        }

        public int getType() {
            return type;
        }
    }

    /**
     * 显示历史对话列表对话框
     */
    private void showHistoryDialog() {
        // 检查登录状态
        if (!readLoginStatus()) {
            Toast.makeText(getContext(), "请先登录后再查看历史对话", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建对话框构建器
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("历史对话");

        // 获取历史对话列表
        List<ChatHistoryItem> historyList = loadChatHistory();
        
        if (historyList.isEmpty()) {
            builder.setMessage("暂无历史对话记录");
            builder.setPositiveButton("确定", null);
        } else {
            // 创建适配器
            final HistoryAdapter adapter = new HistoryAdapter(getContext(), historyList);
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 加载选中的历史对话
                    loadHistoryChat(historyList.get(which).getId());
                }
            });
            
            builder.setNegativeButton("取消", null);
        }
        
        // 显示对话框
        builder.create().show();
    }

    /**
     * 加载历史对话记录
     */
    private List<ChatHistoryItem> loadChatHistory() {
        List<ChatHistoryItem> historyList = new ArrayList<>();
        
        // 获取当前登录用户名
        String userName = AnalysisUtils.readLoginUserName(getContext());
        if (userName == null || userName.isEmpty()) {
            return historyList;
        }
        
        // 从SharedPreferences加载历史记录
        android.content.SharedPreferences sp = getContext().getSharedPreferences(
                "chat_history_" + userName, android.content.Context.MODE_PRIVATE);
        
        // 获取历史会话ID列表
        String historyIds = sp.getString("history_ids", "");
        if (historyIds.isEmpty()) {
            return historyList;
        }
        
        // 解析历史会话ID
        String[] ids = historyIds.split(",");
        for (String id : ids) {
            if (id.isEmpty()) continue;
            
            // 获取会话标题和时间
            String title = sp.getString("title_" + id, "未命名对话");
            long time = sp.getLong("time_" + id, System.currentTimeMillis());
            
            // 添加到列表
            historyList.add(new ChatHistoryItem(id, title, time));
        }
        
        return historyList;
    }

    /**
     * 加载指定ID的历史对话
     */
    private void loadHistoryChat(String chatId) {
        // 获取当前登录用户名
        String userName = AnalysisUtils.readLoginUserName(getContext());
        if (userName == null || userName.isEmpty()) {
            return;
        }
        
        // 从SharedPreferences加载历史记录
        android.content.SharedPreferences sp = getContext().getSharedPreferences(
                "chat_history_" + userName, android.content.Context.MODE_PRIVATE);
        
        // 获取对话内容
        String chatContent = sp.getString("content_" + chatId, "");
        if (chatContent.isEmpty()) {
            Toast.makeText(getContext(), "无法加载历史对话", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 清空当前对话
        msgList.clear();
        
        // 解析对话内容
        String[] messages = chatContent.split("\\|\\|\\|");
        for (String message : messages) {
            if (message.isEmpty()) continue;
            
            String[] parts = message.split(":::");
            if (parts.length != 2) continue;
            
            int type = Integer.parseInt(parts[0]);
            String content = parts[1];
            
            // 添加消息
            msgList.add(new Msg(content, type));
        }
        
        // 更新UI
        adapter.notifyDataSetChanged();
        msgRecyclerView.scrollToPosition(msgList.size() - 1);
        
        Toast.makeText(getContext(), "已加载历史对话", Toast.LENGTH_SHORT).show();
    }

    /**
     * 保存当前对话
     */
    private void saveCurrentChat() {
        // 检查登录状态和消息列表
        if (!readLoginStatus() || msgList.isEmpty()) {
            return;
        }
        
        // 获取当前登录用户名
        String userName = AnalysisUtils.readLoginUserName(getContext());
        if (userName == null || userName.isEmpty()) {
            return;
        }
        
        // 生成唯一ID - 使用当前会话的唯一ID，避免每次保存都创建新ID
        String chatId = getChatSessionId();
        
        // 构建对话内容
        StringBuilder content = new StringBuilder();
        for (Msg msg : msgList) {
            content.append(msg.getType()).append(":::").append(msg.getContent()).append("|||");
        }
        
        // 获取对话标题（使用第一条用户消息）
        String title = "新对话";
        for (Msg msg : msgList) {
            if (msg.getType() == Msg.TYPE_SENT) {
                title = msg.getContent();
                if (title.length() > 20) {
                    title = title.substring(0, 20) + "...";
                }
                break;
            }
        }
        
        // 保存到SharedPreferences
        android.content.SharedPreferences sp = getContext().getSharedPreferences(
                "chat_history_" + userName, android.content.Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sp.edit();
        
        // 保存对话内容
        editor.putString("content_" + chatId, content.toString());
        editor.putString("title_" + chatId, title);
        editor.putLong("time_" + chatId, System.currentTimeMillis());
        
        // 更新历史ID列表 - 检查是否已存在此ID，避免重复添加
        String historyIds = sp.getString("history_ids", "");
        if (!historyIds.contains(chatId)) {
            editor.putString("history_ids", chatId + "," + historyIds);
        }
        
        editor.apply();
    }
    
    // 对话框变量
    private AlertDialog dialog;
    
    // 当前会话的唯一ID
    private String sessionId = null;
    
    /**
     * 获取当前会话的唯一ID
     */
    private String getChatSessionId() {
        if (sessionId == null) {
            sessionId = System.currentTimeMillis() + "";
        }
        return sessionId;
    }
    
    /**
     * 重置会话ID，用于新对话
     */
    private void resetChatSessionId() {
        sessionId = null;
    }

    /**
     * 历史对话项
     */
    private static class ChatHistoryItem {
        private String id;
        private String title;
        private long time;
        
        public ChatHistoryItem(String id, String title, long time) {
            this.id = id;
            this.title = title;
            this.time = time;
        }
        
        public String getId() {
            return id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public long getTime() {
            return time;
        }
    }

    /**
     * 历史对话适配器
     */
    private static class HistoryAdapter extends android.widget.ArrayAdapter<ChatHistoryItem> {
        public HistoryAdapter(Context context, List<ChatHistoryItem> items) {
            super(context, android.R.layout.simple_list_item_2, android.R.id.text1, items);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            
            ChatHistoryItem item = getItem(position);
            
            android.widget.TextView text1 = view.findViewById(android.R.id.text1);
            android.widget.TextView text2 = view.findViewById(android.R.id.text2);
            
            text1.setText(item.getTitle());
            
            // 格式化时间
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            String timeStr = sdf.format(new java.util.Date(item.getTime()));
            text2.setText(timeStr);
            
            return view;
        }
    }
    
    // 接收DeepSeek响应
    private void receiveDeepSeekResponse(String response) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                // 移除加载消息
                msgList.remove(msgList.size() - 1);
                adapter.notifyItemRemoved(msgList.size());
                
                // 添加AI回复消息
                Msg msg = new Msg(response, Msg.TYPE_RECEIVED);
                msgList.add(msg);
                adapter.notifyItemInserted(msgList.size() - 1);
                msgRecyclerView.scrollToPosition(msgList.size() - 1);
                
                // 保存当前对话到历史记录
                saveCurrentChat();
            }
        });
    }
}
