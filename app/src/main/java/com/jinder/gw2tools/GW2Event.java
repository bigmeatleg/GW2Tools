package com.jinder.gw2tools;

/**
 * Created by Jinder on 14/12/2.
 */
public class GW2Event {
    private String strTitle;
    private String strTimeStart;
    private String strZone;
    private String strArea;
    private String strLink;
    private String strTW;
    private String strLevel;
    private int     nId;
    private long    nTimeValue;
    private long    EndTime;

    public long getEndTime() {
        return EndTime;
    }

    public void setEndTime(long endTime) {

        EndTime = endTime;
    }

    public int getnId() {
        return nId;
    }

    public GW2Event(String strTitle, String strTimeStart, String strZone, String strArea, String strLink, String strTW, String strLevel, int nId) {
        this.strTitle = strTitle;
        this.strTimeStart = strTimeStart;
        this.strZone = strZone;
        this.strArea = strArea;
        this.strLink = strLink;
        this.strTW = strTW;
        this.strLevel = strLevel;
        this.nId = nId;
    }

    public String getStrTitle() {
        return strTitle;
    }

    public String getStrTimeStart() {
        return strTimeStart;
    }

    public String getStrZone() {
        return strZone;
    }

    public String getStrArea() {
        return strArea;
    }

    public String getStrLink() {
        return strLink;
    }

    public String getStrTW() {
        return strTW;
    }

    public String getStrLevel(){
        return strLevel;
    }

    public void setnTimeValue(long nTimeValue) {
        this.nTimeValue = nTimeValue;
    }

    public long getnTimeValue() {
        return nTimeValue;
    }
}
