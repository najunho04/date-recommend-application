package com.najunho.datecourserecommendapplication.Util;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.najunho.datecourserecommendapplication.DB.Content;
import com.najunho.datecourserecommendapplication.DB.Post;

import java.util.ArrayList;
import java.util.List;

public class PostSetUpLogic {
    public static void postSetUp(Activity activity, String title, Post post, List<Content> contentList, List<String> selectedInterests,
                                 OnPostSetUpListener callback ){
        if (title.isEmpty()) {
            Toast.makeText(activity, "제목이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        post.setTitle(title);

        //contentList order re setup -> drag로직 때문.
        for (int i = 0; i < contentList.size(); i++) {
            contentList.get(i).setOrder(i);
        }

        //preView 설정
        post.setPreviewText(contentList.get(0).getDescription());

        //postInterests 설정
        List<String> newInterests = new ArrayList<>(selectedInterests);
        post.setPostInterests(newInterests);

        //Post core region 설정 + Post core 좌표 설정
        for (Content content : contentList) {
            if(content.getIsCore() == true){
                post.setCoreRegion(content.getLocation().getRegion1());

                post.setCoreLatitude(content.getLocation().getLatitude());
                post.setCoreLongitude(content.getLocation().getLongitude());

                Log.d("setCoreRegion", post.getCoreRegion());
                break;
            }
        }
        if (post.getCoreRegion() == null){
            post.setCoreRegion(contentList.get(0).getLocation().getRegion1());

            post.setCoreLatitude(contentList.get(0).getLocation().getLatitude());
            post.setCoreLongitude(contentList.get(0).getLocation().getLongitude());

            Log.d("setCoreRegion", post.getCoreRegion());
        }

        //post_thumbnail 설정 -> 필요
        if(contentList.get(0).getImageUrl() != null) {
            post.setThumbnail(contentList.get(0).getImageUrl().get(0));
        }else {
            post.setThumbnail(null);
        }

        //시간 데이터 확인 및 설정
        for (Content content : contentList) {
            String startTime = content.getStartTimeString();
            String endTime = content.getEndTimeString();
            if (TimeCheck.isValidTimeFormat(startTime)) {
                content.setStartTime(TimeCheck.convertToTimestamp(startTime));
            } else {
                Toast.makeText(activity, "시간 형식이 올바르지 않습니다"
                        , Toast.LENGTH_SHORT).show();
                return;
            }
            if (TimeCheck.isValidTimeFormat(endTime)) {
                content.setEndTime(TimeCheck.convertToTimestamp(endTime));
            } else {
                Toast.makeText(activity, "시간 형식이 올바르지 않습니다"
                        , Toast.LENGTH_SHORT).show();
                return;
            }
        }

        callback.onSuccess(post, contentList);
    }

    public interface OnPostSetUpListener {
        void onSuccess(Post post, List<Content> contentList);
    }

}
