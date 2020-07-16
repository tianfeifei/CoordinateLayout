package com.example.commoncoordinatelayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import static android.widget.LinearLayout.HORIZONTAL;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final HorizontalLoadMoreLayout coordinateLayout = findViewById(R.id.coordinateLayout);
//        final TextView more = findViewById(R.id.more);
//        final ImageView icon = findViewById(R.id.icon);
//        final RelativeLayout moreLayout = findViewById(R.id.ll_more);
        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2,HORIZONTAL));
        Adapter adapter = new Adapter(this);
        recyclerView.setAdapter(adapter);

//        coordinateLayout.setMoreLayout(moreLayout);
//        coordinateLayout.setMore(more);
//        coordinateLayout.setIcon(icon);
        coordinateLayout.setRecyclerView(recyclerView);

//        more.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(MainActivity.this, "更多", Toast.LENGTH_SHORT).show();
//                Log.e("TFF", "点击");
//
//            }
//        });




        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {

                        //todo 设置左滑最大距离,需要调整下
                        //设置上滑最小高度
                        coordinateLayout.scrollTo(0, 0);
                        //设置RecyclerView高度
                        ViewGroup.LayoutParams layoutParams = recyclerView.getLayoutParams();
                        layoutParams.width = getDisplay();
//                        recyclerView.setLayoutParams(layoutParams);

                        recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
//
    }

    private  int getDisplay() {
        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            return wm.getDefaultDisplay().getWidth();
        } else {
            return -1;
        }
    }
}
