package ui;

/**
 * Represents the overall state of the game at any given time.
 * Used to control which screen/logic is active.
 */
public enum GameState {
    /** Main menu screen */
    MENU,
    /** Active gameplay */
    PLAYING,
    /** Game paused */
    PAUSED,
    /** Player has died */
    DEAD,
    /** Player reached level exit */
    LEVEL_COMPLETE,
    /** All levels complete */
    WIN,
    /** Settings screen */
    SETTINGS,
    /** Shop screen between levels */
    SHOP
}
