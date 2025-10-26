package com.example.xinqiao.utils;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeepSeekClient {
    private static final String TAG = "DeepSeekClient";
    // DeepSeek API URL
    private static final String BASE_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client;
    private static final String MODEL_NAME = "deepseek-chat"; // 使用DeepSeek Chat模型
    private static final String API_KEY = "sk-270769a7c1f24907bd71f8bdc4166c7b"; // DeepSeek API密钥
    private static final int MAX_RETRIES = 3;
    private int currentRetry = 0;

    public DeepSeekClient() {
        // 配置更详细的网络连接参数
        client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)  // 增加连接超时时间到120秒
                .readTimeout(120, TimeUnit.SECONDS)     // 增加读取超时时间到120秒
                .writeTimeout(120, TimeUnit.SECONDS)    // 增加写入超时时间到120秒
                .retryOnConnectionFailure(true)         // 启用连接失败重试
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES)) // 配置连接池
                .build();
    }

    public interface ChatCallback {
        void onSuccess(String response);
        void onFailure(String error);
    }

    public void sendMessage(String message, ChatCallback callback) {
        sendMessageWithRetry(message, callback, 0);
    }

    private void sendMessageWithRetry(String message, ChatCallback callback, int retryCount) {
        JSONObject requestBody = new JSONObject();
        try {
            // 构建系统提示词
            String systemPrompt = "你是一位经验丰富的心理咨询师，具有以下特点：\n" +
                    "1. 使用温和、共情的语气，让来访者感受到被理解和接纳\n" +
                    "2. 善于倾听，不轻易打断或评判\n" +
                    "3. 通过提问帮助来访者深入思考\n" +
                    "4. 在适当的时候给予专业的建议和指导\n" +
                    "5. 保持专业性和边界感\n" +
                    "6. 使用\"我理解\"、\"我感受到\"等表达共情的语言\n" +
                    "7. 避免使用过于直接或命令式的语言\n" +
                    "8. 在回答中适当使用心理咨询的专业术语\n" +
                    "9. 注意保护来访者的隐私和感受\n" +
                    "10. 在必要时建议寻求专业帮助\n\n" +
                    "请以这样的角色和风格回答用户的问题。";

            // DeepSeek API格式
            requestBody.put("model", MODEL_NAME);
            
            // 创建消息数组
            JSONArray messagesArray = new JSONArray();
            
            // 添加系统消息
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messagesArray.put(systemMessage);
            
            // 添加用户消息
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messagesArray.put(userMessage);
            
            // 将消息数组添加到请求体
            requestBody.put("messages", messagesArray);
            requestBody.put("stream", false);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            String jsonString = requestBody.toString();
            Log.d(TAG, "Request URL: " + BASE_URL);
            Log.d(TAG, "Request Body: " + jsonString);
            Log.d(TAG, "Retry count: " + retryCount);

            RequestBody body = RequestBody.create(jsonString, JSON);
            Request request = new Request.Builder()
                    .url(BASE_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .post(body)
                    .build();

            Log.d(TAG, "Sending request to: " + request.url());
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Request failed", e);
                    Log.e(TAG, "Error message: " + e.getMessage());
                    Log.e(TAG, "Error cause: " + e.getCause());
                    
                    String errorMessage;
                    if (e instanceof SocketTimeoutException) {
                        errorMessage = "连接超时，请检查网络连接";
                    } else if (e instanceof ConnectException) {
                        errorMessage = "无法连接到DeepSeek API服务器，请检查网络连接";
                    } else {
                        errorMessage = "网络请求失败: " + e.getMessage();
                    }

                    if (retryCount < MAX_RETRIES) {
                        Log.d(TAG, "Retrying request... Attempt " + (retryCount + 1) + " of " + MAX_RETRIES);
                        try {
                            // 使用指数退避策略，每次重试等待时间翻倍
                            long waitTime = (long) (2000 * Math.pow(2, retryCount));
                            Log.d(TAG, "Waiting " + waitTime + "ms before retry");
                            Thread.sleep(waitTime);
                            sendMessageWithRetry(message, callback, retryCount + 1);
                        } catch (InterruptedException ie) {
                            Log.e(TAG, "Retry interrupted", ie);
                            callback.onFailure(errorMessage);
                        }
                    } else {
                        callback.onFailure(errorMessage + "\n已重试" + MAX_RETRIES + "次，仍然失败。请检查：\n" +
                                "1. 网络连接是否正常\n" +
                                "2. API密钥是否有效\n" +
                                "3. DeepSeek API服务是否可用");
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response Code: " + response.code());
                    Log.d(TAG, "Response Headers: " + response.headers());
                    Log.d(TAG, "Response Body: " + responseBody);

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONArray choices = jsonResponse.getJSONArray("choices");
                            if (choices.length() > 0) {
                                JSONObject firstChoice = choices.getJSONObject(0);
                                JSONObject message = firstChoice.getJSONObject("message");
                                String content = message.getString("content");
                                // 清理内容
                                content = content.trim();
                                callback.onSuccess(content);
                            } else {
                                callback.onFailure("API返回的响应中没有内容");
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Failed to parse response", e);
                            callback.onFailure("解析响应失败: " + e.getMessage());
                        }
                    } else {
                        try {
                            JSONObject errorJson = new JSONObject(responseBody);
                            String errorMessage;
                            if (errorJson.has("error")) {
                                JSONObject error = errorJson.getJSONObject("error");
                                errorMessage = error.optString("message", "未知错误");
                            } else {
                                errorMessage = errorJson.optString("message", "未知错误");
                            }
                            callback.onFailure("API请求失败: " + errorMessage);
                        } catch (JSONException e) {
                            callback.onFailure("服务器响应错误: " + response.code() + "\n" + responseBody);
                        }
                    }
                    response.close();
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create request body", e);
            callback.onFailure("创建请求失败: " + e.getMessage());
        }
    }
}