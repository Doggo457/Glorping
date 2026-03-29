import utils.MapGenerator;
import utils.ResourceGenerator;

/**
 * Application entry point for the Glorping Cave Explorer game.
 *
 * Generates missing image, sound, and map assets before starting the game.
 * This ensures the game can run on a clean checkout without requiring
 * pre-built resource files.
 */
public class App {

    /**
     * Main method.
     *
     * @param args Command-line arguments (unused)
     */
    public static void main(String[] args) {
        // Generate procedural assets first so the game can load them
        ResourceGenerator.generate();
        MapGenerator.generate();

        // Launch the gamep
        GlorpingGame game = new GlorpingGame();
        game.init();
        game.run(false, GlorpingGame.SCREEN_W, GlorpingGame.SCREEN_H);
    }
}
