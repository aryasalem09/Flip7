import java.util.ArrayList;

public class Player {
    public String name;
    public ArrayList<Card> hand;
    public int score;
    public boolean stayed;
    public boolean frozen;
    public boolean busted;
    public boolean flippedSeven;

    public Player(String name) {
        this.name = name;
        hand = new ArrayList<Card>();
        score = 0;
    }

    public void startRound() {
        hand.clear();
        stayed = false;
        frozen = false;
        busted = false;
        flippedSeven = false;
    }

    public boolean hasNumber(int number) {
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).type.equals("number") && hand.get(i).value == number) return true;
        }
        return false;
    }

    public boolean hasSecondChance() {
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).type.equals("action") && hand.get(i).name.equals("Second Chance")) return true;
        }
        return false;
    }

    public Card removeSecondChance() {
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).type.equals("action") && hand.get(i).name.equals("Second Chance")) return hand.remove(i);
        }
        return null;
    }

    public int roundScore() {
        int faces = 0;
        int modifiers = 0;
        boolean timesTwo = false;
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (card.type.equals("number")) faces += card.value;
            else if (card.type.equals("modifier")) modifiers += card.value;
            else if (card.type.equals("multiplier")) timesTwo = true;
        }
        if (timesTwo) faces = faces * 2;
        if (flippedSeven) return faces + modifiers + 15;
        return faces + modifiers;
    }
}
