package com.cwc.vplayer.controller;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.Surface;

import com.cwc.vplayer.factory.PlayerFactory;
import com.cwc.vplayer.player.base.model.VideoModel;
import com.cwc.vplayer.player.base.model.VideoOptionModel;
import com.cwc.vplayer.player.base.player.BasePlayerManager;
import com.cwc.vplayer.player.base.player.IPlayerInitSuccessListener;
import com.cwc.vplayer.player.base.player.IPlayerManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * 基类管理器
 * VideoViewBridge接口说明可以查阅VideoViewBridge类
 */

public abstract class VideoBaseManager implements IMediaPlayer.OnPreparedListener, IMediaPlayer.OnCompletionListener,
        IMediaPlayer.OnBufferingUpdateListener, IMediaPlayer.OnSeekCompleteListener, IMediaPlayer.OnErrorListener,
        IMediaPlayer.OnVideoSizeChangedListener, IMediaPlayer.OnInfoListener,  VideoViewBridge {

    private static final int HANDLER_PREPARE = 0;
    private static final int HANDLER_SETDISPLAY = 1;
    private static final int HANDLER_RELEASE = 2;
    private static final int HANDLER_RELEASE_SURFACE = 3;
    private static final int BUFFER_TIME_OUT_ERROR = -192;//外部超时错误码
    private Context context;
    private MediaHandler mMediaHandler;
    private Handler mainThreadHandler;
    private WeakReference<MediaPlayerListener> listener;
    private WeakReference<MediaPlayerListener> lastListener;
    private IPlayerInitSuccessListener mPlayerInitSuccessListener;

    /**
     * 配置ijk option
     */
    private List<VideoOptionModel> optionModelList;

    /**
     * 播放的tag，防止错位置，因为普通的url也可能重复
     */
    protected String playTag = "";

    /**
     * 播放内核管理
     */
    private IPlayerManager playerManager;


    /**
     * 当前播放的视频宽的高
     */
    protected int currentVideoWidth = 0;

    /**
     * 当前播放的视屏的高
     */
    protected int currentVideoHeight = 0;

    /**
     * 当前视频的最后状态
     */
    protected int lastState;

    /**
     * 播放的tag，防止错位置，因为普通的url也可能重复
     */
    protected int playPosition = -22;

    /**
     * 缓冲比例
     */
    protected int bufferPoint;

    /**
     * 播放超时
     */
    protected int timeOut = 8 * 1000;

    /**
     * 是否需要静音
     */
    protected boolean needMute = false;

    /**
     * 是否需要外部超时判断
     */
    protected boolean needTimeOutOther;


    protected void init() {
        mMediaHandler = new MediaHandler((Looper.getMainLooper()));
        mainThreadHandler = new Handler();
    }

    protected IPlayerManager getPlayManager() {
        return PlayerFactory.getPlayManager();
    }


    @Override
    public MediaPlayerListener listener() {
        if (listener == null)
            return null;
        return listener.get();
    }

    @Override
    public MediaPlayerListener lastListener() {
        if (lastListener == null)
            return null;
        return lastListener.get();
    }

    @Override
    public void setListener(MediaPlayerListener listener) {
        if (listener == null)
            this.listener = null;
        else
            this.listener = new WeakReference<>(listener);
    }

    @Override
    public void setLastListener(MediaPlayerListener lastListener) {
        if (lastListener == null)
            this.lastListener = null;
        else
            this.lastListener = new WeakReference<>(lastListener);
    }

    @Override
    public void setSpeed(float speed, boolean soundTouch) {
        if (playerManager != null) {
            playerManager.setSpeed(speed, soundTouch);
        }
    }


    @Override
    public void prepare(final String url, final Map<String, String> mapHeadData, boolean loop, float speed, boolean cache, File cachePath, String overrideExtension) {
        if (TextUtils.isEmpty(url)) return;
        Message msg = new Message();
        msg.what = HANDLER_PREPARE;
        VideoModel fb = new VideoModel(url, mapHeadData, loop, speed, cache, cachePath, overrideExtension);
        msg.obj = fb;
        sendMessage(msg);
        if (needTimeOutOther) {
            startTimeOutBuffer();
        }
    }

    @Override
    public void releaseMediaPlayer() {
        Message msg = new Message();
        msg.what = HANDLER_RELEASE;
        sendMessage(msg);
        playTag = "";
        playPosition = -22;
    }

    @Override
    public void setDisplay(Surface holder) {
        Message msg = new Message();
        msg.what = HANDLER_SETDISPLAY;
        msg.obj = holder;
        showDisplay(msg);
    }

    @Override
    public void releaseSurface(Surface holder) {
        Message msg = new Message();
        msg.what = HANDLER_RELEASE_SURFACE;
        msg.obj = holder;
        sendMessage(msg);
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                cancelTimeOutBuffer();
                if (listener() != null) {
                    listener().onPrepared();
                }
            }
        });
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                cancelTimeOutBuffer();
                if (listener() != null) {
                    listener().onAutoCompletion();
                }
            }
        });
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, final int percent) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener() != null) {
                    if (percent > bufferPoint) {
                        listener().onBufferingUpdate(percent);
                    } else {
                        listener().onBufferingUpdate(bufferPoint);
                    }
                }
            }
        });
    }

    @Override
    public void onSeekComplete(IMediaPlayer mp) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                cancelTimeOutBuffer();
                if (listener() != null) {
                    listener().onSeekComplete();
                }
            }
        });
    }

    @Override
    public boolean onError(IMediaPlayer mp, final int what, final int extra) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                cancelTimeOutBuffer();
                if (listener() != null) {
                    listener().onError(what, extra);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onInfo(IMediaPlayer mp, final int what, final int extra) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (needTimeOutOther) {
                    if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                        startTimeOutBuffer();
                    } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                        cancelTimeOutBuffer();
                    }
                }
                if (listener() != null) {
                    listener().onInfo(what, extra);
                }
            }
        });
        return false;
    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
        currentVideoWidth = mp.getVideoWidth();
        currentVideoHeight = mp.getVideoHeight();
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener() != null) {
                    listener().onVideoSizeChanged();
                }
            }
        });
    }


    @Override
    public int getLastState() {
        return lastState;
    }


    @Override
    public int getCurrentVideoWidth() {
        return currentVideoWidth;
    }

    @Override
    public int getCurrentVideoHeight() {
        return currentVideoHeight;
    }

    @Override
    public void setCurrentVideoHeight(int currentVideoHeight) {
        this.currentVideoHeight = currentVideoHeight;
    }

    @Override
    public void setCurrentVideoWidth(int currentVideoWidth) {
        this.currentVideoWidth = currentVideoWidth;
    }

    @Override
    public String getPlayTag() {
        return playTag;
    }

    @Override
    public void setPlayTag(String playTag) {
        this.playTag = playTag;
    }

    @Override
    public int getPlayPosition() {
        return playPosition;
    }

    @Override
    public void setPlayPosition(int playPosition) {
        this.playPosition = playPosition;
    }



    @Override
    public long getNetSpeed() {
        if (playerManager != null) {
            return playerManager.getNetSpeed();
        }
        return 0;
    }



    @Override
    public int getBufferedPercentage() {
        if (playerManager != null) {
            return playerManager.getBufferedPercentage();
        }
        return 0;
    }

    @Override
    public void setSpeedPlaying(float speed, boolean soundTouch) {
        if (playerManager != null) {
            playerManager.setSpeedPlaying(speed, soundTouch);
        }
    }

    @Override
    public IPlayerManager getPlayer() {
        return playerManager;
    }

    @Override
    public void start() {
        if (playerManager != null) {
            playerManager.start();
        }
    }

    @Override
    public void stop() {
        if (playerManager != null) {
            playerManager.stop();
        }
    }

    @Override
    public void pause() {
        if (playerManager != null) {
            playerManager.pause();
        }
    }

    @Override
    public int getVideoWidth() {
        if (playerManager != null) {
            return playerManager.getVideoWidth();
        }
        return 0;
    }

    @Override
    public int getVideoHeight() {
        if (playerManager != null) {
            return playerManager.getVideoHeight();
        }
        return 0;
    }

    @Override
    public boolean isPlaying() {
        if (playerManager != null) {
            return playerManager.isPlaying();
        }
        return false;
    }

    @Override
    public void seekTo(long time) {
        if (playerManager != null) {
            playerManager.seekTo(time);
        }
    }

    @Override
    public long getCurrentPosition() {
        if (playerManager != null) {
            return playerManager.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public long getDuration() {
        if (playerManager != null) {
            return playerManager.getDuration();
        }
        return 0;
    }

    @Override
    public int getRotateInfoFlag() {
        return IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED;
    }

    protected void sendMessage(Message message) {
        mMediaHandler.sendMessage(message);
    }

    private class MediaHandler extends Handler {

        MediaHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_PREPARE:
                    initVideo(msg);
                    break;
                case HANDLER_SETDISPLAY:
                    break;
                case HANDLER_RELEASE:
                    if (playerManager != null) {
                        playerManager.release();
                    }
                    bufferPoint = 0;
                    setNeedMute(false);
                    cancelTimeOutBuffer();
                    break;
                case HANDLER_RELEASE_SURFACE:
                    releaseSurface(msg);
                    break;
            }
        }

    }

    private void initVideo(Message msg) {
        try {
            currentVideoWidth = 0;
            currentVideoHeight = 0;

            if (playerManager != null) {
                playerManager.release();
            }
            playerManager = getPlayManager();
            if (playerManager instanceof BasePlayerManager) {
                ((BasePlayerManager) playerManager)
                        .setPlayerInitSuccessListener(mPlayerInitSuccessListener);
            }
            playerManager.initVideoPlayer(context, msg, optionModelList);

            setNeedMute(needMute);
            IMediaPlayer mediaPlayer = playerManager.getMediaPlayer();
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnSeekCompleteListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnInfoListener(this);
            mediaPlayer.setOnVideoSizeChangedListener(this);
            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 启动十秒的定时器进行 缓存操作
     */
    protected void startTimeOutBuffer() {
        // 启动定时
        mainThreadHandler.postDelayed(mTimeOutRunnable, timeOut);

    }

    /**
     * 取消 十秒的定时器进行 缓存操作
     */
    protected void cancelTimeOutBuffer() {
        // 取消定时
        if (needTimeOutOther)
            mainThreadHandler.removeCallbacks(mTimeOutRunnable);
    }


    private Runnable mTimeOutRunnable = new Runnable() {
        @Override
        public void run() {
            if (listener != null) {
                listener().onError(BUFFER_TIME_OUT_ERROR, BUFFER_TIME_OUT_ERROR);
            }
        }
    };

    private void releaseSurface(Message msg) {
        if (msg.obj != null) {
            if (playerManager != null) {
                playerManager.releaseSurface();
            }
        }
    }

    /**
     * 后面再修改设计模式吧，现在先用着
     */
    private void showDisplay(Message msg) {
        if (playerManager != null) {
            playerManager.showDisplay(msg);
        }
    }

    public void initContext(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 打开raw播放支持
     *
     * @param context
     */
    public void enableRawPlay(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<VideoOptionModel> getOptionModelList() {
        return optionModelList;
    }

    /**
     * 设置IJK视频的option
     */
    public void setOptionModelList(List<VideoOptionModel> optionModelList) {
        this.optionModelList = optionModelList;
    }

    public boolean isNeedMute() {
        return needMute;
    }

    /**
     * 是否需要静音
     */
    public void setNeedMute(boolean needMute) {
        this.needMute = needMute;
        if (playerManager != null) {
            playerManager.setNeedMute(needMute);
        }
    }

    public int getTimeOut() {
        return timeOut;
    }

    public boolean isNeedTimeOutOther() {
        return needTimeOutOther;
    }

    /**
     * 是否需要在buffer缓冲时，增加外部超时判断
     * <p>
     * 超时后会走onError接口，播放器通过onPlayError回调出
     * <p>
     * 错误码为 ： BUFFER_TIME_OUT_ERROR = -192
     * <p>
     * 由于onError之后执行VideoPlayer的OnError，如果不想触发错误，
     * 可以重载onError，在super之前拦截处理。
     * <p>
     * public void onError(int what, int extra){
     * do you want before super and return;
     * super.onError(what, extra)
     * }
     *
     * @param timeOut          超时时间，毫秒 默认8000
     * @param needTimeOutOther 是否需要延时设置，默认关闭
     */
    public void setTimeOut(int timeOut, boolean needTimeOutOther) {
        this.timeOut = timeOut;
        this.needTimeOutOther = needTimeOutOther;
    }

    public IPlayerManager getCurPlayerManager() {
        return playerManager;
    }


    public IPlayerInitSuccessListener getPlayerPreparedSuccessListener() {
        return mPlayerInitSuccessListener;
    }

    /**
     * 播放器初始化后接口
     */
    public void setPlayerInitSuccessListener(IPlayerInitSuccessListener listener) {
        this.mPlayerInitSuccessListener = listener;
    }
}
