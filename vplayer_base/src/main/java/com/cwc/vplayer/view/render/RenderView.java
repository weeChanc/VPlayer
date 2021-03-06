package com.cwc.vplayer.view.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.cwc.vplayer.player.base.model.VideoType;
import com.cwc.vplayer.utils.MeasureHelper;
import com.cwc.vplayer.view.render.listener.VideoShotListener;
import com.cwc.vplayer.view.render.listener.VideoShotSaveListener;
import com.cwc.vplayer.view.render.listener.ISurfaceListener;
import com.cwc.vplayer.view.render.surface.IRenderView;
import com.cwc.vplayer.view.render.surface.VSurfaceView;
import com.cwc.vplayer.view.render.surface.VTextureView;

import java.io.File;

/**
 * render绘制中间控件
 * 具体Surface的代理，做surfaceView和Texture的差异，并提供工具方法
 */

public class RenderView {

    protected IRenderView mShowView;

    /*************************RenderView function start *************************/
    public void requestLayout() {
        if (mShowView != null) {
            mShowView.getRenderView().requestLayout();
        }
    }

    public float getRotation() {
        return mShowView.getRenderView().getRotation();
    }

    public void setRotation(float rotation) {
        if (mShowView != null)
            mShowView.getRenderView().setRotation(rotation);
    }

    public void invalidate() {
        if (mShowView != null)
            mShowView.getRenderView().invalidate();
    }

    public int getWidth() {
        return (mShowView != null) ? mShowView.getRenderView().getWidth() : 0;
    }

    public int getHeight() {
        return (mShowView != null) ? mShowView.getRenderView().getHeight() : 0;
    }

    public View getShowView() {
        if (mShowView != null)
            return mShowView.getRenderView();
        return null;
    }

    public ViewGroup.LayoutParams getLayoutParams() {
        return mShowView.getRenderView().getLayoutParams();
    }

    public void setLayoutParams(ViewGroup.LayoutParams layoutParams) {
        if (mShowView != null)
            mShowView.getRenderView().setLayoutParams(layoutParams);
    }

    /**
     * 添加播放的view
     */
    public void addView(final Context context, final ViewGroup textureViewContainer, final int rotate,
                        final ISurfaceListener surfaceListener,
                        final MeasureHelper.MeasureFormVideoParamsListener videoParamsListener) {
        if (VideoType.getRenderType() == VideoType.SUFRACE) {
            mShowView = VSurfaceView.addSurfaceView(context, textureViewContainer, rotate, surfaceListener, videoParamsListener);
        } else {
            mShowView = VTextureView.addTextureView(context, textureViewContainer, rotate, surfaceListener, videoParamsListener);
        }
    }

    /*************************RenderView function end *************************/

    /*************************ShowView function start *************************/

    /**
     * 主要针对TextureView，设置旋转
     */
    public void setTransform(Matrix transform) {
        if (mShowView != null)
            mShowView.setRenderTransform(transform);
    }

    /**
     * 暂停时初始化位图
     */
    public Bitmap initCover() {
        if (mShowView != null)
            return mShowView.initCover();
        return null;
    }

    /**
     * 暂停时初始化位图
     */
    public Bitmap initCoverHigh() {
        if (mShowView != null)
            return mShowView.initCoverHigh();
        return null;
    }

    /**
     * 获取截图
     */
    public void taskShotPic(VideoShotListener videoShotListener) {
        this.taskShotPic(videoShotListener, false);
    }


    /**
     * 获取截图
     *
     * @param shotHigh 是否需要高清的
     */
    public void taskShotPic(VideoShotListener videoShotListener, boolean shotHigh) {
        if (mShowView != null)
            mShowView.taskShotPic(videoShotListener, shotHigh);
    }

    /**
     * 保存截图
     */
    public void saveFrame(final File file, VideoShotSaveListener videoShotSaveListener) {
        saveFrame(file, false, videoShotSaveListener);
    }

    /**
     * 保存截图
     *
     * @param high 是否需要高清的
     */
    public void saveFrame(final File file, final boolean high, final VideoShotSaveListener videoShotSaveListener) {
        if (mShowView != null)
            mShowView.saveFrame(file, high, videoShotSaveListener);
    }


    /*************************ShowView function end *************************/


    /*************************common function *************************/

    public static void addToParent(ViewGroup textureViewContainer, View render) {
        int params = getTextureParams();
        if (textureViewContainer instanceof RelativeLayout) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(params, params);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            textureViewContainer.addView(render, layoutParams);
        } else if (textureViewContainer instanceof FrameLayout) {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(params, params);
            layoutParams.gravity = Gravity.CENTER;
            textureViewContainer.addView(render, layoutParams);
        }
    }

    /**
     * 获取布局参数
     *
     * @return
     */
    public static int getTextureParams() {
        boolean typeChanged = (VideoType.getShowType() != VideoType.SCREEN_TYPE_DEFAULT);
        return (typeChanged) ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT;
    }

}
