import java.awt.*;
import javax.swing.*;

public class Flip7 extends JFrame {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 780;

    public Flip7(String framename) {
        super(framename);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        add(new Flip7Panel());
        setVisible(true);
    }
}

