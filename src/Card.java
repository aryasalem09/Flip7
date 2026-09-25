public class Card {
    public String type;
    public int value;
    public String name;

    public Card(String type, int value, String name) {
        this.type = type;
        this.value = value;
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
