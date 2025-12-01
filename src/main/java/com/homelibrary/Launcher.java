package com.homelibrary;

import com.homelibrary.ui.MainApp;

/**
 * Launcher class for JavaFX application.
 * This separate launcher is needed to properly bootstrap JavaFX when running from JAR.
 * The main class in the manifest should point to this Launcher, not to MainApp directly.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
