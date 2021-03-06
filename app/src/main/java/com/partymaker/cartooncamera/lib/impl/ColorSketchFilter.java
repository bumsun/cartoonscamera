package com.partymaker.cartooncamera.lib.impl;

import com.partymaker.cartooncamera.lib.Filter;
import com.partymaker.cartooncamera.lib.FilterType;
import com.partymaker.cartooncamera.lib.Native;

import org.opencv.core.Mat;

/**
 * Color-Sketch filter implementation
 */
public class ColorSketchFilter extends Filter {

    public ColorSketchFilter(FilterType filterType) {
        super(filterType);

        mFilterConfigs.add(new FilterConfig("Blend"));
        mFilterConfigs.add(new FilterConfig("Scale"));
        mDefaultScaleFactor = 0.8;

        resetConfig();
    }

    @Override
    public void process(Mat src, Mat dst) {
        Native.colorSketchFilter(src.getNativeObjAddr(), dst.getNativeObjAddr(), mFilterConfigs.get(0).value, mFilterConfigs.get(1).value);
    }

    @Override
    public void resetConfig() {
        mFilterConfigs.get(0).value = 70;
        mFilterConfigs.get(1).value = 40;
    }
}
