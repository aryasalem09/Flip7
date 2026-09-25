import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import javax.swing.JPanel;

public class Flip7Panel extends JPanel implements MouseListener {
    private Flip7Game game = new Flip7Game();
    private CardImages cardImages = new CardImages();

    public Flip7Panel() {
        setBackground(new Color(105, 20, 35));
        addMouseListener(this);
    }

    public void paint(Graphics g) {
        super.paint(g);
        int center = getWidth() / 2;
        g.setColor(new Color(105, 20, 35));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        drawCentered(g, "Dealer: " + game.getPlayer(game.getDealer()).name + "     Current: "
                + game.getPlayer(game.getCurrentPlayer()).name, 0, getWidth(), 40);

        g.setColor(new Color(191, 152, 55));
        g.fillOval(center - 385, 115, 770, 550);
        g.setColor(new Color(72, 10, 28));
        g.fillOval(center - 375, 125, 750, 530);

        for (int i = 0; i < game.getPlayerCount(); i++)
            drawPlayer(g, game.getPlayer(i), i, playerX(i), playerY(i), 280, 140);

        g.setColor(new Color(255, 238, 220));
        g.fillRect(center - 270, 240, 540, 64);
        g.setColor(Color.BLACK);
        drawMessage(g);

        int drawX = center - 140;
        int discardX = center + 65;
        drawDrawCard(g, drawX, 340);
        drawDiscardCard(g, discardX, 340);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        drawCentered(g, "Draw: " + game.getDrawDeckSize(), drawX, 75, 462);
        drawCentered(g, "Discard: " + game.getDiscardDeckSize(), discardX, 75, 462);
        drawCentered(g, "Round discard: " + game.getRoundDiscardDeckSize(), 0, getWidth(), 487);
        drawCentered(g, "Turn: " + game.getPlayer(game.getCurrentPlayer()).name, 0, getWidth(), 509);

        String hitLabel = "HIT";
        if (game.isFlippingThree()) {
            hitLabel = "FLIP " + (4 - game.getFlipThreeRemaining()) + "/3";
            g.setColor(Color.YELLOW);
            drawCentered(g, game.getPlayer(game.getFlipThreeTarget()).name + ": click FLIP for next card",
                    0, getWidth(), 545);
        }
        drawButton(g, buttonX(0), 570, 120, 45, hitLabel, !game.isRoundOver() && !game.isWaitingForTarget());
        drawButton(g, buttonX(1), 570, 120, 45, "STAY", !game.isRoundOver() && !game.isWaitingForTarget() && !game.isFlippingThree());
        drawButton(g, buttonX(2), 570, 120, 45, "NEXT ROUND", game.isRoundOver() && !game.isGameOver());
        if (game.isWaitingForTarget()) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 13));
            drawCentered(g, "Choose a TARGET player", 0, getWidth(), 545);
        }
    }

    // center font, fix 
    private void drawCentered(Graphics g, String text, int x, int width, int baseline) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, x + (width - textWidth) / 2, baseline);
    }

    private void drawMessage(Graphics g) {
        String message = game.getMessage();
        if (message == null || message.length() == 0)
            return;
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        ArrayList<String> lines = wrapText(message, g, 514);
        if (lines.size() > 3) {
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            lines = wrapText(message, g, 514);
        }
        int lineHeight = g.getFontMetrics().getHeight();
        int startY = 240 + (64 - lines.size() * lineHeight) / 2 + g.getFontMetrics().getAscent();
        for (int i = 0; i < lines.size(); i++)
            drawCentered(g, lines.get(i), getWidth() / 2 - 270, 540, startY + i * lineHeight);
    }

    private ArrayList<String> wrapText(String text, Graphics g, int maxWidth) {
        ArrayList<String> lines = new ArrayList<String>();
        while (g.getFontMetrics().stringWidth(text) > maxWidth) {
            int end = text.length();
            while (end > 1 && g.getFontMetrics().stringWidth(text.substring(0, end)) > maxWidth)
                end--;
            int space = end;
            while (space > 0 && !text.substring(space, space + 1).equals(" "))
                space--;
            if (space > 0)
                end = space;
            lines.add(text.substring(0, end));
            text = text.substring(end);
            while (text.length() > 0 && text.substring(0, 1).equals(" "))
                text = text.substring(1);
        }
        if (text.length() > 0)
            lines.add(text);
        return lines;
    }

    private int playerX(int index) {
        if (index == 0) return getWidth() / 2 - 140;
        if (index == 1 || index == 2) return getWidth() - 305;
        return 25;
    }

    private int playerY(int index) {
        if (index == 0) return 95;
        if (index == 1 || index == 4) return 185;
        return 510;
    }

    private int buttonX(int index) {
        return getWidth() / 2 - 195 + index * 135;
    }
    private void drawDrawCard(Graphics g, int x, int y) {
        BufferedImage image = cardImages.getBack();
        if (image != null) {
            g.drawImage(image, x, y, 75, 105, null);
        } else {
            g.setColor(new Color(105, 20, 35));
            g.fillRect(x, y, 75, 105);
            g.setColor(new Color(191, 152, 55));
            g.drawRect(x, y, 75, 105);
            g.drawRect(x + 3, y + 3, 69, 99);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            drawCentered(g, "FLIP", x, 75, y + 43);
            drawCentered(g, "7", x, 75, y + 66);
        }
    }

    private void drawDiscardCard(Graphics g, int x, int y) {
        BufferedImage image = null;
        Card topDiscard = game.getTopDiscard();
        if (topDiscard != null)
            image = cardImages.getImage(topDiscard);
        if (image != null) {
            g.drawImage(image, x, y, 75, 105, null);
        } else {
            g.setColor(new Color(245, 225, 210));
            g.fillRect(x, y, 75, 105);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, 75, 105);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            drawCentered(g, "DISCARD", x, 75, y + (105 - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent());
        }
        g.setColor(Color.BLACK);
        g.drawRect(x, y, 75, 105);
    }

    private void drawPlayer(Graphics g, Player player, int index, int x, int y, int width, int height) {
        Color rowColor = new Color(245, 225, 210);
        if (game.isWaitingForTarget() && game.isEligibleTarget(index))
            rowColor = new Color(190, 225, 185); // light green
        if (player.stayed)
            rowColor = new Color(190, 190, 190); // grey
        if (player.frozen)
            rowColor = new Color(190, 225, 245); // light blue
        if (player.busted)
            rowColor = new Color(220, 120, 120); // light red
        g.setColor(rowColor);
        g.fillRect(x, y, width, height);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, width, height);
        if (game.isWaitingForTarget() && game.isEligibleTarget(index)) {
            g.setColor(new Color(35, 105, 45));
            g.drawRect(x + 2, y + 2, width - 4, height - 4);
        }
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        drawCentered(g, player.name + "  Total: " + player.score, x, width, y + 20);
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        int roundScore = player.roundScore();
        if (player.busted)
            roundScore = 0;

        String status = "Playing";
        if (player.stayed)
            status = "Stayed";
        if (player.frozen)
            status = "Frozen";
        if (player.busted)
            status = "Busted";

        if (game.isWaitingForTarget() && game.isEligibleTarget(index)) {
            g.setColor(new Color(25, 110, 50));
            g.setFont(new Font("Arial", Font.BOLD, 12));
            status = "TARGET";
        }
        drawCentered(g, "Round: " + roundScore + "     " + status, x, width, y + 39);
        drawHand(g, player, x + 8, y + 51, width - 16);
        if (index == game.getCurrentPlayer() && !game.isRoundOver()) {
            boolean pointsRight = index == 1 || index == 2;
            int arrowX = x + width + 8;
            if (pointsRight)
                arrowX = x - 80;
            drawTurnArrow(g, arrowX, y + 4, pointsRight);
        }
    }

    private void drawTurnArrow(Graphics g, int x, int y, boolean pointsRight) {
        int[] xs = { 0, 38, 34, 72, 72, 34, 38 };
        int[] ys = { 22, 0, 14, 10, 34, 30, 44 };
        for (int i = 0; i < xs.length; i++) {
            if (pointsRight)
                xs[i] = 72 - xs[i];
            xs[i] = xs[i] + x;
            ys[i] = ys[i] + y;
        }
        g.setColor(Color.RED);
        g.fillPolygon(xs, ys, xs.length);
        g.drawPolygon(xs, ys, xs.length);
    }
    private void drawHand(Graphics g, Player player, int x, int y, int width) {
        if (player.stayed) {
            int numberOfCards = player.hand.size();
            if (numberOfCards == 0)
                return;
            int uprightCount = numberOfCards - 1;
            if (uprightCount > 17)
                uprightCount = 17;
            for (int row = 0; row < 2; row++) {
                int firstCard = row * 9;
                if (firstCard >= uprightCount)
                    break;
                int cardsInRow = uprightCount - firstCard;
                if (cardsInRow > 9)
                    cardsInRow = 9;
                int rowWidth = 24 + (cardsInRow - 1) * 27;
                int rowX = x + (width - rowWidth) / 2;
                for (int column = 0; column < cardsInRow; column++) {
                    drawSmallCard(g, player.hand.get(firstCard + column), rowX + column * 27, y + 20 + row * 28, 24,
                            34);
                }
            }
            int lastCard = numberOfCards - 1;
            // rot card
            java.awt.Graphics2D sideways = (java.awt.Graphics2D) g.create();
            sideways.translate(x + (width - 34) / 2, y + 24);
            sideways.rotate(-Math.PI / 2);
            drawSmallCard(sideways, player.hand.get(lastCard), 0, 0, 24, 34);
            sideways.dispose();
            return;
        }
        int numberOfCards = player.hand.size();
        for (int i = 0; i < numberOfCards; i++) {
            int column = i % 9;
            int row = i / 9;
            if (row > 1)
                return;
            int cardsInRow = numberOfCards - row * 9;
            if (cardsInRow > 9)
                cardsInRow = 9;
            int rowWidth = 30 + (cardsInRow - 1) * 27;
            int rowX = x + (width - rowWidth) / 2;
            drawSmallCard(g, player.hand.get(i), rowX + column * 27, y + row * 40, 30, 42);
        }
    }

    private void drawSmallCard(Graphics g, Card card, int x, int y, int width, int height) {
        BufferedImage image = cardImages.getImage(card);
        if (image != null) {
            g.drawImage(image, x, y, width, height, null);
        } else {
            g.setColor(new Color(155, 30, 40));
            g.fillRect(x, y, width, height);
            g.setColor(Color.WHITE);
            g.drawRect(x, y, width, height);
            g.setFont(new Font("Arial", Font.BOLD, 9));
            drawCentered(g, shortName(card), x, width, y + (height - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent());
        }
    }

    private String shortName(Card card) {
        if (card.name.equals("Freeze"))
            return "FR";
        if (card.name.equals("Flip Three"))
            return "F3";
        if (card.name.equals("Second Chance"))
            return "SC";
        return card.name;
    }

    private void drawButton(Graphics g, int x, int y, int width, int height, String words, boolean enabled) {
        if (enabled)
            g.setColor(new Color(240, 205, 145));
        else
            g.setColor(Color.GRAY);
        g.fillRect(x, y, width, height);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, width, height);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        drawCentered(g, words, x, width, y + (height - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent());
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1)
            return;
        int x = e.getX();
        int y = e.getY();
        if (game.isWaitingForTarget()) {
            int target = playerAt(x, y);
            if (target >= 0) {
                game.chooseTarget(target);
                repaint();
            }
            return;
        }
        if (y >= 570 && y <= 615) {
            if (x >= buttonX(0) && x <= buttonX(0) + 120) {
                game.hit();
                repaint();
            } else if (x >= buttonX(1) && x <= buttonX(1) + 120) {
                game.stay();
                repaint();
            } else if (x >= buttonX(2) && x <= buttonX(2) + 120) {
                game.nextRound();
                repaint();
            }
        }
    }

    private int playerAt(int x, int y) {
        for (int i = 0; i < game.getPlayerCount(); i++) {
            if (x >= playerX(i) && x <= playerX(i) + 280 && y >= playerY(i) && y <= playerY(i) + 140)
                return i;
        }
        return -1;
    }
    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }
}







