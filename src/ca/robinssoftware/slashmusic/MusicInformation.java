/*
Copyright 2022 Robins Software

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package ca.robinssoftware.slashmusic;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

public class MusicInformation {

    private static final String API_KEY = "AIzaSyBuy0Rq347qByz-38wWNviWUrDlRnL-m98";
    
    Video video;
    
    public MusicInformation(String id) {
        try {
            // thanks to https://stackoverflow.com/questions/30567361/retrieving-details-from-a-youtube-video-given-the-url
            YouTube youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(),
                    new HttpRequestInitializer() {
                        public void initialize(HttpRequest request) throws IOException {
                        }
                    }).setApplicationName("slashmusic").build();

            YouTube.Videos.List videoRequest = youtube.videos().list("snippet,statistics,contentDetails");
            videoRequest.setId(id);
            videoRequest.setKey(API_KEY);
            VideoListResponse listResponse = videoRequest.execute();
            List<Video> videoList = listResponse.getItems();
            
            Video targetVideo = videoList.iterator().next();
            video = targetVideo;
        } catch (IOException | NullPointerException | NoSuchElementException e) {
            video = null;
        }
    }
    
    public int getSeconds() {
        return getMinutesAndSeconds()[1] + (getMinutesAndSeconds()[0] * 60);
    }
    
    public int[] getMinutesAndSeconds() {
        int[] array = new int[2];
        
        if (video.getContentDetails().getDuration().contains("M")) {
            String str[] = video.getContentDetails().getDuration().replaceFirst("PT", "").replaceFirst("S", "").split("M");
            array[0] = Integer.parseInt(str[0]);
            array[1] = Integer.parseInt(str[1]);
        } else {
            array[0] = 0;
            array[1] = Integer.parseInt(video.getContentDetails().getDuration().replaceFirst("PT", "").replaceFirst("S", ""));
        }
                
        return array;
    }
    
    public boolean exists() {
        return video != null;
    }
    
    public Video getVideo() {
        return video;
    }

}
