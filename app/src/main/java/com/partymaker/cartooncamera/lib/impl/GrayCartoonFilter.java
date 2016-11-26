package com.partymaker.cartooncamera.lib.impl;

import com.partymaker.cartooncamera.lib.Filter;
import com.partymaker.cartooncamera.lib.FilterType;
import com.partymaker.cartooncamera.lib.Native;

import org.opencv.core.Mat;

/**
 * Gray-Cartoon Filter Implementation
 */
public class GrayCartoonFilter extends Filter {

    public GrayCartoonFilter(FilterType filterType) {
        super(filterType);

        mFilterConfigs.add(new FilterConfig("Thickness"));
        mFilterConfigs.add(new FilterConfig("Threshold"));
        mDefaultScaleFactor = 0.6;

        resetConfig();
    }

    @Override
    public void process(Mat src, Mat dst) {
        Native.grayCartoonFilter(src.getNativeObjAddr(), dst.getNativeObjAddr(), mFilterConfigs.get(0).value, mFilterConfigs.get(1).value);
    }

    @Override
    public void resetConfig() {
        mFilterConfigs.get(0).value = 40;
        mFilterConfigs.get(1).value = 50;
    }
}
