package com.example.xinqiao.activity;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.Toast;

import com.example.xinqiao.R;
import com.example.xinqiao.adapter.ChatSessionAdapter;
import com.example.xinqiao.adapter.MsgAdapter;
import com.example.xinqiao.bean.ChatHistory;
import com.example.xinqiao.bean.ChatSession;
import com.example.xinqiao.dao.ChatHistoryDao;
import com.example.xinqiao.dao.ChatSessionDao;
import com.example.xinqiao.utils.AnalysisUtils;
import com.example.xinqiao.utils.DeepSeekClient;
import com.example.xinqiao.view.AiView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.example.xinqiao.mysql.DBUtils;
import android.os.Looper;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

public class AIActivity extends AppCompatActivity {
    private static final String KEY_MSG_LIST = "msg_list";
    private List<AiView.Msg> msgList = new ArrayList<>();
    private EditText inputText;
    private ImageButton send;
    private ImageButton btnHistory;
    private Button btnNewChat;
    private RecyclerView msgRecyclerView;
    private MsgAdapter adapter;
    private ChatHistoryDao chatHistoryDao;
    private ChatSessionDao chatSessionDao;
    private Bitmap userAvatarBitmap; // 用户头像
    private int currentSessionId = 0; // 当前会话ID
    private DeepSeekClient deepSeekClient; // DeepSeek客户端
    private Handler mainHandler; // 主线程Handler

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_view_ai);
        chatHistoryDao = new ChatHistoryDao(this);
        chatSessionDao = new ChatSessionDao(this);
        deepSeekClient = new DeepSeekClient();
        mainHandler = new Handler(Looper.getMainLooper());
        if (readLoginStatus()) {
            // 如果已登录，异步加载历史记录
            loadChatHistoryAsync();
        } else {
            // 未登录，显示初始消息
            initMsgs();
        }
        inputText = (EditText)findViewById(R.id.input_text);
        send = (ImageButton) findViewById(R.id.send);
        btnHistory = (ImageButton) findViewById(R.id.btn_history);
        btnNewChat = (Button) findViewById(R.id.btn_new_chat);
        msgRecyclerView = (RecyclerView)findViewById(R.id.msg_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        msgRecyclerView.setLayoutManager(layoutManager);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        adapter = new MsgAdapter(msgList, userAvatarBitmap);
        msgRecyclerView.setAdapter(adapter);
        
        // 创建或获取当前会话
        initCurrentSession();
        
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = inputText.getText().toString();
                if(!"".equals(content)){
                    if (readLoginStatus()) {
                        // 已登录状态下保存消息
                        AiView.Msg msg = new AiView.Msg(content, AiView.Msg.TYPE_SENT);
                        msgList.add(msg);
                        saveChatMessageAsync(content, AiView.Msg.TYPE_SENT);
                        adapter.notifyItemInserted(msgList.size() - 1);
                        msgRecyclerView.scrollToPosition(msgList.size() - 1);
                        inputText.setText("");//清空输入框中的内容
                        
                        // 更新会话标题（使用第一条用户消息作为标题）
                        updateSessionTitle();
                        
                        // 显示AI正在思考的消息
                        AiView.Msg loadingMsg = new AiView.Msg("正在思考...", AiView.Msg.TYPE_RECEIVED);
                        msgList.add(loadingMsg);
                        adapter.notifyItemInserted(msgList.size() - 1);
                        msgRecyclerView.scrollToPosition(msgList.size() - 1);
                        
                        // 发送消息到DeepSeek获取AI回复
                        getAIResponse(content);
                    } else {
                        Toast.makeText(AIActivity.this, "请先登录后再进行对话", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        
        // 设置历史记录按钮点击事件
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChatHistoryDialog();
            }
        });
        
        // 设置新对话按钮点击事件
        btnNewChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNewChat();
            }
        });
        
        showActionbar();
        // 加载用户头像
        loadUserAvatar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次页面恢复时刷新用户头像，确保与个人中心同步
        loadUserAvatar();
    }

    private void showActionbar() {
        ActionBar actionBar = this.getSupportActionBar();//定义actionbar上的返回箭头
        if (actionBar != null) {
            actionBar.setTitle("心理咨询");
            actionBar.setDisplayHomeAsUpEnabled(true);
        } else {
            Log.e("AIActivity", "ActionBar is null in showActionbar");
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {//定义actionbar上的返回箭头
        if(item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 保存消息列表状态
        ArrayList<String> messagesToSave = new ArrayList<>();
        for (AiView.Msg msg : msgList) {
            messagesToSave.add(msg.getContent());
            messagesToSave.add(String.valueOf(msg.getType()));
        }
        outState.putStringArrayList(KEY_MSG_LIST, messagesToSave);
    }

    private void initMsgs(){
        AiView.Msg msg1 = new AiView.Msg("你好，欢迎来到心理咨询室。", AiView.Msg.TYPE_RECEIVED);
        msgList.add(msg1);
        AiView.Msg msg2 = new AiView.Msg("我是你的AI心理健康顾问，我会以专业、温暖和理解的态度倾听你的心声。", AiView.Msg.TYPE_RECEIVED);
        msgList.add(msg2);
        AiView.Msg msg3 = new AiView.Msg("无论你遇到什么困扰，都可以和我分享。我们一起探讨，寻找解决方案。", AiView.Msg.TYPE_RECEIVED);
        msgList.add(msg3);
        
        if (readLoginStatus()) {
            // 如果已登录，保存初始消息
            saveChatMessage(msg1.getContent(), msg1.getType());
            saveChatMessage(msg2.getContent(), msg2.getType());
            saveChatMessage(msg3.getContent(), msg3.getType());
        }
    }

    /**
     * 从SharedPreferences中读取登录状态
     */
    private boolean readLoginStatus() {
        SharedPreferences sp = getSharedPreferences("loginInfo", Context.MODE_PRIVATE);
        return sp.getBoolean("isLogin", false);
    }

    /**
     * 保存聊天消息到数据库
     */
    private void saveChatMessage(String content, int type) {
        String userName = AnalysisUtils.readLoginUserName(this);
        ChatHistory chatHistory = new ChatHistory(userName, content, type, System.currentTimeMillis(), currentSessionId);
        chatHistoryDao.saveChatHistory(chatHistory);
    }

    /**
     * 异步保存聊天消息到数据库
     */
    private void saveChatMessageAsync(String content, int type) {
        String userName = AnalysisUtils.readLoginUserName(this);
        ChatHistory chatHistory = new ChatHistory(userName, content, type, System.currentTimeMillis(), currentSessionId);
        chatHistoryDao.saveChatHistoryAsync(chatHistory, success -> {
            if (!success) {
                runOnUiThread(() -> Toast.makeText(this, "聊天记录保存失败", Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    /**
     * 初始化当前会话
     */
    private void initCurrentSession() {
        String userName = AnalysisUtils.readLoginUserName(this);
        if (userName != null && !userName.isEmpty()) {
            // 检查是否有未完成的会话
            chatSessionDao.getLatestSessionAsync(userName, new ChatSessionDao.GetSessionCallback() {
                @Override
                public void onResult(ChatSession session) {
                    if (session != null) {
                        // 使用最新的会话
                        currentSessionId = session.getId();
                        // 加载该会话的历史消息
                        loadChatHistoryBySession(currentSessionId);
                    } else {
                        // 创建新会话
                        createNewSession();
                    }
                }
            });
        }
    }
    
    /**
     * 创建新会话
     */
    private void createNewSession() {
        String userName = AnalysisUtils.readLoginUserName(this);
        if (userName != null && !userName.isEmpty()) {
            ChatSession newSession = new ChatSession(userName, "新对话", System.currentTimeMillis(), System.currentTimeMillis());
            chatSessionDao.createChatSessionAsync(newSession, new ChatSessionDao.CreateSessionCallback() {
                @Override
                public void onResult(int sessionId) {
                    if (sessionId > 0) {
                        currentSessionId = sessionId;
                    } else {
                        Toast.makeText(AIActivity.this, "创建会话失败", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    
    /**
     * 更新会话标题
     */
    private void updateSessionTitle() {
        if (currentSessionId > 0 && !msgList.isEmpty()) {
            // 查找第一条用户发送的消息作为标题
            String title = "新对话";
            for (AiView.Msg msg : msgList) {
                if (msg.getType() == AiView.Msg.TYPE_SENT) {
                    // 使用用户的第一条消息作为标题（限制长度）
                    title = msg.getContent();
                    if (title.length() > 20) {
                        title = title.substring(0, 20) + "...";
                    }
                    break;
                }
            }
            
            // 更新会话标题
            chatSessionDao.updateSessionTitleAsync(currentSessionId, title, null);
        }
    }
    
    /**
     * 根据会话ID加载聊天历史
     */
    private void loadChatHistoryBySession(int sessionId) {
        String userName = AnalysisUtils.readLoginUserName(this);
        if (userName != null && !userName.isEmpty()) {
            msgList.clear();
            chatHistoryDao.getChatHistoryAsync(userName, sessionId, history -> {
                msgList.clear();
                for (ChatHistory chatHistory : history) {
                    msgList.add(new AiView.Msg(chatHistory.getContent(), chatHistory.getType()));
                }
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    if (!msgList.isEmpty()) {
                        msgRecyclerView.scrollToPosition(msgList.size() - 1);
                    }
                });
            });
        }
    }
    
    /**
     * 显示聊天历史对话框
     */
    private void showChatHistoryDialog() {
        String userName = AnalysisUtils.readLoginUserName(this);
        if (userName == null || userName.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 创建对话框视图
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_chat_history, null);
        RecyclerView sessionRecyclerView = dialogView.findViewById(R.id.rv_chat_history);
        Button btnClearHistory = dialogView.findViewById(R.id.btn_clear_history);
        
        // 设置RecyclerView
        sessionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // 创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setTitle("历史对话");
        
        final AlertDialog dialog = builder.create();
        
        // 加载会话列表
        chatSessionDao.getSessionListAsync(userName, new ChatSessionDao.GetSessionListCallback() {
            @Override
            public void onResult(List<ChatSession> sessionList) {
                if (sessionList.isEmpty()) {
                    Toast.makeText(AIActivity.this, "暂无历史对话", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    return;
                }
                
                // 创建适配器
                ChatSessionAdapter sessionAdapter = new ChatSessionAdapter(sessionList);
                sessionRecyclerView.setAdapter(sessionAdapter);
                
                // 设置点击事件
                sessionAdapter.setOnItemClickListener(new ChatSessionAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(ChatSession session) {
                        // 点击会话项时加载该会话的聊天记录
                    currentSessionId = session.getId();
                    loadChatHistoryBySession(currentSessionId);
                    // 更新UI
                    adapter.notifyDataSetChanged();
                    if (!msgList.isEmpty()) {
                        msgRecyclerView.scrollToPosition(msgList.size() - 1);
                    }
                    dialog.dismiss();
                    }
                });
            }
        });
        
        // 设置清空历史按钮点击事件
        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(AIActivity.this)
                        .setTitle("确认清空")
                        .setMessage("确定要清空所有历史对话吗？此操作不可恢复。")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                chatSessionDao.deleteAllSessionsAsync(userName, new ChatSessionDao.DeleteCallback() {
                                    @Override
                                    public void onResult(boolean success) {
                                        if (success) {
                                            runOnUiThread(() -> {
                                                Toast.makeText(AIActivity.this, "历史对话已清空", Toast.LENGTH_SHORT).show();
                                                dialog.dismiss();
                                                // 创建新会话
                                                startNewChat();
                                            });
                                        } else {
                                            runOnUiThread(() -> {
                                                Toast.makeText(AIActivity.this, "清空失败", Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    }
                                });
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
        
        dialog.show();
    }
    
    /**
     * 开始新对话
     */
    private void startNewChat() {
        // 清空当前消息列表
        msgList.clear();
        
        // 显示初始消息
        initMsgs();
        
        adapter.notifyDataSetChanged();
        msgRecyclerView.scrollToPosition(0);
        
        // 创建新会话
        createNewSession();
        
        Toast.makeText(this, "已开始新对话", Toast.LENGTH_SHORT).show();
    }

    /**
     * 异步加载历史聊天记录
     */
    private void loadChatHistoryAsync() {
        String userName = AnalysisUtils.readLoginUserName(this);
        chatHistoryDao.getChatHistoryAsync(userName, history -> {
            msgList.clear();
            for (ChatHistory chatHistory : history) {
                msgList.add(new AiView.Msg(chatHistory.getContent(), chatHistory.getType()));
            }
            if (msgList.isEmpty()) {
                // 如果没有历史记录，显示初始消息
                initMsgs();
            }
            runOnUiThread(() -> adapter.notifyDataSetChanged());
        });
    }
    
    /**
     * 获取AI回复
     */
    private void getAIResponse(String userMessage) {
        deepSeekClient.sendMessage(userMessage, new DeepSeekClient.ChatCallback() {
            @Override
            public void onSuccess(String response) {
                mainHandler.post(() -> {
                    // 移除"正在思考..."的消息
                    msgList.remove(msgList.size() - 1);
                    adapter.notifyItemRemoved(msgList.size() - 1);
                    
                    // 添加AI回复消息
                    AiView.Msg aiMsg = new AiView.Msg(response, AiView.Msg.TYPE_RECEIVED);
                    msgList.add(aiMsg);
                    adapter.notifyItemInserted(msgList.size() - 1);
                    msgRecyclerView.scrollToPosition(msgList.size() - 1);
                    
                    // 保存AI回复到数据库
                    saveChatMessageAsync(response, AiView.Msg.TYPE_RECEIVED);
                });
            }
            
            @Override
            public void onFailure(String error) {
                mainHandler.post(() -> {
                    // 移除"正在思考..."的消息
                    msgList.remove(msgList.size() - 1);
                    adapter.notifyItemRemoved(msgList.size() - 1);
                    
                    // 添加错误消息
                    AiView.Msg errorMsg = new AiView.Msg("抱歉，我暂时无法回复，请稍后再试。错误：" + error, AiView.Msg.TYPE_RECEIVED);
                    msgList.add(errorMsg);
                    adapter.notifyItemInserted(msgList.size() - 1);
                    msgRecyclerView.scrollToPosition(msgList.size() - 1);
                    
                    // 保存错误消息到数据库
                    saveChatMessageAsync(errorMsg.getContent(), AiView.Msg.TYPE_RECEIVED);
                    
                    Toast.makeText(AIActivity.this, "AI回复失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
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

        String userName = AnalysisUtils.readLoginUserName(this);
        DBUtils dbUtils = null;
        try {
            dbUtils = DBUtils.getInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (dbUtils != null) {
            dbUtils.getUserAvatarPath(userName, new DBUtils.AvatarPathCallback() {
                @Override
                public void onSuccess(String avatarBase64) {
                    if (avatarBase64 != null && avatarBase64.startsWith("data:image")) {
                        String base64 = avatarBase64.substring(avatarBase64.indexOf(",") + 1);
                        byte[] avatarData = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                        // 使用BitmapFactory.Options设置采样率，避免OOM
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2; // 降低采样率，减少内存使用
                options.inPreferredConfig = Bitmap.Config.RGB_565; // 使用RGB_565配置减少内存使用
                userAvatarBitmap = BitmapFactory.decodeByteArray(avatarData, 0, avatarData.length, options);
                        runOnUiThread(() -> {
                            if (adapter != null) {
                                adapter.setUserAvatarBitmap(userAvatarBitmap);
                            }
                        });
                    } else {
                        userAvatarBitmap = null;
                        runOnUiThread(() -> {
                            if (adapter != null) {
                                adapter.setUserAvatarBitmap(null);
                            }
                        });
                    }
                }

                @Override
                public void onError(java.sql.SQLException e) {
                    userAvatarBitmap = null;
                    runOnUiThread(() -> {
                        if (adapter != null) {
                            adapter.setUserAvatarBitmap(null);
                        }
                    });
                }
            });
        }
    }
}