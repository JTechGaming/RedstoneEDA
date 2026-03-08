package com.cybrisoft.redstoneeda.client.util;

import net.minecraft.util.Util;

import java.net.URI;
import java.net.URISyntaxException;

public class WebUtil {
    public static void openWebpage(String url) {
        try {
            Util.getOperatingSystem().open(new URI(url));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
