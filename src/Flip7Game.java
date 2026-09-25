import java.util.ArrayList;

public class Flip7Game {
    private ArrayList<Card> drawDeck = new ArrayList<Card>();
    private ArrayList<Card> discardDeck = new ArrayList<Card>();
    private ArrayList<Card> roundDiscardDeck = new ArrayList<Card>();
    private ArrayList<Player> players = new ArrayList<Player>();
    private int dealer;
    private int currentPlayer;
    private Card waitingAction;
    private int actionOwner;
    private int flipThreeTarget;
    private int flipThreeRemaining;
    private ArrayList<Card> delayedActions = new ArrayList<Card>();
    private ArrayList<Integer> delayedOwners = new ArrayList<Integer>();
    private String message;
    private boolean roundOver;
    private boolean gameOver;
    private boolean deckReshuffled = false;

    public Flip7Game() {
        for (int i = 1; i <= 5; i++) players.add(new Player("Player " + i));
        makeDeck();
        shuffleDrawDeck();
        dealer = 0;
        startRound();
    }

    private void shuffleDrawDeck() {
        for (int i = drawDeck.size() - 1; i > 0; i--) {
            int other = (int) (Math.random() * (i + 1));
            Card temp = drawDeck.get(i);
            drawDeck.set(i, drawDeck.get(other));
            drawDeck.set(other, temp);
        }
    }

    private void makeDeck() {
        drawDeck.clear();
        drawDeck.add(new Card("number", 0, "0"));
        for (int number = 1; number <= 12; number++) {
            for (int copy = 0; copy < number; copy++) drawDeck.add(new Card("number", number, "" + number));
        }
        for (int value = 2; value <= 10; value += 2) drawDeck.add(new Card("modifier", value, "+" + value));
        drawDeck.add(new Card("multiplier", 2, "x2"));
        for (int i = 0; i < 3; i++) {
            drawDeck.add(new Card("action", 0, "Freeze"));
            drawDeck.add(new Card("action", 0, "Flip Three"));
            drawDeck.add(new Card("action", 0, "Second Chance"));
        }
    }

    private void startRound() {
        for (int i = 0; i < players.size(); i++) players.get(i).startRound();
        discardDeck.clear();
        waitingAction = null;
        delayedActions.clear();
        delayedOwners.clear();
        flipThreeRemaining = 0;
        roundOver = false;
        gameOver = false;
        deckReshuffled = false;
        currentPlayer = nextPlayer(dealer);
        message = players.get(currentPlayer).name + " start. Hit or Stay?";
    }

    private int nextPlayer(int player) { return (player + 1) % players.size(); }
    private boolean isActive(Player player) { return !player.stayed && !player.frozen && !player.busted; }

    private void refillDrawDeck() {
        if (drawDeck.size() == 0 && roundDiscardDeck.size() > 0) {
            for (int i = 0; i < roundDiscardDeck.size(); i++) drawDeck.add(roundDiscardDeck.get(i));
            roundDiscardDeck.clear();
            shuffleDrawDeck();
            deckReshuffled = true;
            message = "Round discard pile was shuffled into the draw deck.";
        }
    }

    private Card drawCard() {
        refillDrawDeck();
        if (drawDeck.size() == 0) {
            endRound("No cards left.");
            return null;
        }
        return drawDeck.remove(drawDeck.size() - 1);
    }

    public void hit() {
        if (isFlippingThree()) {
            drawNextFlipThreeCard();
            return;
        }
        if (roundOver || waitingAction != null || !isActive(players.get(currentPlayer))) return;
        deckReshuffled = false;
        Card card = drawCard();
        if (card == null || roundOver) return;
        receiveCard(currentPlayer, card, false);
        if (!roundOver && waitingAction == null) advanceTurn();
    }

    private void receiveCard(int playerNumber, Card card, boolean forcedDraw) {
        Player player = players.get(playerNumber);
        if (card.type.equals("number") && player.hasNumber(card.value)) {
            if (player.hasSecondChance()) {
                discardDeck.add(player.removeSecondChance());
                discardDeck.add(card);
                message = player.name + " drew a duplicate " + card.name + "! Their Second Chance was used.";
            } else {
                discardDeck.add(card);
                for (int i = 0; i < player.hand.size(); i++) discardDeck.add(player.hand.get(i));
                player.hand.clear();
                player.busted = true;
                message = player.name + " busted by drawing a duplicate " + card.name + ".";
            }
            applyReshuffleNotice();
            return;
        }
        if (card.type.equals("action")) {
            if (forcedDraw) {
                delayedActions.add(card);
                delayedOwners.add(playerNumber);
                message = player.name + " drew " + card.name + " during Flip Three. It will be used after the forced draws.";
            } else {
                startAction(card, playerNumber);
            }
            applyReshuffleNotice();
            return;
        }
        player.hand.add(card);
        message = player.name + " drew " + card.name + ".";
        if (card.type.equals("number") && hasSevenDifferentNumbers(player)) {
            player.flippedSeven = true;
            endRound(player.name + " flipped seven different cards, +15 bonus points");
        }
        applyReshuffleNotice();
    }

    private void applyReshuffleNotice() {
        if (deckReshuffled && message != null && !message.startsWith("Round discard pile was shuffled into the draw deck.")) {
            message = "Round discard pile was shuffled into the draw deck. " + message;
        }
    }

    private void startAction(Card action, int owner) {
        if (!hasEligibleTarget(action)) {
            discardDeck.add(action);
            message = "No player can receive " + action.name + ", so it was discarded.";
            return;
        }
        waitingAction = action;
        actionOwner = owner;
        message = players.get(owner).name + " drew " + action.name + ". Click a player to choose the target.";
    }

    private boolean hasEligibleTarget(Card action) {
        for (int i = 0; i < players.size(); i++) if (isEligibleTarget(i, action)) return true;
        return false;
    }

    private boolean isEligibleTarget(int target, Card action) {
        if (!isActive(players.get(target))) return false;
        if (action.name.equals("Second Chance") && players.get(target).hasSecondChance()) return false;
        return true;
    }

    private boolean hasSevenDifferentNumbers(Player player) {
        int different = 0;
        for (int number = 0; number <= 12; number++) if (player.hasNumber(number)) different++;
        return different >= 7;
    }

    public void chooseTarget(int target) {
        if (waitingAction == null || roundOver || !isEligibleTarget(target, waitingAction)) return;
        deckReshuffled = false;
        Card action = waitingAction;
        waitingAction = null;
        if (action.name.equals("Freeze")) {
            discardDeck.add(action);
            players.get(target).frozen = true;
            message = players.get(actionOwner).name + " froze " + players.get(target).name + ".";
            finishAction();
        } else if (action.name.equals("Second Chance")) {
            players.get(target).hand.add(action);
            message = players.get(actionOwner).name + " gave a Second Chance to " + players.get(target).name + ".";
            finishAction();
        } else {
            discardDeck.add(action);
            message = players.get(actionOwner).name + " made " + players.get(target).name + " flip three cards.";
            flipThree(target);

        }
    }

    private void finishAction() {
        if (roundOver) return;
        showNextDelayedAction();
    }

    private void showNextDelayedAction() {
        while (delayedActions.size() > 0) {
            Card action = delayedActions.remove(0);
            int owner = delayedOwners.remove(0);
            if (players.get(owner).busted) {
                discardDeck.add(action);
                continue;
            }
            if (hasEligibleTarget(action)) {
                waitingAction = action;
                actionOwner = owner;
                message = players.get(owner).name + " must now use " + action.name + ". Click an eligible player.";
                applyReshuffleNotice();
                return;
            }
            discardDeck.add(action);
            message = "No player can receive " + action.name + ", so it was discarded.";
            applyReshuffleNotice();
        }
        advanceTurn();
    }

    private void flipThree(int target) {
        flipThreeTarget = target;
        flipThreeRemaining = 3;
        message = message + " Click FLIP for each card.";
    }

    private void drawNextFlipThreeCard() {
        if (roundOver || flipThreeRemaining == 0) return;
        Card card = drawCard();
        if (card == null || roundOver) return;
        flipThreeRemaining--;
        receiveCard(flipThreeTarget, card, true);
        if (roundOver) return;
        if (!isActive(players.get(flipThreeTarget)) || flipThreeRemaining == 0) {
            flipThreeRemaining = 0;
            finishAction();
        }
    }

    public boolean isFlippingThree() { return flipThreeRemaining > 0; }
    public int getFlipThreeRemaining() { return flipThreeRemaining; }
    public int getFlipThreeTarget() { return flipThreeTarget; }
    public void stay() {
        if (isFlippingThree()) return;
        if (roundOver || waitingAction != null || !isActive(players.get(currentPlayer))) return;
        deckReshuffled = false;
        players.get(currentPlayer).stayed = true;
        message = players.get(currentPlayer).name + " stayed.";
        advanceTurn();
    }

    private void advanceTurn() {
        for (int i = 0; i < players.size(); i++) {
            currentPlayer = nextPlayer(currentPlayer);
            if (isActive(players.get(currentPlayer))) {
                message = message + "  " + players.get(currentPlayer).name + "'s turn.";
                return;
            }
        }
        endRound("Nobody can receive another card.");
    }

    private void endRound(String ending) {
        flipThreeRemaining = 0;
        roundOver = true;
        if (waitingAction != null) discardDeck.add(waitingAction);
        waitingAction = null;
        for (int i = 0; i < delayedActions.size(); i++) discardDeck.add(delayedActions.get(i));
        delayedActions.clear();
        delayedOwners.clear();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (!player.busted) player.score += player.roundScore();
        }
        moveRoundCards();
        message = ending + " Round scores were added. Click Next Round.";
        checkWinner();
        applyReshuffleNotice();
    }

    private void moveRoundCards() {
        for (int i = 0; i < discardDeck.size(); i++) roundDiscardDeck.add(discardDeck.get(i));
        discardDeck.clear();
        for (int i = 0; i < players.size(); i++) {
            for (int j = 0; j < players.get(i).hand.size(); j++)
                roundDiscardDeck.add(players.get(i).hand.get(j));
        }
    }

    private void checkWinner() {
        int best = -1;
        int leaders = 0;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).score > best) { best = players.get(i).score; leaders = 1; }
            else if (players.get(i).score == best) leaders++;
        }
        if (best > 200 && leaders == 1) {
            gameOver = true;
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).score == best) message = players.get(i).name + " wins with " + best + " points!";
            }
        } else if (best > 200) {
            message = "The high score is tied at " + best + ". Play another round to break the tie.";
        }
    }

    public void nextRound() {
        if (!roundOver || gameOver) return;
        dealer = nextPlayer(dealer);
        startRound();
    }

    public Player getPlayer(int number) { return players.get(number); }
    public int getPlayerCount() { return players.size(); }
    public int getDealer() { return dealer; }
    public int getCurrentPlayer() { return currentPlayer; }
    public String getMessage() {
        if (deckReshuffled && message != null && !message.startsWith("Round discard pile was shuffled into the draw deck.")) {
            return "Round discard pile was shuffled into the draw deck. " + message;
        }
        return message;
    }
    public boolean isRoundOver() { return roundOver; }
    public boolean isGameOver() { return gameOver; }
    public boolean isWaitingForTarget() { return waitingAction != null; }
    public boolean isEligibleTarget(int target) { return waitingAction != null && isEligibleTarget(target, waitingAction); }
    public int getDrawDeckSize() { return drawDeck.size(); }
    public int getDiscardDeckSize() { return discardDeck.size(); }
    public int getRoundDiscardDeckSize() { return roundDiscardDeck.size(); }
    public Card getTopDiscard() {
        if (discardDeck.size() == 0) return null;
        return discardDeck.get(discardDeck.size() - 1);
    }
}



