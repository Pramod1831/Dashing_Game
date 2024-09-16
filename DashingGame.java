import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.Timer;
import javax.swing.*;
import java.util.*;
import javax.sound.sampled.*;

public class DashingGame extends JPanel implements ActionListener, KeyListener {
    int boardWidth = 360;
    int boardHeight = 640;
    int pipeHeight = 400; // Set this to your desired pipe height

    private static final String HIGHSCORE_FILE = "highscore.txt";

    private Clip backgroundClip;

    Image backgroundImage;
    Image topPipeImage;
    Image bottomPipeImage;
    Image squareImage;

    // Square settings
    int squareX = boardWidth / 8;
    int squareY = boardHeight / 16;
    int squareWidth = 40;
    int squareHeight = 40;

    int topWallY = 35;  // Top boundary
    int bottomWallY = boardHeight - 40;  // Bottom boundary
    int pipeGap = 150;  // Gap between the top and bottom pipes
    int pipeGapDistance = 200; // Adjust this value to change the horizontal gap

    boolean gamePaused = false;
    

    // Square class
    class Square {
        int x = squareX;
        int y = squareY;
        int width = squareWidth;
        int height = squareHeight;
        Image img;

        Square(Image img) {
            this.img = img;
        }

        Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    // Pipe class
    class Pipe {
        int x;
        int y;
        int width;
        int height;
        Image img;
        boolean passed = false; // Add this flag to detect if pipe has been passed

        Pipe(int x, int y, int width, int height, Image img) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.img = img;
        }

        Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    int score = 0;
    boolean gameOver = false;

    Square square;
    int velocityY = 2; // Default downward movement
    int velocityX = -4; // Pipes move left
    int fallStrength = 999;
    int jumpStrength = -999;

    int highScore ;

    ArrayList<Pipe> pipes;
    Timer gameLoop;
    Timer speedIncreaseTimer;

    DashingGame() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        addKeyListener(this);

        preparesounds();

        // Load images
        backgroundImage = new ImageIcon(getClass().getResource("/Background.png")).getImage();
        topPipeImage = new ImageIcon(getClass().getResource("/TopPipe.png")).getImage();
        bottomPipeImage = new ImageIcon(getClass().getResource("/BottomPipe.png")).getImage();
        squareImage = new ImageIcon(getClass().getResource("/SquareImg.png")).getImage();

        square = new Square(squareImage);
        pipes = new ArrayList<>();

        // Start game loop
        gameLoop = new Timer(1000 / 60, this);
        gameLoop.start();

        backgroundMusic();
        loadHighScore();

        // Start speed increase timer, triggers every 30 seconds (30000 milliseconds)
        speedIncreaseTimer = new Timer(30000, e -> increaseSpeed());
        speedIncreaseTimer.start();
    }

    public void preparesounds(){
        backgroundClip = loadClip("./background.wav");
    }

    private void loadHighScore() {
        try (BufferedReader reader = new BufferedReader(new FileReader(HIGHSCORE_FILE))) {
            String line = reader.readLine();
            if (line != null && !line.isEmpty()) {
                highScore = Integer.parseInt(line);
                System.out.println("High score loaded: " + highScore);
            } else {
                System.out.println("No high score found in the file.");
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading high score: " + e.getMessage());
        }
    }

    private Clip loadClip(String string) {
        try {
            InputStream audioSrc = getClass().getResourceAsStream(string);
            InputStream bufferedIn = new BufferedInputStream(audioSrc);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(bufferedIn);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            return clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void backgroundMusic(){
        if (backgroundClip != null) {
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    // Draw background, square, pipes, and score
    private void draw(Graphics g) {
        g.drawImage(backgroundImage, 0, 0, boardWidth, boardHeight, null);
        g.drawImage(square.img, square.x, square.y, square.width, square.height, null);

        for (Pipe pipe : pipes) {
            g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
        }

        // Display the score below the top wall boundary
        g.setColor(Color.BLACK);
        g.setFont(new Font("Garamond", Font.BOLD, 25));
        g.drawString("" + score, 10,  21);
        //highscore
        g.drawString("HighScore: " + highScore, 190, 21) ;

        if (gamePaused) {
            g.setColor(Color.BLUE);
            g.setFont(new Font("Garamond", Font.BOLD, 40));
            g.drawString("Paused", boardWidth / 4, boardHeight / 2);
        }

        // If game is over, display "Game Over" in the middle of the screen
        if (gameOver) {
            backgroundClip.stop();
            g.setColor(Color.RED);
            g.setFont(new Font("Garamond", Font.BOLD, 40));
            g.drawString("Game Over", boardWidth / 4, boardHeight / 2);
        }
       
    }

    // Place pipes with a gap for the player to move through
    public void placePipes() {
        boolean placeAtTop = Math.random() < 0.5;
        @SuppressWarnings("unused")
        int pipeOffset = (int) (Math.random() * 100);

        if (placeAtTop) {
            Pipe topPipe = new Pipe(boardWidth, topWallY, 64, pipeHeight, topPipeImage);
            pipes.add(topPipe);
        } else {
            Pipe bottomPipe = new Pipe(boardWidth, bottomWallY - pipeHeight, 64, pipeHeight, bottomPipeImage);
            pipes.add(bottomPipe);
        }
    }

    private void move() {
         // Move the square based on the current velocity
        square.y += velocityY;

        // Ensure the square doesn't go out of bounds
        if (square.y <= topWallY) {
            square.y = topWallY;
        }
        if (square.y >= bottomWallY - squareHeight) {
            square.y = bottomWallY - squareHeight;
        }

        // Move the pipes
        for (Pipe pipe : pipes) {
            pipe.x += velocityX;

            // Check if the square has passed the pipe (without collision)
            if (!pipe.passed && square.x > pipe.x + pipe.width) {
                pipe.passed = true; // Mark pipe as passed
                score++; // Increment the score
            }
        }

        // **New Logic to place pipes based on distance**
        // If the last pipe is far enough to the left, place a new set of pipes
        if (pipes.isEmpty() || pipes.get(pipes.size() - 1).x < boardWidth - pipeGapDistance) {
            placePipes();
        }

        checkCollision();

        // Remove pipes that move out of the screen
        pipes.removeIf(pipe -> pipe.x + pipe.width < 0);
    }

    private void checkCollision() {
        for (Pipe pipe : pipes) {
            if (pipe.getBounds().intersects(square.getBounds())) {
                gameOver = true;  // Set gameOver to true if a collision occurs
                gameLoop.stop();  // Stop the game loop
                // placePipesTimer.stop();  // Stop placing pipes
                break;
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            move(); // Move the square and pipes
            repaint(); // Redraw the screen
        } else {
            gameLoop.stop();
            //placePipesTimer.stop();
            System.out.println("Game Over");
        }
        if(gameOver && score > highScore){
            highScore = (int)score;
            saveHighScore();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            // When space is pressed, reverse the direction of movement
            if (velocityY > 0) {
                velocityY = jumpStrength;  // Move upwards
            } else {
                velocityY = fallStrength;  // Move downwards
            }

            if(gameOver){
                //restart the game by reseting the positions
                restartGame();
            }
        }

        if( e.getKeyCode() == KeyEvent.VK_ESCAPE){
            if (gamePaused) {
                resumeGame();  // Resume if the game is paused
            } else {
                pauseGame();   // Pause if the game is running
            }
        }
    }

    // Method to increase speed after each 30-second interval
    private void increaseSpeed() {
        if (!gameOver) {
            velocityX -= 1; // Increase the leftward speed of pipes
            fallStrength += 50; // Increase the falling speed
            System.out.println("Speed increased: velocityX = " + velocityX + ", fallStrength = " + fallStrength);
        }
    }

    public void resumeGame() {
        gamePaused = false;
        speedIncreaseTimer.start();
        gameLoop.start();
        // placePipesTimer.start();
        backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
        repaint();  // Resume the game visuals
    }

    public void pauseGame() {
        gamePaused = true;
        speedIncreaseTimer.stop();
        gameLoop.stop();
        // placePipesTimer.stop();
        backgroundClip.stop();
        repaint();  // Ensure the "Paused" message is displayed
    }

    public void restartGame(){
         // Reset square position and velocity
    square.y = squareY;
    velocityY = 2;
    pipes.clear();
    score = 0;
    gameOver = false;

    // Reset game-specific values like velocityX and fallStrength
    velocityX = -4;  // Reset pipe speed
    fallStrength = 999;  // Reset falling speed

    // Completely stop and nullify the old timer to ensure it doesn't continue
    if (speedIncreaseTimer != null) {
        speedIncreaseTimer.stop();
        speedIncreaseTimer = null;  // Remove any existing reference
    }

    // Reinitialize the timer to start from 0 again
    speedIncreaseTimer = new Timer(30000, e -> increaseSpeed());  // Fresh new timer
    speedIncreaseTimer.setRepeats(true);  // Ensure it repeats every 30 seconds
    speedIncreaseTimer.start();

    // Restart the game loop
    gameLoop.start();

    // Reset the background music
    if (backgroundClip != null) {
        backgroundClip.setFramePosition(0);  // Rewind the music
        backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);  // Start looping music again
    }
    } 

    private void saveHighScore() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(HIGHSCORE_FILE))) {
            writer.write(String.valueOf(highScore));
            System.out.println("High score saved: " + highScore);
        } catch (IOException e) {
            System.err.println("Error saving high score: " + e.getMessage());
        }
    }


    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}
}
