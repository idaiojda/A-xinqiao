package com.example.xinqiao.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.example.xinqiao.bean.ArticleBean;
import com.example.xinqiao.utils.DBHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章收藏数据访问对象
 */
public class ArticleFavoriteDao {
    private DBHelper dbHelper;
    private static final String TABLE_NAME = "article_favorite";

    public ArticleFavoriteDao(Context context) {
        dbHelper = DBHelper.getInstance(context);
    }

    /**
     * 创建文章收藏表
     */
    public void createTable() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // 创建文章收藏表
        String createTableSql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_name TEXT, " +
                "article_id INTEGER, " +
                "title TEXT, " +
                "content TEXT, " +
                "category TEXT, " +
                "summary TEXT, " +
                "image_res_id INTEGER, " +
                "favorite_timestamp INTEGER, " +
                "UNIQUE(user_name, article_id)" +
                ")";
        db.execSQL(createTableSql);
    }

    /**
     * 异步保存文章收藏
     */
    public void saveFavoriteAsync(ArticleBean article, OnSaveListener listener) {
        new AsyncTask<ArticleBean, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(ArticleBean... articles) {
                return saveFavorite(articles[0]);
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (listener != null) {
                    listener.onSave(success);
                }
            }
        }.execute(article);
    }

    /**
     * 保存文章收藏
     */
    private boolean saveFavorite(ArticleBean article) {
        createTable();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_name", article.userName);
        values.put("article_id", article.articleId);
        values.put("title", article.title);
        values.put("content", article.content);
        values.put("category", article.category);
        values.put("summary", article.summary);
        values.put("image_res_id", article.imageResId);
        values.put("favorite_timestamp", System.currentTimeMillis());

        long result = db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    /**
     * 异步删除文章收藏
     */
    public void deleteFavoriteAsync(String userName, int articleId, OnDeleteListener listener) {
        new AsyncTask<Object, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Object... params) {
                String userName = (String) params[0];
                int articleId = (int) params[1];
                return deleteFavorite(userName, articleId);
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (listener != null) {
                    listener.onDelete(success);
                }
            }
        }.execute(userName, articleId);
    }

    /**
     * 删除文章收藏
     */
    private boolean deleteFavorite(String userName, int articleId) {
        createTable();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete(TABLE_NAME, "user_name=? AND article_id=?", 
                new String[]{userName, String.valueOf(articleId)});
        return result > 0;
    }

    /**
     * 异步检查文章是否已收藏
     */
    public void isFavoriteExistsAsync(String userName, int articleId, OnCheckListener listener) {
        new AsyncTask<Object, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Object... params) {
                String userName = (String) params[0];
                int articleId = (int) params[1];
                return isFavoriteExists(userName, articleId);
            }

            @Override
            protected void onPostExecute(Boolean exists) {
                if (listener != null) {
                    listener.onCheck(exists);
                }
            }
        }.execute(userName, articleId);
    }

    /**
     * 检查文章是否已收藏
     */
    private boolean isFavoriteExists(String userName, int articleId) {
        createTable();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, "user_name=? AND article_id=?",
                new String[]{userName, String.valueOf(articleId)}, null, null, null);
        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }
        return exists;
    }

    /**
     * 异步获取用户的所有收藏文章
     */
    public void getFavoriteArticlesAsync(String userName, OnLoadListener listener) {
        new AsyncTask<String, Void, List<ArticleBean>>() {
            @Override
            protected List<ArticleBean> doInBackground(String... params) {
                return getFavoriteArticles(params[0]);
            }

            @Override
            protected void onPostExecute(List<ArticleBean> articles) {
                if (listener != null) {
                    listener.onLoad(articles);
                }
            }
        }.execute(userName);
    }

    /**
     * 获取用户的所有收藏文章
     */
    private List<ArticleBean> getFavoriteArticles(String userName) {
        createTable();
        List<ArticleBean> articles = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, "user_name=?",
                new String[]{userName}, null, null, "favorite_timestamp DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                ArticleBean article = new ArticleBean();
                article.userName = userName;
                article.articleId = cursor.getInt(cursor.getColumnIndex("article_id"));
                article.title = cursor.getString(cursor.getColumnIndex("title"));
                article.content = cursor.getString(cursor.getColumnIndex("content"));
                article.category = cursor.getString(cursor.getColumnIndex("category"));
                article.summary = cursor.getString(cursor.getColumnIndex("summary"));
                article.imageResId = cursor.getInt(cursor.getColumnIndex("image_res_id"));
                articles.add(article);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return articles;
    }

    /**
     * 保存监听器
     */
    public interface OnSaveListener {
        void onSave(boolean success);
    }

    /**
     * 删除监听器
     */
    public interface OnDeleteListener {
        void onDelete(boolean success);
    }

    /**
     * 检查监听器
     */
    public interface OnCheckListener {
        void onCheck(boolean exists);
    }

    /**
     * 加载监听器
     */
    public interface OnLoadListener {
        void onLoad(List<ArticleBean> articles);
    }
}