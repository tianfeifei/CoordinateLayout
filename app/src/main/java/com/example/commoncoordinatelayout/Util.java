package com.example.commoncoordinatelayout;

import android.content.Context;
import android.graphics.Paint;
import android.view.View;
import android.widget.TextView;

/**
 * @author tianfeifei on 2020/7/7
 */
public class Util {

    //方法入口
    public static float getCharacterWidth(TextView tv,int size){
        if(null == tv) return 0f;
        float v = getCharacterWidth(tv.getText().toString(), tv.getTextSize()) * tv.getScaleX();
        return v*size;
    }

    //获取每个字符的宽度bai主方法：
    private static float getCharacterWidth(String text, float size){
        if(null == text || "".equals(text))
            return 0;
        float width = 0;
        Paint paint = new Paint();
        paint.setTextSize(size);
        float text_width = paint.measureText(text);//得到总体长度
        width = text_width/text.length();//每一个字符的长度
        return width;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}
