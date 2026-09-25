import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.*;

public class CardImages {
    private BufferedImage[] numbers = new BufferedImage[13];
    private BufferedImage plus2;
    private BufferedImage plus4;
    private BufferedImage plus6;
    private BufferedImage plus8;
    private BufferedImage plus10;
    private BufferedImage timesTwo;
    private BufferedImage freeze;
    private BufferedImage flipThree;
    private BufferedImage secondChance;
    private BufferedImage back;
    private BufferedImage redArrow;

    public CardImages() {
        try {
            numbers[0] = ImageIO.read(CardImages.class.getResource("/Image/F7-0.png"));
            numbers[1] = ImageIO.read(CardImages.class.getResource("/Image/F7-1.png"));
            numbers[2] = ImageIO.read(CardImages.class.getResource("/Image/F7-2.png"));
            numbers[3] = ImageIO.read(CardImages.class.getResource("/Image/F7-3.png"));
            numbers[4] = ImageIO.read(CardImages.class.getResource("/Image/F7-4.png"));
            numbers[5] = ImageIO.read(CardImages.class.getResource("/Image/F7-5.png"));
            numbers[6] = ImageIO.read(CardImages.class.getResource("/Image/F7-6.png"));
            numbers[7] = ImageIO.read(CardImages.class.getResource("/Image/F7-7.png"));
            numbers[8] = ImageIO.read(CardImages.class.getResource("/Image/FC-8.png"));
            numbers[9] = ImageIO.read(CardImages.class.getResource("/Image/FC-9.png"));
            numbers[10] = ImageIO.read(CardImages.class.getResource("/Image/FC-10.png"));
            numbers[11] = ImageIO.read(CardImages.class.getResource("/Image/FC-11.png"));
            numbers[12] = ImageIO.read(CardImages.class.getResource("/Image/FC-12.png"));
            plus2 = ImageIO.read(CardImages.class.getResource("/Image/FC-P2.png"));
            plus4 = ImageIO.read(CardImages.class.getResource("/Image/FC-P4.png"));
            plus6 = ImageIO.read(CardImages.class.getResource("/Image/FC-P6.png"));
            plus8 = ImageIO.read(CardImages.class.getResource("/Image/FC-P8.png"));
            plus10 = ImageIO.read(CardImages.class.getResource("/Image/FC-P10.png"));
            timesTwo = ImageIO.read(CardImages.class.getResource("/Image/FC-T2.png"));
            freeze = ImageIO.read(CardImages.class.getResource("/Image/FC-F.png"));
            flipThree = ImageIO.read(CardImages.class.getResource("/Image/FC-FT.png"));
            secondChance = ImageIO.read(CardImages.class.getResource("/Image/FC-SC.png"));
            back = ImageIO.read(CardImages.class.getResource("/Image/CardBack.png"));
            redArrow = ImageIO.read(CardImages.class.getResource("/Image/RedArrow.png"));
        } catch (Exception E) {
            System.out.println("Exception Error");
            return;
        }
    }

    public BufferedImage getImage(Card card) {
        if (card.type.equals("number")) return numbers[card.value];
        if (card.name.equals("+2")) return plus2;
        if (card.name.equals("+4")) return plus4;
        if (card.name.equals("+6")) return plus6;
        if (card.name.equals("+8")) return plus8;
        if (card.name.equals("+10")) return plus10;
        if (card.name.equals("x2")) return timesTwo;
        if (card.name.equals("Freeze")) return freeze;
        if (card.name.equals("Flip Three")) return flipThree;
        if (card.name.equals("Second Chance")) return secondChance;
        return null;
    }

    public BufferedImage getBack() {
        return back;
    }

    public BufferedImage getRedArrow() {
        return redArrow;
    }
}