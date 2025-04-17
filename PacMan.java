import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Random;
import javax.swing.*;
import javax.sound.sampled.*;
import java.io.*;

public class PacMan extends JPanel implements ActionListener, KeyListener {
    class Block {
        int x;
        int y;
        int width;
        int height;
        Image image;

        int startX;
        int startY;
        char direction = 'U'; // U D L R
        int velocityX = 0;
        int velocityY = 0;

        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char direction) {
            char prevDirection = this.direction;
            this.direction = direction;
            updateVelocity();
            this.x += this.velocityX;
            this.y += this.velocityY;
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    this.direction = prevDirection;
                    updateVelocity();
                }
            }
        }

        void updateVelocity() {
            if (this.direction == 'U') {
                this.velocityX = 0;
                this.velocityY = -tileSize / 4;
            } else if (this.direction == 'D') {
                this.velocityX = 0;
                this.velocityY = tileSize / 4;
            } else if (this.direction == 'L') {
                this.velocityX = -tileSize / 4;
                this.velocityY = 0;
            } else if (this.direction == 'R') {
                this.velocityX = tileSize / 4;
                this.velocityY = 0;
            }
        }

        void reset() {
            this.x = this.startX;
            this.y = this.startY;
        }
    }

    private int rowCount = 21;
    private int columnCount = 19;
    private int tileSize = 32;
    private int boardWidth = columnCount * tileSize;
    private int boardHeight = rowCount * tileSize;

    private Image wallImage;
    private Image blueGhostImage;
    private Image orangeGhostImage;
    private Image pinkGhostImage;
    private Image redGhostImage;

    private Image pacmanUpImage;
    private Image pacmanDownImage;
    private Image pacmanLeftImage;
    private Image pacmanRightImage;

    private boolean gameStarted = false;
    private boolean gameCompleted = false; // Flag to track game completion
    private JButton playButton;

    private Clip backgroundClip;
    private Clip deathClip;
    private Clip pelletClip;
    private Clip eatGhostClip;

    //X = wall, O = skip, P = pac man, ' ' = food
    //Ghosts: b = blue, o = orange, p = pink, r = red
    private String[] tileMap = {
        "XXXXXXXXXXXXXXXXXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X                 X",
        "X XX X XXXXX X XX X",
        "X    X       X    X",
        "XXXX XXXX XXXX XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXrXX X XXXX",
        "O       bpo       O",
        "XXXX X XXXXX X XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXXXX X XXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X  X     P     X  X",
        "XX X X XXXXX X X XX",
        "X    X   X   X    X",
        "X XXXXXX X XXXXXX X",
        "X                 X",
        "XXXXXXXXXXXXXXXXXXX" 
    };

    HashSet<Block> walls;
    HashSet<Block> foods;
    HashSet<Block> ghosts;
    Block pacman;

    Timer gameLoop;
    char[] directions = {'U', 'D', 'L', 'R'}; //up down left right
    Random random = new Random();
    int score = 0;
    int lives = 3;
    boolean gameOver = false;

    PacMan() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        setLayout(null); // Use null layout to position the button manually
        addKeyListener(this);
        setFocusable(true);

        // Initialize play button
        playButton = new JButton("Play");
        playButton.setBounds(boardWidth / 2 - 50, boardHeight - 75, 100, 50); // Move to bottom
        playButton.addActionListener(e -> startGame());
        add(playButton);

        // Load images
        wallImage = new ImageIcon(getClass().getResource("./wall.png")).getImage();
        blueGhostImage = new ImageIcon(getClass().getResource("./blueGhost.png")).getImage();
        orangeGhostImage = new ImageIcon(getClass().getResource("./orangeGhost.png")).getImage();
        pinkGhostImage = new ImageIcon(getClass().getResource("./pinkGhost.png")).getImage();
        redGhostImage = new ImageIcon(getClass().getResource("./redGhost.png")).getImage();

        pacmanUpImage = new ImageIcon(getClass().getResource("./pacmanUp.png")).getImage();
        pacmanDownImage = new ImageIcon(getClass().getResource("./pacmanDown.png")).getImage();
        pacmanLeftImage = new ImageIcon(getClass().getResource("./pacmanLeft.png")).getImage();
        pacmanRightImage = new ImageIcon(getClass().getResource("./pacmanRight.png")).getImage();

        // Load sounds
        try {
            backgroundClip = AudioSystem.getClip();
            backgroundClip.open(AudioSystem.getAudioInputStream(new File("pac_sound.mp4")));
            deathClip = AudioSystem.getClip();
            deathClip.open(AudioSystem.getAudioInputStream(new File("pac_death.mp4")));
            pelletClip = AudioSystem.getClip();
            pelletClip.open(AudioSystem.getAudioInputStream(new File("pac_chop.mp4")));
            eatGhostClip = AudioSystem.getClip();
            eatGhostClip.open(AudioSystem.getAudioInputStream(new File("pac_eat.mp4")));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Play background sound
        playBackgroundSound();

        loadMap();
        for (Block ghost : ghosts) {
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
        // How long it takes to start timer, milliseconds gone between frames
        gameLoop = new Timer(50, this); // 20fps (1000/50)
    }

    private void startGame() {
        gameStarted = true;
        playButton.setVisible(false);
        pauseBackgroundSound(); // Stop the background sound
        requestFocusInWindow(); // Ensure the game panel gets focus for key events
        gameLoop.start();
    }

    private void playBackgroundSound() {
        if (backgroundClip != null) {
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY); // Loop the background sound
            backgroundClip.start();
        }
    }

    private void playDeathSound() {
        if (deathClip != null) {
            deathClip.setFramePosition(0); // Reset to the beginning
            deathClip.start();
        }
    }

    private void playPelletSound() {
        if (pelletClip != null) {
            pelletClip.setFramePosition(0); // Reset to the beginning
            pelletClip.start();
        }
    }

    private void pauseBackgroundSound() {
        if (backgroundClip != null && backgroundClip.isRunning()) {
            backgroundClip.stop();
        }
    }

    public void loadMap() {
        walls = new HashSet<Block>();
        foods = new HashSet<Block>();
        ghosts = new HashSet<Block>();

        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                String row = tileMap[r];
                char tileMapChar = row.charAt(c);

                int x = c * tileSize;
                int y = r * tileSize;

                if (tileMapChar == 'X') { //block wall
                    Block wall = new Block(wallImage, x, y, tileSize, tileSize);
                    walls.add(wall);
                } else if (tileMapChar == 'b') { //blue ghost
                    Block ghost = new Block(blueGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                } else if (tileMapChar == 'o') { //orange ghost
                    Block ghost = new Block(blueGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                } else if (tileMapChar == 'o') { //orange ghost
                    Block ghost = new Block(orangeGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                } else if (tileMapChar == 'p') { //pink ghost
                    Block ghost = new Block(pinkGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                } else if (tileMapChar == 'r') { //red ghost
                    Block ghost = new Block(redGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                } else if (tileMapChar == 'P') { //pacman
                    pacman = new Block(pacmanRightImage, x, y, tileSize, tileSize);
                } else if (tileMapChar == ' ') { //food
                    Block food = new Block(null, x + 14, y + 14, 4, 4);
                    foods.add(food);
                }
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!gameStarted) {
            drawStartScreen(g);
        } else if (gameOver) {
            drawRetryScreen(g);
        } else if (gameCompleted) {
            drawCompletionScreen(g);
        } else {
            draw(g);
        }
    }

    private void drawStartScreen(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("PAC-MAN", boardWidth / 2 - 60, boardHeight / 2 - 100);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("Click 'Play' to Start", boardWidth / 2 - 80, boardHeight / 2 - 70);

        // Add names
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.setColor(Color.WHITE);
        g.drawRect(boardWidth / 2 - 130, boardHeight / 2 - 60, 300, 70); // Draw the box
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("JACOB ASHIRWAD M P URK23EC6039", boardWidth / 2 - 120, boardHeight / 2 - 40);
        g.drawString("ABEN M PHILIP URK23EC6001", boardWidth / 2 - 120, boardHeight / 2 - 20);
        g.drawString("JEFRIS SAJU DANI URK23EC6012", boardWidth / 2 - 120, boardHeight / 2);
        g.setFont(new Font("Arial", Font.BOLD, 14)); // Set bold font
        g.drawString("", boardWidth / 2 - 120, boardHeight / 2 + 10); // Add a gap]
        g.drawString("", boardWidth / 2 - 120, boardHeight / 2 + 10); // Add a gap
        g.drawString("Escape ghosts and eat all pellets to win!", boardWidth / 2 - 120, boardHeight / 2 + 20);
    }
    
    private void drawRetryScreen(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Game Over!", boardWidth / 2 - 70, boardHeight / 2 - 100);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("Score: " + score, boardWidth / 2 - 40, boardHeight / 2 - 70);
        g.drawString("Press 'R' to Retry", boardWidth / 2 - 80, boardHeight / 2 - 40);

        playBackgroundSound(); // Ensure background sound plays until 'R' is pressed
    }

    private void drawCompletionScreen(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Game Completed!", boardWidth / 2 - 100, boardHeight / 2 - 50);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("Final Score: " + score, boardWidth / 2 - 60, boardHeight / 2 - 20);
        g.drawString("Press 'R' to Restart", boardWidth / 2 - 80, boardHeight / 2 + 20);
    }

    public void draw(Graphics g) {
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);

        for (Block ghost : ghosts) {
            g.drawImage(ghost.image, ghost.x, ghost.y, ghost.width, ghost.height, null);
        }

        for (Block wall : walls) {
            g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        }

        g.setColor(Color.WHITE);
        for (Block food : foods) {
            g.fillRect(food.x, food.y, food.width, food.height);
        }
        //score
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        if (gameOver) {
            g.drawString("Game Over: " + String.valueOf(score), tileSize / 2, tileSize / 2);
        } else {
            g.drawString("x" + String.valueOf(lives) + " Score: " + String.valueOf(score), tileSize / 2, tileSize / 2);
        }
    }

    public void move() {
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;

        // Check wall collisions
        for (Block wall : walls) {
            if (collision(pacman, wall)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
                break;
            }
        }

        // Check ghost collisions
        for (Block ghost : ghosts) {
            if (collision(ghost, pacman)) {
                lives -= 1;
                playDeathSound(); // Play death sound
                if (lives == 0) {
                    gameOver = true;
                    return;
                }
                resetPositions();
            }

            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;

            // Check wall collisions for ghosts
            for (Block wall : walls) {
                if (collision(ghost, wall) || ghost.x <= 0 || ghost.x + ghost.width >= boardWidth || ghost.y <= 0 || ghost.y + ghost.height >= boardHeight) {
                    ghost.x -= ghost.velocityX;
                    ghost.y -= ghost.velocityY;
                    char newDirection = directions[random.nextInt(4)];
                    ghost.updateDirection(newDirection);
                    break;
                }
            }

            // Randomly change ghost direction periodically
            if (random.nextInt(20) == 0) { // Adjust frequency as needed
                char newDirection = directions[random.nextInt(4)];
                ghost.updateDirection(newDirection);
            }
        }

        // Check food collision
        Block foodEaten = null;
        for (Block food : foods) {
            if (collision(pacman, food)) {
                foodEaten = food;
                score += 10;
                playPelletSound(); // Play sound for eating a pellet
            }
        }
        foods.remove(foodEaten);

        // Check if all pellets are eaten
        if (foods.isEmpty() && !gameCompleted) {
            gameCompleted = true;
            gameLoop.stop(); // Stop the game loop
        }
    }

    public boolean collision(Block a, Block b) {
        return a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y;
    }

    public void resetPositions() {
        pacman.reset();
        pacman.velocityX = 0;
        pacman.velocityY = 0;
        for (Block ghost : ghosts) {
            ghost.reset();
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameStarted) {
            move();
            repaint();
            if (gameOver) {
                gameLoop.stop();
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if ((gameOver || gameCompleted) && e.getKeyCode() == KeyEvent.VK_R) {
            // Reset game state for retry or restart
            loadMap();
            resetPositions();
            lives = 3;
            score = 0;
            gameOver = false;
            gameCompleted = false;
            gameLoop.start();
            pauseBackgroundSound(); // Stop the background sound after retry
            return;
        }

        if (gameOver || gameCompleted) {
            return; // Do nothing if the game is over or completed
        }

        // Handle direction changes using W, A, S, D keys
        if (e.getKeyCode() == KeyEvent.VK_W) {
            pacman.updateDirection('U');
            pacman.image = pacmanUpImage; // Update Pac-Man's image
        } else if (e.getKeyCode() == KeyEvent.VK_S) {
            pacman.updateDirection('D');
            pacman.image = pacmanDownImage; // Update Pac-Man's image
        } else if (e.getKeyCode() == KeyEvent.VK_A) {
            pacman.updateDirection('L');
            pacman.image = pacmanLeftImage; // Update Pac-Man's image
        } else if (e.getKeyCode() == KeyEvent.VK_D) {
            pacman.updateDirection('R');
            pacman.image = pacmanRightImage; // Update Pac-Man's image
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Handle direction changes and completely change trajectory
        if (e.getKeyCode() == KeyEvent.VK_W) {
            pacman.updateDirection('U');
        } else if (e.getKeyCode() == KeyEvent.VK_S) {
            pacman.updateDirection('D');
        } else if (e.getKeyCode() == KeyEvent.VK_A) {
            pacman.updateDirection('L');
        } else if (e.getKeyCode() == KeyEvent.VK_D) {
            pacman.updateDirection('R');
        }

        // Update Pac-Man's image based on the new direction
        if (pacman.direction == 'U') {
            pacman.image = pacmanUpImage;
        } else if (pacman.direction == 'D') {
            pacman.image = pacmanDownImage;
        } else if (pacman.direction == 'L') {
            pacman.image = pacmanLeftImage;
        } else if (pacman.direction == 'R') {
            pacman.image = pacmanRightImage;
        }
    }
}

