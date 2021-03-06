package com.cwc.vplayer.controller;

import android.content.Context;
import android.view.Surface;


import com.cwc.vplayer.player.base.player.IPlayerManager;

import java.io.File;
import java.util.Map;

/**
 * Manager 与 View之间的接口
 */

public interface VideoViewBridge {

    MediaPlayerListener listener();

    MediaPlayerListener lastListener();

    void setListener(MediaPlayerListener listener);

    void setLastListener(MediaPlayerListener lastListener);

    /**
     * tag和position都是属于标记flag，不参与播放器实际工作，只是用于防止错误等等
     */
    String getPlayTag();

    void setPlayTag(String playTag);

    int getPlayPosition();

    void setPlayPosition(int playPosition);


    /**
     * 开始准备播放
     *
     * @param url               播放url
     * @param mapHeadData       头部信息
     * @param loop              是否循环
     * @param speed             播放速度
     * @param cache             是否缓存
     * @param cachePath         缓存目录，可以为空，为空时使用默认
     * @param overrideExtension 是否需要覆盖拓展类型
     */
    void prepare(final String url, final Map<String, String> mapHeadData, boolean loop, float speed, boolean cache, File cachePath, String overrideExtension);

    /**
     * 获取当前播放内核
     */
    IPlayerManager getPlayer();

    /**
     * 针对某些内核，缓冲百分比
     */
    int getBufferedPercentage();

    /**
     * 是否播放器
     */
    void releaseMediaPlayer();

    void setCurrentVideoHeight(int currentVideoHeight);

    void setCurrentVideoWidth(int currentVideoWidth);

    int getCurrentVideoWidth();

    int getCurrentVideoHeight();

    /**
     * 设置渲染
     */
    void setDisplay(Surface holder);

    void releaseSurface(Surface surface);

    int getLastState();



    /**
     * 网络速度
     */
    long getNetSpeed();

    /**
     * 播放速度修改
     *
     * @param speed 播放速度
     */
    void setSpeed(float speed, boolean soundTouch);

    /**
     * 播放速度修改
     *
     * @param speed      播放速度
     * @param soundTouch
     */
    void setSpeedPlaying(float speed, boolean soundTouch);

    /**
     * 获取Rotate选择的flag，目前只有ijk用到
     */
    int getRotateInfoFlag();

    void start();

    void stop();

    void pause();

    int getVideoWidth();

    int getVideoHeight();

    boolean isPlaying();

    void seekTo(long time);

    long getCurrentPosition();

    long getDuration();
}
