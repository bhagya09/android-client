package com.bsb.hike.bots;

import com.bsb.hike.utils.CustomAnnotation.DoNotObfuscate;

/**
 * Created by konarkarora on 01/05/16.
 */
@DoNotObfuscate
public class Sk {

    private String catId;
    private String stkrId;

    /**
     *
     * @return
     * The catId
     */
    public String getCatId() {
        return catId;
    }

    /**
     *
     * @param catId
     * The catId
     */
    public void setCatId(String catId) {
        this.catId = catId;
    }

    /**
     *
     * @return
     * The stkrId
     */
    public String getStkrId() {
        return stkrId;
    }

    /**
     *
     * @param stkrId
     * The stkrId
     */
    public void setStkrId(String stkrId) {
        this.stkrId = stkrId;
    }

}
