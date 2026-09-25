import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JFrame;

public class TestHarness {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Flip 7 Test Suite...");
        testDeckCompositionAndConservation();
        testCardImagesLoadingCleanly();
        testFlip7WindowExitOnClose();
        testSecondChanceMechanics();
        testFreezeMechanics();
        testFlipThreeStepsOneCardAndBlocksStay();
        testFlipThreeBustAbortsAndDiscardsQueuedActions();
        testFlipThreeSevenUniqueBonusAndImmediateRoundEnd();
        testRefillDrawDeckPreservesMessageAndSeparatesDiscards();
        testFlipThreeMultiDrawReshufflePreservation();
        testFlipThreeReshuffleFollowedByBust();
        testLastActivePlayerBustAfterReshuffle();
        testDrawMessageTextWrappingWithinBounds();
        testCenteredPanelInteractionBounds();
        testTiebreakerAndOver200Win();
        testLeapfroggingInTiebreaker();
        testSimulatedFullGames(200);
        System.out.println("ALL TESTS PASSED SUCCESSFULLY!");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError(message + " | Expected: " + expected + ", Actual: " + actual);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) throw new AssertionError(message);
    }

    // Helper to get total cards across all game piles and players
    @SuppressWarnings("unchecked")
    private static int countTotalCards(Flip7Game game) throws Exception {
        Field drawF = Flip7Game.class.getDeclaredField("drawDeck");
        drawF.setAccessible(true);
        ArrayList<Card> drawDeck = (ArrayList<Card>) drawF.get(game);

        Field discardF = Flip7Game.class.getDeclaredField("discardDeck");
        discardF.setAccessible(true);
        ArrayList<Card> discardDeck = (ArrayList<Card>) discardF.get(game);

        Field roundDiscardF = Flip7Game.class.getDeclaredField("roundDiscardDeck");
        roundDiscardF.setAccessible(true);
        ArrayList<Card> roundDiscardDeck = (ArrayList<Card>) roundDiscardF.get(game);

        Field waitingActionF = Flip7Game.class.getDeclaredField("waitingAction");
        waitingActionF.setAccessible(true);
        Card waitingAction = (Card) waitingActionF.get(game);

        Field delayedActionsF = Flip7Game.class.getDeclaredField("delayedActions");
        delayedActionsF.setAccessible(true);
        ArrayList<Card> delayedActions = (ArrayList<Card>) delayedActionsF.get(game);

        int total = drawDeck.size() + discardDeck.size() + roundDiscardDeck.size()
                + (waitingAction != null ? 1 : 0) + delayedActions.size();

        if (!game.isRoundOver()) {
            for (int i = 0; i < game.getPlayerCount(); i++) {
                total += game.getPlayer(i).hand.size();
            }
        }
        return total;
    }

    private static void testDeckCompositionAndConservation() throws Exception {
        System.out.print("Test 1: Deck composition & initial card count... ");
        Flip7Game game = new Flip7Game();
        assertEquals(94, countTotalCards(game), "Total card count must be 94");
        assertEquals(94, game.getDrawDeckSize(), "Initial draw deck size must be 94");
        assertEquals(0, game.getDiscardDeckSize(), "Initial discard deck size must be 0");
        assertEquals(0, game.getRoundDiscardDeckSize(), "Initial round discard deck size must be 0");

        // Inspect card types and counts
        Field drawF = Flip7Game.class.getDeclaredField("drawDeck");
        drawF.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Card> drawDeck = (ArrayList<Card>) drawF.get(game);

        int zeroCount = 0;
        int[] numberCounts = new int[13];
        int modCount = 0;
        int multCount = 0;
        int freezeCount = 0;
        int flipThreeCount = 0;
        int secondChanceCount = 0;

        for (Card c : drawDeck) {
            if (c.type.equals("number")) {
                if (c.value == 0) zeroCount++;
                else numberCounts[c.value]++;
            } else if (c.type.equals("modifier")) {
                modCount++;
            } else if (c.type.equals("multiplier")) {
                multCount++;
            } else if (c.type.equals("action")) {
                if (c.name.equals("Freeze")) freezeCount++;
                else if (c.name.equals("Flip Three")) flipThreeCount++;
                else if (c.name.equals("Second Chance")) secondChanceCount++;
            }
        }

        assertEquals(1, zeroCount, "Number 0 must have 1 card");
        for (int i = 1; i <= 12; i++) {
            assertEquals(i, numberCounts[i], "Number " + i + " must have " + i + " cards");
        }
        assertEquals(5, modCount, "Modifiers count must be 5 (+2, +4, +6, +8, +10)");
        assertEquals(1, multCount, "Multiplier x2 count must be 1");
        assertEquals(3, freezeCount, "Freeze cards must be 3");
        assertEquals(3, flipThreeCount, "Flip Three cards must be 3");
        assertEquals(3, secondChanceCount, "Second Chance cards must be 3");

        System.out.println("PASSED");
    }

    private static void testCardImagesLoadingCleanly() {
        System.out.print("Test 2: CardImages loading all assets cleanly... ");
        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CardImages images;
        try {
            System.setOut(new PrintStream(baos));
            images = new CardImages();
        } finally {
            System.setOut(originalOut);
        }
        String captured = baos.toString();
        assertFalse(captured.contains("Image could not be loaded."), "CardImages printed error: " + captured);
        assertTrue(images.getBack() != null, "CardBack image must be loaded and not null");
        assertTrue(images.getRedArrow() != null, "RedArrow image must be loaded and not null");
        for (int i = 0; i <= 12; i++) {
            assertTrue(images.getImage(new Card("number", i, "" + i)) != null, "Card image for " + i + " must load");
        }
        assertTrue(images.getImage(new Card("modifier", 2, "+2")) != null, "+2 image must load");
        assertTrue(images.getImage(new Card("modifier", 4, "+4")) != null, "+4 image must load");
        assertTrue(images.getImage(new Card("modifier", 6, "+6")) != null, "+6 image must load");
        assertTrue(images.getImage(new Card("modifier", 8, "+8")) != null, "+8 image must load");
        assertTrue(images.getImage(new Card("modifier", 10, "+10")) != null, "+10 image must load");
        assertTrue(images.getImage(new Card("multiplier", 2, "x2")) != null, "x2 image must load");
        assertTrue(images.getImage(new Card("action", 0, "Freeze")) != null, "Freeze image must load");
        assertTrue(images.getImage(new Card("action", 0, "Flip Three")) != null, "Flip Three image must load");
        assertTrue(images.getImage(new Card("action", 0, "Second Chance")) != null, "Second Chance image must load");
        System.out.println("PASSED");
    }

    private static void testFlip7WindowExitOnClose() {
        System.out.print("Test 3: Flip7 JFrame exit on close... ");
        try {
            Flip7 window = new Flip7("Test Window");
            int closeOp = window.getDefaultCloseOperation();
            window.dispose();
            assertEquals(JFrame.EXIT_ON_CLOSE, closeOp, "Close operation must be EXIT_ON_CLOSE");
        } catch (java.awt.HeadlessException e) {
            System.out.println("SKIPPED (headless environment)");
            return;
        }
        System.out.println("PASSED");
    }

    private static void testSecondChanceMechanics() throws Exception {
        System.out.print("Test 4: Second Chance eligibility and consumption... ");
        Flip7Game game = new Flip7Game();
        Player p1 = game.getPlayer(1);

        Card sc = new Card("action", 0, "Second Chance");
        Method isEligible = Flip7Game.class.getDeclaredMethod("isEligibleTarget", int.class, Card.class);
        isEligible.setAccessible(true);

        assertTrue((Boolean) isEligible.invoke(game, 1, sc), "Active player without SC should be eligible");

        p1.hand.add(sc);
        assertFalse((Boolean) isEligible.invoke(game, 1, sc), "Active player with SC should NOT be eligible");

        // Duplicate draw uses SC
        p1.hand.add(new Card("number", 7, "7"));
        Method receive = Flip7Game.class.getDeclaredMethod("receiveCard", int.class, Card.class, boolean.class);
        receive.setAccessible(true);

        receive.invoke(game, 1, new Card("number", 7, "7"), false);
        assertFalse(p1.busted, "Player should NOT be busted after using Second Chance");
        assertFalse(p1.hasSecondChance(), "Player should have consumed Second Chance");
        assertEquals(1, p1.hand.size(), "Player should only retain original 7");
        assertEquals(2, game.getDiscardDeckSize(), "Used SC and duplicate 7 should be in discard deck");

        // Now next duplicate should bust
        receive.invoke(game, 1, new Card("number", 7, "7"), false);
        assertTrue(p1.busted, "Player without Second Chance should bust on duplicate");
        assertEquals(0, p1.hand.size(), "Busted player hand must be cleared");

        System.out.println("PASSED");
    }

    private static void testFreezeMechanics() throws Exception {
        System.out.print("Test 5: Freeze mechanics & score preservation... ");
        Flip7Game game = new Flip7Game();
        Player p1 = game.getPlayer(1);
        p1.hand.add(new Card("number", 8, "8"));
        p1.hand.add(new Card("modifier", 4, "+4"));

        Field waitingActionF = Flip7Game.class.getDeclaredField("waitingAction");
        waitingActionF.setAccessible(true);
        waitingActionF.set(game, new Card("action", 0, "Freeze"));

        Field actionOwnerF = Flip7Game.class.getDeclaredField("actionOwner");
        actionOwnerF.setAccessible(true);
        actionOwnerF.set(game, 0);

        game.chooseTarget(1);
        assertTrue(p1.frozen, "Target should be frozen");
        Method isActiveM = Flip7Game.class.getDeclaredMethod("isActive", Player.class);
        isActiveM.setAccessible(true);
        assertFalse((Boolean) isActiveM.invoke(game, p1), "Frozen player is not active");

        // End round and verify points banked
        Method endRoundM = Flip7Game.class.getDeclaredMethod("endRound", String.class);
        endRoundM.setAccessible(true);
        endRoundM.invoke(game, "Round test over.");

        assertEquals(12, p1.score, "Frozen player should bank hand points (8 + 4 = 12)");
        System.out.println("PASSED");
    }

    private static void drainFlipThree(Flip7Game game) {
        int maximumForcedDraws = 3;
        while (game.isFlippingThree() && maximumForcedDraws-- > 0) {
            game.hit();
        }
        assertFalse(game.isFlippingThree(), "Flip Three must finish within three forced draws");
    }

    @SuppressWarnings("unchecked")
    private static void testFlipThreeStepsOneCardAndBlocksStay() throws Exception {
        System.out.print("Test 6: Flip Three starts without drawing, steps once, and blocks stay... ");
        Flip7Game game = new Flip7Game();

        Field drawF = Flip7Game.class.getDeclaredField("drawDeck");
        drawF.setAccessible(true);
        ArrayList<Card> drawDeck = (ArrayList<Card>) drawF.get(game);
        drawDeck.clear();
        drawDeck.add(new Card("number", 9, "9"));
        drawDeck.add(new Card("number", 8, "8"));
        drawDeck.add(new Card("number", 7, "7"));

        Field waitingActionF = Flip7Game.class.getDeclaredField("waitingAction");
        waitingActionF.setAccessible(true);
        waitingActionF.set(game, new Card("action", 0, "Flip Three"));
        Field actionOwnerF = Flip7Game.class.getDeclaredField("actionOwner");
        actionOwnerF.setAccessible(true);
        actionOwnerF.set(game, 0);

        game.chooseTarget(1);
        assertTrue(game.isFlippingThree(), "Choosing a Flip Three target must begin the forced sequence");
        assertEquals(3, game.getFlipThreeRemaining(), "Flip Three must begin with three remaining draws");
        assertEquals(3, drawDeck.size(), "Choosing a target must draw zero cards");

        game.stay();
        assertTrue(game.isFlippingThree(), "Stay must be blocked during Flip Three");
        assertEquals(3, game.getFlipThreeRemaining(), "Blocked stay must not consume a forced draw");
        assertEquals(3, drawDeck.size(), "Blocked stay must not draw a card");

        game.hit();
        assertTrue(game.isFlippingThree(), "One forced hit must leave the sequence active");
        assertEquals(2, game.getFlipThreeRemaining(), "One forced hit must consume exactly one draw");
        assertEquals(2, drawDeck.size(), "One forced hit must draw exactly one card");
        assertTrue(game.getPlayer(1).hasNumber(7), "First forced hit must give the target the top card");

        System.out.println("PASSED");
    }

    @SuppressWarnings("unchecked")
    private static void testFlipThreeBustAbortsAndDiscardsQueuedActions() throws Exception {
        System.out.print("Test 7: Flip Three busts immediately & discards queued actions... ");
        Flip7Game game = new Flip7Game();
        Player p1 = game.getPlayer(1);
        p1.hand.add(new Card("number", 5, "5")); // starting card

        // Stack drawDeck:
        // Top of deck is last element of ArrayList
        Field drawF = Flip7Game.class.getDeclaredField("drawDeck");
        drawF.setAccessible(true);
        ArrayList<Card> drawDeck = (ArrayList<Card>) drawF.get(game);
        drawDeck.clear();

        // Cards to draw in Flip Three:
        // Card 1: Freeze (action) -> queued as delayed action
        // Card 2: 5 (duplicate) -> will bust p1!
        // Card 3: 9 (number) -> should NOT be drawn because p1 busted on card 2!
        drawDeck.add(new Card("number", 9, "9"));       // drawn 3rd (if not aborted)
        drawDeck.add(new Card("number", 5, "5"));       // drawn 2nd
        drawDeck.add(new Card("action", 0, "Freeze"));  // drawn 1st

        Field waitingActionF = Flip7Game.class.getDeclaredField("waitingAction");
        waitingActionF.setAccessible(true);
        waitingActionF.set(game, new Card("action", 0, "Flip Three"));

        Field actionOwnerF = Flip7Game.class.getDeclaredField("actionOwner");
        actionOwnerF.setAccessible(true);
        actionOwnerF.set(game, 0);

        game.chooseTarget(1);
        assertTrue(game.isFlippingThree(), "Flip Three must await forced hits after target selection");
        assertEquals(3, game.getFlipThreeRemaining(), "No forced card is drawn on target selection");
        drainFlipThree(game);

        // Verify Player 1 busted
        assertTrue(p1.busted, "Player 1 should be busted");
        // Verify Card 3 was NOT drawn
        assertEquals(1, drawDeck.size(), "3rd card was NOT drawn after bust");
        assertEquals("9", drawDeck.get(0).name, "Remaining card in draw deck must be 9");

        // Verify delayed action (Freeze) was discarded to discardDeck and NOT waiting for target
        assertFalse(game.isWaitingForTarget(), "Game should NOT be waiting for target from busted player");
        Field delayedActionsF = Flip7Game.class.getDeclaredField("delayedActions");
        delayedActionsF.setAccessible(true);
        ArrayList<Card> delayedActions = (ArrayList<Card>) delayedActionsF.get(game);
        assertEquals(0, delayedActions.size(), "delayedActions queue must be empty");

        // Discard deck must contain: Flip Three (used), Freeze (discarded), 5 (original), 5 (duplicate)
        assertEquals(4, game.getDiscardDeckSize(), "Discard deck should contain Flip Three, Freeze, and two 5s");

        System.out.println("PASSED");
    }

    @SuppressWarnings("unchecked")
    private static void testFlipThreeSevenUniqueBonusAndImmediateRoundEnd() throws Exception {
        System.out.print("Test 8: Flip Three 7 unique numbers ends round with +15... ");
        Flip7Game game = new Flip7Game();
        Player p1 = game.getPlayer(1);
        for (int i = 1; i <= 6; i++) {
            p1.hand.add(new Card("number", i, "" + i));
        }

        Field drawF = Flip7Game.class.getDeclaredField("drawDeck");
        drawF.setAccessible(true);
        ArrayList<Card> drawDeck = (ArrayList<Card>) drawF.get(game);
        drawDeck.clear();

        // Top card drawn: 7 (the 7th unique card!)
        // Other cards should not even be drawn
        drawDeck.add(new Card("number", 10, "10"));
        drawDeck.add(new Card("number", 9, "9"));
        drawDeck.add(new Card("number", 7, "7"));

        Field waitingActionF = Flip7Game.class.getDeclaredField("waitingAction");
        waitingActionF.setAccessible(true);
        waitingActionF.set(game, new Card("action", 0, "Flip Three"));

        Field actionOwnerF = Flip7Game.class.getDeclaredField("actionOwner");
        actionOwnerF.setAccessible(true);
        actionOwnerF.set(game, 0);

        game.chooseTarget(1);
        drainFlipThree(game);

        assertTrue(game.isRoundOver(), "Round must end immediately when 7 unique cards are flipped");
        assertTrue(p1.flippedSeven, "Player 1 flippedSeven must be true");
        // Score: (1+2+3+4+5+6+7) + 15 bonus = 28 + 15 = 43 points
        assertEquals(43, p1.score, "Player 1 score should include sum of cards (28) + 15 bonus = 43");
        assertEquals(2, drawDeck.size(), "Subsequent cards were not drawn");

        System.out.println("PASSED");
    }

    @SuppressWarnings("unchecked")
    private static void testRefillDrawDeckPreservesMessageAndSeparatesDiscards() throws Exception {
        System.out.print("Test 8: refillDrawDeck message preservation & 3-deck separation... ");
        Flip7Game game = new Flip7Game();

        Field drawF = Flip7Game.class.getDeclaredField("drawDeck");
        drawF.setAccessible(true);
        ArrayList<Card> drawDeck = (ArrayList<Card>) drawF.get(game);

        Field discardF = Flip7Game.class.getDeclaredField("discardDeck");
        discardF.setAccessible(true);
        ArrayList<Card> discardDeck = (ArrayList<Card>) discardF.get(game);

        Field roundDiscardF = Flip7Game.class.getDeclaredField("roundDiscardDeck");
        roundDiscardF.setAccessible(true);
        ArrayList<Card> roundDiscardDeck = (ArrayList<Card>) roundDiscardF.get(game);

        // Put 1 card in discardDeck (from current round)
        discardDeck.add(new Card("number", 8, "8"));
        // Empty drawDeck
        drawDeck.clear();
        // Put cards in roundDiscardDeck
        roundDiscardDeck.add(new Card("number", 1, "1"));
        roundDiscardDeck.add(new Card("number", 2, "2"));
        roundDiscardDeck.add(new Card("number", 3, "3"));

        int activePlayer = game.getCurrentPlayer();
        game.hit();

        // Verify discardDeck is STILL 1 card (NOT shuffled into drawDeck!)
        assertEquals(1, game.getDiscardDeckSize(), "Current-round discardDeck must NOT be refilled into drawDeck");
        // Verify roundDiscardDeck was emptied into drawDeck (minus 1 card drawn)
        assertEquals(0, game.getRoundDiscardDeckSize(), "roundDiscardDeck must be emptied into drawDeck");
        assertEquals(2, game.getDrawDeckSize(), "drawDeck has 3 - 1 = 2 cards");

        // Verify message contains the reshuffle announcement prepended!
        String msg = game.getMessage();
        assertTrue(msg.contains("The round discard pile was shuffled into the draw deck."),
                "Message must preserve reshuffle text: " + msg);

        System.out.println("PASSED");
    }

    @SuppressWarnings("unchecked")
    private static void testFlipThreeMultiDrawReshufflePreservation() throws Exception {
        System.out.print("Test 8b: Flip Three preserves reshuffle message across all 3 draws... ");
        Flip7Game game = new Flip7Game();

        Field drawF = Flip7Game.class.getDeclaredField("drawDeck");
        drawF.setAccessible(true);
        ArrayList<Card> drawDeck = (ArrayList<Card>) drawF.get(game);

        Field roundDiscardF = Flip7Game.class.getDeclaredField("roundDiscardDeck");
        roundDiscardF.setAccessible(true);
        ArrayList<Card> roundDiscardDeck = (ArrayList<Card>) roundDiscardF.get(game);

        // Empty drawDeck and put 3 cards in roundDiscardDeck
        drawDeck.clear();
        roundDiscardDeck.add(new Card("number", 1, "1"));
        roundDiscardDeck.add(new Card("number", 2, "2"));
        roundDiscardDeck.add(new Card("number", 3, "3"));

        // Setup waitingAction Flip Three targeting player 1
        Field waitingActionF = Flip7Game.class.getDeclaredField("waitingAction");
        waitingActionF.setAccessible(true);
        waitingActionF.set(game, new Card("action", 0, "Flip Three"));

        Field actionOwnerF = Flip7Game.class.getDeclaredField("actionOwner");
        actionOwnerF.setAccessible(true);
        actionOwnerF.set(game, 0);

        game.chooseTarget(1);
        drainFlipThree(game);

        String msg = game.getMessage();
        assertTrue(msg.contains("The round discard pile was shuffled into the draw deck."),
                "Reshuffle notice must survive all 3 draws of Flip Three: " + msg);
        System.out.println("PASSED");
    }

    @SuppressWarnings("unchecked")
    private static void testFlipThreeReshuffleFollowedByBust() throws Exception {
        System.out.print("Test 8c: Flip Three reshuffle followed by bust retains reshuffle notice... ");
        Flip7Game game = new Flip7Game();
        Player p1 = game.getPlayer(1);
        p1.hand.add(new Card("number", 7, "7"));

        Field drawF = Flip7Game.class.getDeclaredField("drawDeck");
        drawF.setAccessible(true);
        ArrayList<Card> drawDeck = (ArrayList<Card>) drawF.get(game);

        Field roundDiscardF = Flip7Game.class.getDeclaredField("roundDiscardDeck");
        roundDiscardF.setAccessible(true);
        ArrayList<Card> roundDiscardDeck = (ArrayList<Card>) roundDiscardF.get(game);

        // Empty drawDeck and put 2 cards in roundDiscardDeck: a 7 (duplicate that busts) and a 9
        drawDeck.clear();
        roundDiscardDeck.add(new Card("number", 7, "7"));
        roundDiscardDeck.add(new Card("number", 7, "7"));

        Field waitingActionF = Flip7Game.class.getDeclaredField("waitingAction");
        waitingActionF.setAccessible(true);
        waitingActionF.set(game, new Card("action", 0, "Flip Three"));

        Field actionOwnerF = Flip7Game.class.getDeclaredField("actionOwner");
        actionOwnerF.setAccessible(true);
        actionOwnerF.set(game, 0);

        game.chooseTarget(1);
        drainFlipThree(game);

        assertTrue(p1.busted, "Player 1 should have busted on duplicate 7");
        String msg = game.getMessage();
        assertTrue(msg.contains("The round discard pile was shuffled into the draw deck."),
                "Message must retain reshuffle notice when bust occurs: " + msg);
        assertTrue(msg.contains("busted"),
                "Message must mention player busted: " + msg);
        System.out.println("PASSED");
    }

    @SuppressWarnings("unchecked")
    private static void testLastActivePlayerBustAfterReshuffle() throws Exception {
        System.out.print("Test 8d: Last active player bust after reshuffle retains reshuffle notice... ");
        Flip7Game game = new Flip7Game();

        // Make players 1, 2, 3, 4 stayed
        for (int i = 1; i < 5; i++) {
            game.getPlayer(i).stayed = true;
        }

        Player p0 = game.getPlayer(0);
        p0.hand.add(new Card("number", 4, "4"));

        Field currentF = Flip7Game.class.getDeclaredField("currentPlayer");
        currentF.setAccessible(true);
        currentF.set(game, 0);

        Field drawF = Flip7Game.class.getDeclaredField("drawDeck");
        drawF.setAccessible(true);
        ArrayList<Card> drawDeck = (ArrayList<Card>) drawF.get(game);

        Field roundDiscardF = Flip7Game.class.getDeclaredField("roundDiscardDeck");
        roundDiscardF.setAccessible(true);
        ArrayList<Card> roundDiscardDeck = (ArrayList<Card>) roundDiscardF.get(game);

        drawDeck.clear();
        roundDiscardDeck.add(new Card("number", 4, "4")); // duplicate causes bust

        game.hit();

        assertTrue(p0.busted, "Player 0 must bust");
        assertTrue(game.isRoundOver(), "Round must be over as no active players remain");
        String msg = game.getMessage();
        assertTrue(msg.contains("The round discard pile was shuffled into the draw deck."),
                "Message must preserve reshuffle notice when last player busts: " + msg);
        System.out.println("PASSED");
    }

    private static void testDrawMessageTextWrappingWithinBounds() throws Exception {
        System.out.print("Test 8e: Flip7Panel drawMessage text wrapping within bounds... ");
        Flip7Panel panel = new Flip7Panel();
        Method wrapMethod = Flip7Panel.class.getDeclaredMethod("wrapText", String.class, java.awt.Graphics.class,
                int.class);
        wrapMethod.setAccessible(true);

        BufferedImage img = new BufferedImage(1200, 780, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        int maxWidth = 515;

        // Test very long reshuffle + tiebreaker message
        String testMsg = "The round discard pile was shuffled into the draw deck. The high score is tied at 210. Play another round to break the tie.";
        @SuppressWarnings("unchecked")
        ArrayList<String> lines = (ArrayList<String>) wrapMethod.invoke(panel, testMsg, g2, maxWidth);

        assertTrue(lines.size() >= 2, "Message should wrap into at least 2 lines");
        for (String line : lines) {
            int lineWidth = g2.getFontMetrics().stringWidth(line);
            assertTrue(lineWidth <= maxWidth, "Wrapped line exceeds maxWidth: '" + line + "' (" + lineWidth
                    + " > " + maxWidth + ")");
        }

        @SuppressWarnings("unchecked")
        ArrayList<String> unbrokenLines = (ArrayList<String>) wrapMethod.invoke(panel,
                "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ",
                g2, maxWidth);
        assertTrue(unbrokenLines.size() >= 2, "An unbroken long word should wrap without an exception");
        for (String line : unbrokenLines) {
            assertTrue(g2.getFontMetrics().stringWidth(line) <= maxWidth,
                    "Unbroken wrapped line exceeds maxWidth: '" + line + "'");
        }

        // Test painting panel directly to verify no Graphics exceptions
        panel.setSize(1200, 780);
        panel.paint(g2);
        g2.dispose();

        System.out.println("PASSED");
    }

    private static void testCenteredPanelInteractionBounds() throws Exception {
        System.out.print("Test 8f: Flip7Panel centered player and button interaction bounds... ");
        Flip7Panel panel = new Flip7Panel();
        panel.setSize(1200, 780);
        int center = panel.getWidth() / 2;
        Method playerAt = Flip7Panel.class.getDeclaredMethod("playerAt", int.class, int.class);
        playerAt.setAccessible(true);

        assertEquals(0, playerAt.invoke(panel, center, 95 + 70), "Top player box must be centered");
        assertEquals(1, playerAt.invoke(panel, panel.getWidth() - 305 + 140, 185 + 70),
                "Upper-right player box must use the right layout edge");
        assertEquals(2, playerAt.invoke(panel, panel.getWidth() - 305 + 140, 510 + 70),
                "Lower-right player box must use the right layout edge");
        assertEquals(3, playerAt.invoke(panel, 25 + 140, 510 + 70),
                "Lower-left player box must use the left layout edge");
        assertEquals(4, playerAt.invoke(panel, 25 + 140, 185 + 70),
                "Upper-left player box must use the left layout edge");

        Field gameField = Flip7Panel.class.getDeclaredField("game");
        gameField.setAccessible(true);
        Flip7Game game = (Flip7Game) gameField.get(panel);
        int initialDrawDeckSize = game.getDrawDeckSize();
        panel.mouseClicked(new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, 0, 0, center - 195 + 60, 592, 1,
                false, MouseEvent.BUTTON1));
        assertEquals(initialDrawDeckSize - 1, game.getDrawDeckSize(), "HIT button must be centered at center - 195");

        Flip7Panel stayPanel = new Flip7Panel();
        stayPanel.setSize(1200, 780);
        Flip7Game stayGame = (Flip7Game) gameField.get(stayPanel);
        int stayingPlayer = stayGame.getCurrentPlayer();
        stayPanel.mouseClicked(new MouseEvent(stayPanel, MouseEvent.MOUSE_CLICKED, 0, 0, center, 592, 1, false,
                MouseEvent.BUTTON1));
        assertTrue(stayGame.getPlayer(stayingPlayer).stayed, "STAY button must be centered at center - 60");

        Flip7Panel nextRoundPanel = new Flip7Panel();
        nextRoundPanel.setSize(1200, 780);
        Flip7Game nextRoundGame = (Flip7Game) gameField.get(nextRoundPanel);
        for (int i = 0; i < nextRoundGame.getPlayerCount(); i++) {
            if (i != nextRoundGame.getCurrentPlayer())
                nextRoundGame.getPlayer(i).stayed = true;
        }
        nextRoundGame.stay();
        assertTrue(nextRoundGame.isRoundOver(), "Game must be ready for NEXT ROUND interaction");
        nextRoundPanel.mouseClicked(new MouseEvent(nextRoundPanel, MouseEvent.MOUSE_CLICKED, 0, 0, center + 75 + 60,
                592, 1, false, MouseEvent.BUTTON1));
        assertFalse(nextRoundGame.isRoundOver(), "NEXT ROUND button must be centered at center + 75");

        System.out.println("PASSED");
    }

    private static void testTiebreakerAndOver200Win() throws Exception {
        System.out.print("Test 9: Win condition > 200 and tiebreaker... ");
        Flip7Game game = new Flip7Game();
        Player p1 = game.getPlayer(1);
        Player p2 = game.getPlayer(2);

        // Case 1: High score exactly 200 does NOT trigger win or tiebreaker
        p1.score = 200;
        p2.score = 200;
        Method checkWinnerM = Flip7Game.class.getDeclaredMethod("checkWinner");
        checkWinnerM.setAccessible(true);
        checkWinnerM.invoke(game);
        assertFalse(game.isGameOver(), "Score = 200 does not win");
        assertFalse(game.getMessage().contains("tied"), "Score = 200 does not trigger tiebreaker message");

        // Case 2: Sole leader > 200 triggers game over
        p1.score = 205;
        p2.score = 190;
        checkWinnerM.invoke(game);
        assertTrue(game.isGameOver(), "Score > 200 with sole leader must end game");
        assertTrue(game.getMessage().contains("Player 2 wins with 205 points!"), "Winner announcement must be correct: " + game.getMessage());

        // Case 3: Tied high score > 200 triggers tiebreaker
        Field gameOverF = Flip7Game.class.getDeclaredField("gameOver");
        gameOverF.setAccessible(true);
        gameOverF.set(game, false);
        p1.score = 210;
        p2.score = 210;
        checkWinnerM.invoke(game);
        assertFalse(game.isGameOver(), "Tied score > 200 must NOT end game");
        assertTrue(game.getMessage().contains("The high score is tied at 210."), "Tiebreaker message must be set: " + game.getMessage());

        System.out.println("PASSED");
    }

    private static void testLeapfroggingInTiebreaker() throws Exception {
        System.out.print("Test 10: Tiebreaker round allows leapfrogging... ");
        Flip7Game game = new Flip7Game();
        Player p1 = game.getPlayer(0);
        Player p2 = game.getPlayer(1);
        Player p3 = game.getPlayer(2);

        // Tied at 205
        p1.score = 205;
        p2.score = 205;
        p3.score = 180;

        Method endRoundM = Flip7Game.class.getDeclaredMethod("endRound", String.class);
        endRoundM.setAccessible(true);
        endRoundM.invoke(game, "End tied round.");

        assertTrue(game.isRoundOver(), "Round must be over");
        assertFalse(game.isGameOver(), "Game must NOT be over");
        assertTrue(game.getMessage().contains("The high score is tied at 205."), "Must have tie message");

        // Next round
        game.nextRound();
        assertFalse(game.isRoundOver(), "New round started");

        // In this round, Player 3 leapfrogs by getting 30 points
        p3.hand.add(new Card("number", 10, "10"));
        p3.hand.add(new Card("number", 10, "10")); // just points simulation
        p3.hand.add(new Card("modifier", 10, "+10"));
        // p1 and p2 bust (score 0)
        p1.busted = true;
        p2.busted = true;

        endRoundM.invoke(game, "End leapfrog round.");
        assertTrue(game.isGameOver(), "Game must be over after leapfrog");
        assertEquals(210, p3.score, "Player 3 must have 180 + 30 = 210");
        assertTrue(game.getMessage().contains("Player 3 wins with 210 points!"),
                "Player 3 leapfrogged and won: " + game.getMessage());

        System.out.println("PASSED");
    }

    private static void testSimulatedFullGames(int count) throws Exception {
        System.out.print("Test 11: Simulating " + count + " full automated games... ");
        Random rng = new Random(42);

        for (int g = 0; g < count; g++) {
            Flip7Game game = new Flip7Game();
            int moveCount = 0;
            int maxMoves = 25000;

            int prevTotal = countTotalCards(game);
            while (!game.isGameOver() && moveCount < maxMoves) {
                moveCount++;
                String actionDesc = "";

                if (game.isFlippingThree()) {
                    actionDesc = "forced hit() by " + game.getCurrentPlayer();
                    game.hit();
                } else if (game.isWaitingForTarget()) {
                    // Pick a random eligible target
                    ArrayList<Integer> eligible = new ArrayList<Integer>();
                    for (int i = 0; i < game.getPlayerCount(); i++) {
                        if (game.isEligibleTarget(i)) eligible.add(i);
                    }
                    if (eligible.isEmpty()) {
                        throw new AssertionError("Waiting for target but no eligible targets found!");
                    }
                    int target = eligible.get(rng.nextInt(eligible.size()));
                    actionDesc = "chooseTarget(" + target + ")";
                    game.chooseTarget(target);
                } else if (game.isRoundOver()) {
                    actionDesc = "nextRound()";
                    game.nextRound();
                } else {
                    // Decide hit or stay
                    if (rng.nextBoolean()) {
                        actionDesc = "hit() by " + game.getCurrentPlayer();
                        game.hit();
                    } else {
                        actionDesc = "stay() by " + game.getCurrentPlayer();
                        game.stay();
                    }
                }

                int totalCards = countTotalCards(game);
                if (totalCards != prevTotal) {
                    System.out.println("DEBUG: Move " + moveCount + " (" + actionDesc + ") changed card count from "
                            + prevTotal + " to " + totalCards + ". Message: " + game.getMessage());
                    throw new AssertionError("Card conservation broken at move " + moveCount +
                            " in game " + g + ": total changed from " + prevTotal + " to " + totalCards);
                }
            }

            assertTrue(game.isGameOver(), "Game " + g + " did not terminate within " + maxMoves + " moves");
            int winningScore = -1;
            for (int p = 0; p < game.getPlayerCount(); p++) {
                if (game.getPlayer(p).score > winningScore) winningScore = game.getPlayer(p).score;
            }
            assertTrue(winningScore > 200, "Winning score must be strictly > 200, was: " + winningScore);
            assertEquals(94, countTotalCards(game), "Final card count must be 94");
        }
        System.out.println("PASSED (" + count + " games completed flawlessly)");
    }
}

