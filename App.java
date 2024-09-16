import javax.swing.*;

public class App {            
     public static void main(String[] args) throws Exception {
        int boardWidth = 360;
        int boardHeight = 640;


        JFrame frame = new JFrame("Dashing Game");
        
        frame.setSize(boardWidth, boardHeight);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        DashingGame ballGame = new DashingGame();
        frame.add(ballGame);
        frame.pack();
        ballGame.requestFocus(); 
        frame.setVisible(true);
    }
}
