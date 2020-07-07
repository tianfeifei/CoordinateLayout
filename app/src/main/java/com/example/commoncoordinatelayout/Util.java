package com.example.commoncoordinatelayout;

import android.graphics.Paint;
import android.view.View;
import android.widget.TextView;

/**
 * @author tianfeifei on 2020/7/7
 */
public class Util {

    //方法入口
    public static float getCharacterWidth(TextView tv){
        if(null == tv) return 0f;
        return getCharacterWidth("更多",tv.getTextSize()) * tv.getScaleX();
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


    public static void startRotateAnimation(View view) {
    }
}
