package com.example.xinqiao.util;

import com.example.xinqiao.R;
import com.example.xinqiao.bean.ArticleBean;

import java.util.List;

/**
 * 文章图片解析工具：根据文章ID补全本地图片资源ID。
 * 用于阅读历史条目未保存图片时的展示补全。
 */
public class ArticleImageResolver {

    // 文章ID与图片资源的静态映射（示例文章1-10）
    private static final int[] ARTICLE_IMAGE_MAP = new int[]{
            R.drawable.bg_11,
            R.drawable.bg_12,
            R.drawable.bg_13,
            R.drawable.bg_14,
            R.drawable.bg_15,
            R.drawable.bg_16,
            R.drawable.bg_17,
            R.drawable.bg_18,
            R.drawable.bg_19,
            R.drawable.bg_20
    };

    /**
     * 根据文章ID返回图片资源ID；若超出范围则返回默认图。
     */
    public static int resolveImageResId(int articleId) {
        if (articleId >= 1 && articleId <= ARTICLE_IMAGE_MAP.length) {
            return ARTICLE_IMAGE_MAP[articleId - 1];
        }
        return R.drawable.default_img;
    }

    /**
     * 为列表中缺失图片的文章补全图片资源ID。
     */
    public static void enrichImages(List<ArticleBean> list) {
        if (list == null) return;
        for (ArticleBean a : list) {
            if (a != null && a.imageResId == 0) {
                a.imageResId = resolveImageResId(a.articleId);
            }
        }
    }
}

