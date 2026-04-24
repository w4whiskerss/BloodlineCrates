package com.bloodline.crates.broadcast;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BroadcastTemplate {
    private BroadcastTrigger trigger;
    private List<String> chatLines = new ArrayList<>();
    private String titleText;
    private String subtitleText;
    private int titleFadeIn;
    private int titleStay;
    private int titleFadeOut;
    private boolean sendToDiscord;
    private String soundOnBroadcast;
}
