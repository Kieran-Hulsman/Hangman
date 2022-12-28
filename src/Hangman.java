/**
 * Kieran Hulsman
 * ICS4U1-1B
 * Ms. Lai
 * Sunday, 11-April 2021
 * Culminating Final Project
 * 
 * DESCRIPTION:
 * This program is a hangman game. The user has a maximum of 6 incorrect guesses per round. The phrases that the 
 * user has to guess are all related to computer science, and are stored in an encrypted text file. The program 
 * also has animated graphics and sound effects for a more engaging UX.
 */

// imports
import java.util.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.Class;
import javax.sound.sampled.*;

/**
 * Name: Hangman (driver class)
 * Purpose: Controls the game mechanics
 */
public class Hangman 
{
    // Many of the methods related to the game mechanics operate on the same variables, so declaring them as
    // static allows the methods to access them directly, rather than having them passed as parameters. 
    // Doing this makes the operations in the main method easier to follow.
    static ArrayList<Character> guessesSorted = new ArrayList<>();
    static ArrayList<Character> guessesUnsorted = new ArrayList<>();
    static ArrayList<Character> bad = new ArrayList<>();
    static String[][] phrases;
    static Letter[] phrase;
    static int phraseNum = 0;
    static GameStatus game = new GameStatus();
    static Scanner in = new Scanner (System.in);

    /**
     * Main method
     */
    public static void main(String[] args) {      

        // relative path of phrases text file
        final String PHRASES_PATH = "src/phrases.txt";

        // generates encryption
        Encryption keys = new Encryption();

        // decrypts phrases
        phrases = Encryption.decryptFile(PHRASES_PATH);
        shufflePhrases();

        // generates a new set of keys
        keys = keys.generateKeys();

        // re-encrypts phrases
        Encryption.sendKeys(keys);
        Encryption.encryptFile(keys, PHRASES_PATH, phrases);

        // title screen
        Sound.play("Startup");
        GameStatus.titleScreen();

        outputInstructions();
        String name = getName();

        // hangman game
        do {
            newGame(name);
            do {
                outputStatus();
                char guess = getGuess();
                updateGame(guess);
            } while (!isOver());
        } while (isPlayAgain(game.endScreen(phrases[0][phraseNum-1], name)));
    }

    /**
     * Name: outputInstructions
     * Purpose: outputs the instructions to the user
     * Parameters: none
     * Returns: void
     */
    public static void outputInstructions () {
        String instructions = 
            "\nINSTRUCTIONS:"
        + "\n\nHi there! Welcome to my hangman game. For this program, you will have to guess a word or" 
        +   "\nphrase related to computer science. You will have a maximum of six incorrect guesses. If you" 
        +   "\nmake seven incorrect guesses, you will lose."
        + "\n\nBefore you start the game, the program will prompt you to enter your name. It will then use"
        +   "\nthe letters of your name to make the hangman graphic."
        + "\n\nThe graphics are quite large, so ensure that your console is as big as possible. Also, make"
        +   "\nsure your volume is turned on, and you've given Java permission to access your computer's" 
        +   "\nspeakers."
        + "\n\nGood luck!";
        
        Animation.clearConsole();
        outputGap();
        Colour.print(instructions + "\n", "blue dark");
    }

    /**
     * Name: outputGap
     * Purpose: outputs a gap, making the prompts easier for the user to follow
     * Parameters: none
     * Returns: void
     */
    public static void outputGap () {
        char c = '=';
        System.out.print(Colour.getEscape("blue dark"));
        
        for (int i = 0; i < 100; i++) {
            System.out.print(c);
        }
        Colour.reset();
    }

    /**
     * Name: invalidInput
     * Purpose: outputs a given error message to the user, and plays a sound effect
     * Parameters: String
     * Returns: void
     */
    public static void invalidInput (String msg) {
        Sound.play("Incorrect");

        // game is in progress
        if (game.midGame) {
            outputStatus();
        }
        // game hasn't started
        else if (game.badGuesses == 0) {
            Animation.clearConsole();
            outputInstructions();
        }
        
        outputGap();
        Colour.print(String.format("\nINVALID - %s\n", msg), "red");
    }

    /**
     * Name: getName
     * Purpose: gets the user's name (used to make up the characters in the gallows graphic)
     * Parameters: none
     * Returns: String
     */
    public static String getName () {
        boolean valid = false;
        String s = null;
        while (!valid) {

            outputGap();
            System.out.print("\nEnter your name: ");
            s = in.nextLine().trim().toLowerCase();

            final int MIN = 3;
            final int MAX = 9;

            if (s.isEmpty()) {
                invalidInput("input is required");
            }
            else if (s.indexOf(' ') != -1) {
                invalidInput("your name cannot have any spaces in it");
            }
            else if (s.length() < MIN) {
                invalidInput(String.format("your name must have at least %s letters in it", MIN));
            }
            else if (s.length() > MAX) {
                invalidInput(String.format("your name can't have more than %s letters in it", MAX));
            }
            else {
                valid = true;
                for (int i = 0; i < s.length(); i++) {
                    if (s.charAt(i) < 97 || s.charAt(i) > 122) {
                        invalidInput("your name can only have letters in it");
                        valid = false;
                        break;
                    }
                }
            }
        }
        return s; 
    }

    /**
     * Name: shufflePhrases
     * Purpose: shuffles the order of the phrases array before they're re-encrypted and send back to the phrases
     * text file (shuffling the order makes decypting the phrases more difficult)
     * Parameters: none
     * Returns: void
     */
    public static void shufflePhrases () {
        List<String> list = Arrays.asList(phrases[0]);
        Collections.shuffle(list);
        list.toArray(phrases[0]);
    }

    /**
     * Name: newGame
     * Purpose: resets the variables storing the data from the previous round if the user wants to play again
     * Parameters: String
     * Returns: void
     */
    public static void newGame (String name) {

        if (phraseNum == phrases[0].length) {
            phraseNum = 0;
        }
            phrase = Letter.convert(phrases[0][phraseNum]);

        ArrayList<Character> temp1 = new ArrayList<>();
        guessesSorted = temp1;

        ArrayList<Character> temp2 = new ArrayList<>();
        bad = temp2;

        game.newGame(name);
        phraseNum++;
    }

    /**
     * Name: getGuess
     * Purpose: gets the user's guess
     * Parameters: none
     * Returns: char
     */
    public static char getGuess() { 
        char c = '1'; // will act as a tracer if something goes wrong
        boolean valid = false;

        do {
            // tells the user how many bad guesses they have left
            int remaining = GameStatus.maxBadGuesses - game.badGuesses;
            String remainingMsg;
            
            switch (remaining) {
                case 0:
                    remainingMsg = "You don't have anymore incorrect guesses left.";
                    break;
                case 1: 
                    remainingMsg = String.format("You have %s incorrect guess left.", remaining);
                    break;
                default:    
                    remainingMsg = String.format("You have %s incorrect guesses left.", remaining);
            }

            outputGap();
            System.out.print(String.format("\n%s Enter your guess: ", remainingMsg));
            String s = in.nextLine().trim().toUpperCase();

            switch (s.length()) {
                case 0:
                    invalidInput("input is required");
                    break;

                case 1:
                    c = s.charAt(0);

                    if (c >= 65 && c <= 95) {
                        if (isAlreadyGuessed(c)) {
                            invalidInput(String.format("you've already guessed \'%s\'", c));
                        }
                        
                        else {
                            addElement(guessesSorted, c);
                            guessesUnsorted.add(c);
                            valid = true;
                        }
                    }
                    else {
                        invalidInput("please enter a letter");
                    }
                    break;
                
                default:
                    invalidInput("only enter one character");
            }
        } while (!valid);
        outputGap();
        return c;
    }

    /**
     * Name: addElement
     * Purpose: adds a given element to a given sorted ArrayList, inserting the element into the correct index
     * position
     * Parameters: ArrayList<Character>, char
     * Returns: void
     */
    public static void addElement (ArrayList<Character> a, char c) {
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) > c) {
                a.add(i, c);
                return;
            }
        }
        // new guess is the largest alphabetically
        a.add(c);
    }

    /**
     * Name: isAlreadyGuessed
     * Purpose: determines whether or not the user has already guessed a given letter
     * Parameters: char
     * Returns: boolean
     */
    public static boolean isAlreadyGuessed (char c)
    {
        int bottom = 0;
        int top = guessesSorted.size() - 1;
        int middle;

        while (bottom <= top) {
            middle = (bottom + top)/2;
            if (guessesSorted.get(middle) == c) {
                return true;
            }
            else if (guessesSorted.get(middle) < c) {
                bottom = middle + 1;
            }
            else {
                top = middle - 1;
            }
        }
        return false;
    }

    /**
     * Name: updateGame
     * Purpose: updates the variables storing the user's progress with their most recent guess
     * Parameters: char
     * Returns: void
     */
    public static void updateGame (char c) {
        boolean correct = false;
        for (int i = 0; i < phrase.length; i++) {
            if (phrase[i].actual == c) {
                phrase[i].visible = phrase[i].actual;
                correct = true;
            }
        }

        if (!correct) {
            game.badGuesses++;
            addElement(bad, c);
        }

        if (!isOver()) {
            if (correct) {
                Sound.play("Correct");
            }
            else {
                Sound.play("Incorrect");
            }
        }
    }

    /**
     * Name: outputStatus
     * Purpose: outputs the gallows graphic, semi-completed phrases and incorrect guesses after each of the 
     * user's guesses
     * Parameters: none
     * Returns: void
     */
    public static void outputStatus () {

        // gallows
        Animation.clearConsole();
        System.out.println(game.setGallows());
        outputGap();
        
        // phrase
        Colour.print("\nPHRASE:\n", "green dark");
        for (int i = 0; i < phrase.length; i++) {
            
            // if the most recent guess was correct, outputs it in green
            if (guessesUnsorted.size() > 0 
            && phrase[i].visible == guessesUnsorted.get(guessesUnsorted.size()-1)) {
                Colour.print(String.valueOf(phrase[i].visible), "green dark");
            }
            else {
                System.out.print(phrase[i].visible);
            }

            if (i < phrase.length-1) {
                System.out.print(' ');
            }
        }
        System.out.println();

        // bad guesses
        if (!bad.isEmpty()) {
            Colour.print("\n\nINCORRECT GUESSES:\n", "red");
            for (int i = 0; i < bad.size(); i++) {

                // if the most recent guess was incorrect, outputs it in red
                if (bad.get(i) == guessesUnsorted.get(guessesUnsorted.size()-1)) {
                    Colour.print(String.valueOf(bad.get(i)), "red");
                }
                else {
                    System.out.print(bad.get(i));
                }

                if (i < bad.size()-1) {
                    System.out.print(' ');
                }
            }
            System.out.println();
        }
    }

    /**
     * Name: isOver
     * Purpose: determines whether or not the round is over
     * Parameters: none
     * Returns: void
     */
    public static boolean isOver () {
        boolean over = true;
        boolean won = true;
        
        // user has run out of guesses (they've lost)
        if (game.badGuesses > GameStatus.maxBadGuesses) {
            over = true;
            won = false;
        }

        // user has guessed the phrase (they've won)
        else {
            for (int i = 0; i < phrase.length; i++) {
                if (phrase[i].visible == Letter.hidden) {
                    won = false;
                }
            }

            over = won;
        }

        if (over) {
            game.midGame = false;

            if (won) {
                Sound.play("Win");
            }
            else {
                Sound.play("Lose");
            }
        }
        return over;
    }

    /**
     * Name: isPlayAgain
     * Purpose: determines whether or not the user wants to play another round
     * Parameters: String
     * Returns: boolean
     */
    public static boolean isPlayAgain (String endScreen) {
        String s;

        while (true) {
            outputGap();
            System.out.print("\nUSER MENU\n1... Play another round\n2... Exit program\n\nEnter your choice: ");
            s = in.nextLine().trim();

            // play again
            if (s.equals("1")) {
                Sound.play("Reset");
                return true;
            }

            // exit
            else if (s.equals("2")) {
                Sound.play("Ending");
                outputGap();
                System.out.print("\nHave a great day!\n");
                outputGap();
                System.out.println();
                return false;
            }

            // invalid
            else {
                Animation.clearConsole();
                System.out.println(endScreen);
                if (s.isEmpty()) {
                    invalidInput("input is required");
                }
                else {
                    invalidInput("please enter \"1\" or \"2\"");
                }
            }
        }
    }
}

/**
 * Name: Encryption
 * Purpose: Controls the encryption and decryption of the phrases
 */
class Encryption
{
    public String a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z,sp,br1,br2;

    private static String[][] fieldArray = {
        {"a","A"}, {"b","B"}, {"c", "C"}, {"d","D"}, {"e","E"}, {"f","F"}, {"g","G"}, {"h","H"}, {"i","I"}, 
        {"j","J"}, {"k","K"}, {"l","L"}, {"m","M"}, {"n","N"}, {"o","O"}, {"p","P"}, {"q","Q"}, {"r","R"}, 
        {"s","S"}, {"t","T"}, {"u","U"}, {"v","V"}, {"w","W"}, {"x","X"}, {"y","Y"}, {"z","Z"}, {"sp"," "}, 
        {"br1","+"}, {"br2","-"}
    };

    private static final int KEY_LEN = 100;
    private static final int LINE_LEN = 100;
    
    static String secretsPath = "src/secrets.txt";

    /**
     * Name: generateKeys
     * Purpose: generates a new set of random encryption keys
     * Parameters: none
     * Returns: Encryption
     */
    Encryption generateKeys () {

        Encryption keys = new Encryption();
        try {
            Class<?> cls = Class.forName("Encryption");
            for (int i = 0; i < fieldArray.length; i++) {
                Field fld = cls.getField(fieldArray[i][0]);
                fld.set(keys, createKey());
            }
        }
        catch (Throwable e) {e.printStackTrace();}
        return keys;
    }

    /**
     * Name: createKey
     * Purpose: creates each individual encryption key
     * Parameters: none
     * Returns: String
     */
    private static String createKey () {
        String key = "";
        for (int i = 0; i < KEY_LEN; i++) {       
            String abt = 
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890!@#$%^&*()~`_={}[]<>|:;,./?";

            int index = (int)(Math.random() * abt.length());
            String randLetter = String.valueOf(abt.charAt(index));
            key += randLetter;
        }
        return key;
    }

    /**
     * Name: getRow
     * Purpose: gets the row of a given item in the fieldArray using a sequential search algorithm
     * Parameters: String
     * Returns: int
     */
    private static int getRow (String item) {
        int index = 0;
        for (int i = 0; i < fieldArray.length; i++) {
            if (fieldArray[i][0].equals(item)) {
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * Name: getContents
     * Purpose: gets the contents of a given text file
     * Parameters: String
     * Returns: String
     */
    private static String getContents (String path) {    
        File file = new File (path);
        String contents = "";
        try {
            Scanner reader = new Scanner (file);
            
            while (reader.hasNextLine()) {
                contents += reader.nextLine();
            }
            reader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return contents;
    }

    /**
     * Name: writeFile
     * Purpose: writes to a given message to a given text file
     * Parameters: String, String
     * Returns: void
     */
    static void writeFile (String path, String contents) {
        try {
            File file = new File (path);
            FileWriter writer = new FileWriter (file);
            for (int low=0,high=LINE_LEN; low < contents.length(); low+=LINE_LEN, high+=LINE_LEN) {
                if (high < contents.length()-1) {
                    writer.write(contents.substring(low,high));
                    writer.write("\n");
                }
                else {
                    writer.write(contents.substring(low));
                }
            }
            writer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Name: encryptFile
     * Purpose: encrypts a given text file with a given message
     * Parameters: Encryption, String, String
     * Returns: void
     */
    static void encryptFile (Encryption key, String path, String[][] deArr) {

        // converts decrypted msg to char array
        char[][][] deArr2 = new char [deArr.length][][];
        for (int row = 0; row < deArr2.length; row++) {
            deArr2[row] = new char [deArr[row].length][];
            
            for (int col = 0; col < deArr2[row].length; col++) {
                deArr2[row][col] = deArr[row][col].toCharArray();
            }
        }

        // encrypts message
        String enStr = "";
        try {
            Class<?> cls = Class.forName("Encryption");
            for (int vol = 0; vol < deArr2.length; vol++) {
                for (int row = 0; row < deArr2[vol].length; row++) {   
                    for (int col = 0; col < deArr2[vol][row].length; col++) {
                        for (int i = 0; i < fieldArray.length; i++) {
                            if (String.valueOf(deArr2[vol][row][col]).equals(fieldArray[i][1])) {
                                enStr += cls.getField(fieldArray[i][0]).get(key);
                            }
                        }
                    }
                    if (row != deArr2[vol].length-1) {
                        enStr += cls.getField(fieldArray[getRow("br1")][0]).get(key);
                    }
                }
                if (vol != deArr2.length-1) {
                    enStr += cls.getField(fieldArray[getRow("br2")][0]).get(key);
                }
            }
        }
        catch (Throwable e) {e.printStackTrace();}

        // sends encrypted message to text fiel
        writeFile(path, enStr);
    }

    /**
     * Name: decryptFile
     * Purpose: decrypts a given text file
     * Parameters: String
     * Returns: String[][]
     */
    static String[][] decryptFile (String path) { 

        // gets the encryption keys that were created the last time the program ran
        Encryption oldKey = new Encryption();
        String keysStr = getContents(secretsPath);
        StringTokenizer st = new StringTokenizer (keysStr, fieldArray[getRow("br1")][1]);    

        int oldKeyLen = Integer.parseInt(st.nextToken());
        try {
            Class<?> cls = Class.forName("Encryption");
            
            for (int i = 0; st.hasMoreTokens(); i++) {
                Field fld = cls.getField(fieldArray[i][0]);
                fld.set(oldKey, st.nextToken());
            }
        }
        catch (Throwable e) {e.printStackTrace();}

        // gets the encrypted msg
        String enStr = getContents(path);

        // gets the delimiters
        for (int low=0, high=oldKeyLen; high <= enStr.length(); low+=oldKeyLen, high+=oldKeyLen) {
            if (enStr.substring(low,high).equals(oldKey.br1)) {
                String delim = "";
                for (int i = 0; i < oldKeyLen; i++) {
                    delim += fieldArray[getRow("br1")][1];
                }
                enStr = enStr.substring(0,low) + delim + enStr.substring(high);
            }
            else if (enStr.substring(low,high).equals(oldKey.br2)) {

                String delim = "";
                for (int i = 0; i < oldKeyLen; i++) {
                    delim += fieldArray[getRow("br2")][1];
                }
                enStr = enStr.substring(0,low) + delim + enStr.substring(high);
            }
        }

        // separates the full encrypted message into each encypted character
        String[] enArr1 = new String [enStr.length()];
        StringTokenizer st1 = new StringTokenizer (enStr, fieldArray[getRow("br2")][1]);
        int br2s = 0;
        for (br2s = 0; st1.hasMoreTokens(); br2s++) {
            enArr1[br2s] = st1.nextToken();
        }

        String[] temp1 = new String [br2s];
        for (int i = 0; i < enArr1.length && i < temp1.length; i++) {
            temp1[i] = enArr1[i];
        }
        enArr1 = temp1;
        
        String[][] enArr2 = new String[enArr1.length][enStr.length()];
        for (int i = 0; i < enArr2.length; i++) {
            
            StringTokenizer st2 = new StringTokenizer(enArr1[i], fieldArray[getRow("br1")][1]);
            int br1s = 0;
            
            for (br1s = 0; st2.hasMoreTokens(); br1s++) {
                enArr2[i][br1s] = st2.nextToken();
            }

            String[] temp2 = new String[br1s];
            for (int j = 0; j < enArr2[i].length && j < temp2.length; j++) {
                temp2[j] = enArr2[i][j];
            }
            enArr2[i] = temp2;
        }

        // decrypts the message
        String[][] deArr1 = new String [enArr2.length][];
        try {
            Class<?> cls = Class.forName("Encryption");
            
            for (int row = 0; row < enArr2.length; row++) {
                deArr1[row] = new String [enArr2[row].length];
                
                for (int col = 0; col < enArr2[row].length; col++) {
                    deArr1[row][col] = "";

                    for (int i=0, low=0, high=oldKeyLen; i < enArr2[row][col].length()/oldKeyLen; i++, low+=oldKeyLen, high+=oldKeyLen) {
                        String enWord = enArr2[row][col].substring(low,high);

                        for (int j = 0; j < fieldArray.length; j++) {
                            if (enWord.equals(cls.getField(fieldArray[j][0]).get(oldKey))) {
                                deArr1[row][col] += fieldArray[j][1];
                                break;
                            }
                        }
                    }
                }
            }
        }
        catch (Throwable e) {e.printStackTrace();}
        return deArr1;
    }
    
    /**
     * Name: sendKeys
     * Purpose: sends a given set of encryption keys to the secrets text file
     * Parameters: Encryption
     * Returns: void
     */
    static void sendKeys (Encryption keys) {

        String msg = String.valueOf(KEY_LEN);
        for (int i = 0; i < fieldArray.length; i++) {
            try {
                Class<?> cls = Class.forName("Encryption");
                msg += fieldArray[getRow("br1")][1] + cls.getField(fieldArray[i][0]).get(keys);
            }
            catch (Throwable e) {e.printStackTrace();}
        }
        writeFile(secretsPath, msg);
    }

    /**
     * Name: getKeys
     * Purpose: gets the set of encryption keys that were created last time the program was run
     * Parameters: none
     * Returns: String
     */
    static String[] getKeys () {

        String keysStr = getContents(secretsPath);
        StringTokenizer st = new StringTokenizer (keysStr, fieldArray[getRow("br1")][1]);
        
        String[] keysArr = new String [fieldArray.length+1]; // +1 because of keyLen

        for (int i = 0; st.hasMoreTokens(); i++) {
            keysArr[i] = st.nextToken();
        }
        return keysArr;
    }
}

/**
 * Name: Letter
 * Purpose: Used in the hangman game for better object oriented programming. Has two main attributes - one to 
 * store the actual letter in the phrase, the other to store the character in the phrase that's outputted to the
 * user (dependent on whether or not the user has guessed that letter).
 */
class Letter 
{
    char actual, visible;
    static char hidden = '_';

    /**
     * Name: convert
     * Purpose: converts a given phrase to an array of Letter objects
     * Parameters: String
     * Returns: Letter[]
     */
    static Letter[] convert (String s) {
        char[] chArr = s.toCharArray();
        Letter[] ltrArr = new Letter [chArr.length];

        for (int i = 0; i < ltrArr.length; i++) {

            Letter ltr = new Letter();
            ltr.actual = chArr[i];

            if (ltr.actual == ' ') {
                ltr.visible = ltr.actual;
            }
            else {
                ltr.visible = hidden;
            }
            ltrArr[i] = ltr;
        }
        return ltrArr;
    }
}

/**
 * Name: Colour (superclass of Animation)
 * Purpose: Uses ANSI escape sequences to control the colour of the output
 */
class Colour
{
    // reset
    private static final int RESET = 0;

    // attributes
    private static final int BRIGHT = 1;
    private static final int DARK = 2;

    // colours
    private static final int BLACK = 30;
    private static final int RED = 31;
    private static final int GREEN = 32;
    private static final int YELLOW = 33;
    private static final int BLUE = 34;
    private static final int MAGENTA = 35;
    private static final int CYAN = 36;
    private static final int WHITE = 37;

    // format of ANSI escape sequences
    private static String base = "\033[%sm";
    
    /**
     * Name: getEscape
     * Purpose: gets the ANSI escape sequence of a given colour
     * Parameters: String
     * Returns: String
     */
    protected static String getEscape (String clr) {
        String escape;

        StringTokenizer st = new StringTokenizer(clr);
        int code1 = getCode(st.nextToken());

        // colour
        if (st.countTokens() == 0) {
            escape = String.format(base, code1);
        }

        // colour + attribute
        else {
            int code2 = getCode(st.nextToken());
            escape = String.format(base, String.format("%s;%s", code1, code2));
        }
        return escape;
    }

    /**
     * Name: getCode
     * Purpose: gets the ANSI code for a given colour
     * Parameters: String
     * Returns: int
     */
    protected static int getCode (String clr) {
        clr = clr.trim().toLowerCase();
        int value = 0;
        switch (clr) {
            case "reset":
                value = RESET;
                break;
            case "bright":
                value = BRIGHT;
                break;
            case "dark":
                value = DARK;
                break;
            case "black":
                value = BLACK;
                break;
            case "red":
                value = RED;
                break;
            case "green":
                value = GREEN;
                break;
            case "yellow":
                value = YELLOW;
                break;
            case "blue":
                value = BLUE;
                break;
            case "magenta":
                value = MAGENTA;
                break;
            case "cyan":
                value = CYAN;
                break;
            case "white":
                value = WHITE;
        }
        return value;
    }

    /**
     * Name: reset
     * Purpose: resets the output colour to the system default (for NetBeans, default is be black)
     * Parameters: none
     * Returns: void
     */
    static void reset () {
        System.out.print(String.format(base, RESET));
    }    

    /**
     * Name: print
     * Purpose: ouputs a given message in a given colour, then resets the output colour
     * Parameters: String, String
     * Returns: void
     */
    static void print (String s, String clr) {

        // gets ANSI escape sequence
        String escape = getEscape(clr);

        // prints string 1 char at a time
        for (int i = 0; i < s.length(); i++) {
            System.out.print(escape);
            System.out.print(s.charAt(i));
        }
        reset();
    }
}

/**
 * Name: Animation (subclass of Colour)
 * Purpose: Controls the animated title/end screen
 */
class Animation extends Colour 
{
    String img;

    // colours will be the system default (usually black) unless specified otherwise
    String lower = "reset";
    String upper = "reset";
    String bf1 = "reset";
    String bf2 = "reset";

    // literal char
    static final char LIT = '*';

    /**
     * Name: clearConsole
     * Purpose: clears the console by outputting blank lines until the previous output is no longer visible
     * Parameters: none
     * Returns: void
     */
    static void clearConsole () {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    } 

    /**
     * Name: animationStr
     * Purpose: stylizes a given string by inserting ANSI escape sequences, so that it will be outputted in
     * colour 
     * Parameters: String
     * Returns: String
     */
    private String animationStr (String s) {
        String output = "";

        // uses ANSI escape sequences
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            String recent = "";
                // without keeping track of the most recent, the print statement is too slow for netbeans

            // literal character
            // means the next char should be outputted in the same colour the lower case letters
            if (c == LIT) {
                s = GameStatus.swapChar(s, i);
                output += getEscape(lower);
            }

            // a-z
            else if (!recent.equals(lower) && (c >= 97 && c <= 122)) {
                output += getEscape(lower);
                recent = lower;
            }

            // A-Z
            else if (!recent.equals(upper) && (c >= 65 && c <= 90)) {
                output += getEscape(upper);
                recent = upper;
            }

            // big font - foreground
            else if (!recent.equals(bf1) && c == '█') {
                output += getEscape(bf1);
                recent = bf1;
            }

            // big font - background
            else if (!recent.equals(bf2) && (c != '\n' && c != ' ' && c != ':')) {
                output += getEscape(bf2);
                recent = bf2;
            }
            output += s.charAt(i);
        }
        return output;
    }

    /**
     * Name: output
     * Purpose: clears the console, outputs a given stylized string, and then adds a delay
     * Parameters: String
     * Returns: void
     */
    private static void output (String s) {
        try {
            clearConsole();
            System.out.println(s);
            Thread.sleep(500);  
        } catch (InterruptedException e) {}
    }

    /**
     * Name: rainbow
     * Purpose: outputs an animated graphic for a given amount of time, stylized with a given set of colours
     * Parameters: String[], int
     * Returns: void
     */
    void rainbow (String[] clrs, int secs) {
        double start = System.currentTimeMillis();
        double end = start + (secs * 1000);

        // needed for efficiency of output
        String[][] blink = new String [clrs.length][];
        for (int i = 0; i < blink.length; i++) {
            bf1 = clrs[i];
            bf2 = clrs[i];

            blink[i] = getBlink();
        }
        
        for (int i = 0; System.currentTimeMillis() < end; i++) {
            animate(blink[i], 1);

            if (i == clrs.length-1) {
                i = 0;
            }
        }
    }

    /**
     * Name: getBlink
     * Purpose: removes the big font from a graphic, so that the text will look like it's flashing when the 
     * animation plays
     * Parameters: none
     * Returns: String[]
     */
    String[] getBlink () {

        // need to define both, as it will destroy the original img otherwise
        String img1 = img;
        String img2 = img;

        // removing all the big font
        for (int i = 0; i < img.length(); i++) {
            char c = img.toLowerCase().charAt(i);

            // literals are dealt with afterwards in animation() method
            if (c == LIT) {
                i++;
            }
            else if ((c < 97 || c > 122) && c != '\n') {
                img1 = GameStatus.swapChar(img1, ' ', i);
            }
        }

        img1 = animationStr(img1);
        img2 = animationStr(img2);

        String[] blink = {img1, img2};
        return blink;
    }

    /**
     * Name: animate
     * Purpose: animates a given graphic for a given amount of time
     * Parameters: int, String
     * Returns: String
     */
    String animate (String[] blink, int secs) {

        // timer
        double start = System.currentTimeMillis();
        double end = start + (secs * 1000);

        while(System.currentTimeMillis() < end) {
            output(blink[0]);
            output(blink[1]);
        }
        return blink[1];
    }
}

/**
 * Name: GameStatus
 * Purpose: Stores the user's progress for each round of the hangman game
 * Note: Also stores the hard-coded graphics for the game, and is where the title/end screen animations are called
 */
class GameStatus
{
    boolean midGame;
    String letters = "hangman"; // the gallows in the title screen will be made up of the word "hangman"
    int badGuesses;

    private char graphicChar = 'c';
    private char nameChar = 'd';

    static int maxBadGuesses = 6;

    private String gallowsGraphic = 
          "\n           00000000000000000000000000000000000000000000000"
        + "\n           0000          000000                       0000"
        + "\n           000         00000                           000"
        + "\n           000       00000                             000"
        + "\n           000     00000                               000"
        + "\n           000  00000                                  000"
        + "\n           00000000                                    000"
        + "\n           000000                                    111111                 ██╗  ██╗██╗███████╗██████╗  █████╗ ███╗  ██╗██╗ ██████╗"
        + "\n           0000                                  111111111111111            ██║ ██╔╝██║██╔════╝██╔══██╗██╔══██╗████╗ ██║╚█║██╔════╝"
        + "\n           000                                 11111         11111          █████═╝ ██║█████╗  ██████╔╝███████║██╔██╗██║ ╚╝╚█████╗"
        + "\n           000                                1111             1111         ██╔═██╗ ██║██╔══╝  ██╔══██╗██╔══██║██║╚████║    ╚═══██╗"
        + "\n           000                                111               111         ██║ ╚██╗██║███████╗██║  ██║██║  ██║██║ ╚███║   ██████╔╝"
        + "\n           000                                111               111         ╚═╝  ╚═╝╚═╝╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚══╝   ╚═════╝"
        + "\n           000                                1111             1111     ██╗  ██╗ █████╗ ███╗  ██╗ ██████╗ ███╗   ███╗ █████╗ ███╗  ██╗"
        + "\n           000                                 11111         11111      ██║  ██║██╔══██╗████╗ ██║██╔════╝ ████╗ ████║██╔══██╗████╗ ██║"
        + "\n           000                                    1111111111111         ███████║███████║██╔██╗██║██║  ██╗ ██╔████╔██║███████║██╔██╗██║"
        + "\n           000                                        11111             ██╔══██║██╔══██║██║╚████║██║  ╚██╗██║╚██╔╝██║██╔══██║██║╚████║"
        + "\n           000                                         222              ██║  ██║██║  ██║██║ ╚███║╚██████╔╝██║ ╚═╝ ██║██║  ██║██║ ╚███║"
        + "\n           000                                       4422233            ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚══╝ ╚═════╝ ╚═╝     ╚═╝╚═╝  ╚═╝╚═╝  ╚══╝"
        + "\n           000                                 4444444422233333333                    ██████╗  █████╗ ███╗   ███╗███████╗"
        + "\n           000                           4444444444    222    333333333              ██╔════╝ ██╔══██╗████╗ ████║██╔════╝"
        + "\n           000                          44444          222          3333             ██║  ██╗ ███████║██╔████╔██║█████╗"
        + "\n           000                                         222                           ██║  ╚██╗██╔══██║██║╚██╔╝██║██╔══╝"
        + "\n           000                                         222                           ╚██████╔╝██║  ██║██║ ╚═╝ ██║███████╗"
        + "\n           000                                         222                            ╚═════╝ ╚═╝  ╚═╝╚═╝     ╚═╝╚══════╝"
        + "\n           000                                         222"
        + "\n           000                                        65555"
        + "\n           000                                      666655555"
        + "\n           000                                    66666   55555"
        + "\n           000                                  66666       55555"
        + "\n           000                                66666           55555"
        + "\n           000                                666               555"
        + "\n           0000"
        + "\n0000000000000000000000000\n";

    /**
     * Name: swapChar (overload form 1)
     * Purpose: replaces the char at a given index position with another given char in a given String
     * Parameters: String, char, int
     * Returns: String
     */
    static String swapChar (String s, char c, int index) {
        return s.substring(0, index) + c + s.substring(index+1);
    }

    /**
     * Name: swapChar (overload form 2)
     * Purpose: deletes the char at a given index position in a given String
     * Parameters: Sting, int
     * Returns: String
     */
    static String swapChar (String s, int index) {
        return s.substring(0, index) + s.substring(index+1);
    }

    /**
     * Name: titleScreen
     * Purpose: outputs the title screen animation
     * Parameters: none
     * Returns: void
     */
    static void titleScreen () {

        GameStatus title = new GameStatus();
        title.midGame = false;
        title.badGuesses = 6;

        Animation gallows = new Animation();
        gallows.img = title.setGallows();
        
        String[] clrs = {"red", "yellow", "green", "cyan", "blue", "magenta", "magenta dark"};
        gallows.rainbow(clrs, 7);
    }

    /**
     * Name: newGame
     * Purpose: resets the class attributes related to the user's progress
     * Parameters: String
     * Returns: name
     */
    void newGame (String name) {
        midGame = true;
        badGuesses = 0;
        letters = name;   
    }

    /**
     * Name: setGallows
     * Purpose: sets the characters that make up the gallows graphic to characters in the user's name
     * Parameters: none
     * Returns: String
     */
    String setGallows () {

        String s = gallowsGraphic;
        for (int i = 0; i < s.length(); i++) {
            try {
                if (Integer.parseInt(String.valueOf(s.charAt(i))) <= badGuesses) {
                    s = swapChar(s, graphicChar, i);
                }
                else {
                    s = swapChar(s, ' ', i);
                }
            } catch (java.lang.NumberFormatException e) {
                if (midGame && s.charAt(i) != '\n') {
                    s = swapChar(s, ' ', i); 
                }
            }
        }
        s = changeLetters(s, letters, graphicChar);
        return s;
    }

    /**
     * Name: endScreen
     * Purpose: ouputs the end screen animation
     * Parameters: String, String
     * Returns: String
     */
    String endScreen(String phrase, String name) {
        
        String s;
        String endColour;

        // user won
        if (badGuesses <= 6) {
            letters = "winner";
            endColour = "green dark";
            s =
                  "\n            ccccccccccccccccccccccccccccccccccccccccccccccc"
                + "\n          cccccccccccccccccccccccccccccccccccccccccccccccccccc"            
                + "\n          cccc                                            cccc"            
                + "\n          cccc        ██╗   ██╗ █████╗ ██╗   ██╗          cccc"            	
                + "\n          cccc        ╚██╗ ██╔╝██╔══██╗██║   ██║          cccc"            
                + "\n ccccccccccccc         ╚████╔╝ ██║  ██║██║   ██║          ccccccccccccc"
                + "\ncccc       ccc          ╚██╔╝  ██║  ██║██║   ██║          ccc       cccc"
                + "\ncccc       cccc          ██║   ╚█████╔╝╚██████╔╝         cccc       cccc"
                + "\n cccc      cccc          ╚═╝    ╚════╝  ╚═════╝          cccc       cccc" 
                + "\n cccc       cccc      ██╗       ██╗██╗███╗  ██╗██╗       cccc       cccc"   
                + "\n  ccccc     cccc      ██║  ██╗  ██║██║████╗ ██║██║      cccc      cccc"          
                + "\n    cccc     cccc     ╚██╗████╗██╔╝██║██╔██╗██║██║      cccc     cccc"
                + "\n     ccccc   cccc      ████╔═████║ ██║██║╚████║╚═╝     cccc   ccccc"       
                + "\n       ccccc  cccc     ╚██╔╝ ╚██╔╝ ██║██║ ╚███║██╗    cccc  ccccc"             
                + "\n         cccccccccc     ╚═╝   ╚═╝  ╚═╝╚═╝  ╚══╝╚═╝   cccccccccc"           
                + "\n           ccccccccc                                ccccccccc"             
                + "\n              ccccccc                              ccccccc"                
                + "\n                  cccc                            cccc"                   
                + "\n                   cccc         865333479        cccc"                 
                + "\n                    ccccc                      ccccc"                   
                + "\n                      ccccc                  ccccc"                   
                + "\n                       cccccc            cccccc"                          
                + "\n                          cccccccccccccccccc"                             
                + "\n                              cccccccccccc"                                 
                + "\n                             cccc      cccc"                             
                + "\n                            cccc        cccc"                              
                + "\n                          ccccc         ccccc"                             
                + "\n                      cccccccccccccccccccccccccccc"                        
                + "\n                     cccccccccccccccccccccccccccccc"                       
                + "\n                     ccc                        ccc"                       
                + "\n                     ccc                        ccc"                       
                + "\n               cccccccccccccccccccccccccccccccccccccccccc"                 
                + "\n             cccccccccccccccccccccccccccccccccccccccccccccc"               
                + "\n             ccc                                        ccc"               
                + "\n             cccccccccccccccccccccccccccccccccccccccccccccc"               
                + "\n             cccccccccccccccccccccccccccccccccccccccccccccc";
        }
        
        // user lost
        else {
            letters = "loser";
            endColour = "red";
            s = 
                  "\n        cccccccccc                                                       ccccccccc"               
                + "\n       cccc    cccc                                                    ccccc   ccccc"             
                + "\n    cccccc      cccc                                                   ccc       cccccc"          
                + "\n ccccccccc      ccc                    cccccccccccccccc                cccc      cccccccc"        
                + "\ncccc          cccc              cccccccccccccccccccccccccccccc          cccc           ccc"      
                + "\ncccc       cc  ccccc        ccccccccc                     cccccccc    ccccc   cc       ccc"       
                + "\n cccccccccccccc  cccccc  ccccccc                               cccccccccc   cccccccccccccc"       
                + "\n   ccccccc  ccccc   cccccccc      ██╗   ██╗ █████╗ ██╗   ██╗       ccccc  ccccc  cccccccc"         
                + "\n               ccccc ccccc        ╚██╗ ██╔╝██╔══██╗██║   ██║        cccccccc"                     
                + "\n                 ccccccc           ╚████╔╝ ██║  ██║██║   ██║          cccc"                       
                + "\n                   cccc             ╚██╔╝  ██║  ██║██║   ██║            ccc"                      
                + "\n                  cccc               ██║   ╚█████╔╝╚██████╔╝            cccc"                     
                + "\n                  ccc                ╚═╝    ╚════╝  ╚═════╝              ccc"                     
                + "\n                  ccc         ██╗      █████╗  ██████╗███████╗██╗        ccc"                     
                + "\n                  ccc         ██║     ██╔══██╗██╔════╝██╔════╝██║        ccc"                     
                + "\n                   ccc        ██║     ██║  ██║╚█████╗ █████╗  ██║       cccc"                     
                + "\n                   cccc       ██║     ██║  ██║ ╚═══██╗██╔══╝  ╚═╝      cccc"                      
                + "\n                    ccccc     ███████╗╚█████╔╝██████╔╝███████╗██╗     cccc"                       
                + "\n                      ccccc   ╚══════╝ ╚════╝ ╚═════╝ ╚══════╝╚═╝   cccc"                         
                + "\n                       cccccc                                    cccccc"                          
                + "\n                      ccccccccccc                            cccccccc"                            
                + "\n                   ccccc   cccccccc                        cccccc ccccc"                          
                + "\n                 ccccc  ccccc   ccc        865333479       ccccccc   ccccc"                       
                + "\n              cccccc  ccccc     ccc                        ccc  ccccc  ccccc"                     
                + "\n   ccccc   cccccc  cccccc       ccc                        ccc    ccccc  ccccc  cccccccc"         
                + "\n cccccccccccccc  ccccc          cccccccccccccccccccccccccccccc      ccccc   cccccc   ccccc"       
                + "\ncccc       cc  ccccc            cccccccccccccccccccccccccccccc         ccccc  cc       ccc"       
                + "\ncccc          ccccc                                                     ccccc          ccc"       
                + "\n ccccccccc      cccc                                                   cccc      cccccccc"        
                + "\n    cccccc      cccc                                                   ccc       ccccc"           
                + "\n       cccc    cccc                                                    ccccc   ccccc"             
                + "\n        cccccccccc                                                       ccccccccc";       
        }
        
        for (int i = 0; i < s.length(); i++) {
            try {
                if (Integer.parseInt(String.valueOf(s.charAt(i))) <= name.length()) {
                    s = swapChar(s, nameChar, i);
                }
                else {
                    s = swapChar(s, ' ', i);
                } 
            } catch (java.lang.NumberFormatException e) {}
        }

        s = changeLetters(s, name.toUpperCase(), nameChar);
        s = changeLetters(s, letters, graphicChar);

        Animation end = new Animation();
        end.img = String.format("%s\n\n%sThe phrase is%s: %s", s, Animation.LIT, Animation.LIT, phrase);
        end.upper = endColour;
        end.bf1 = endColour;
        end.bf2 = endColour;

        return end.animate(end.getBlink(), 3);
    }

    /**
     * Name: changeLetters
     * Purpose: changes the letters in a given String with another set of letters
     * Parameters: String, String, char
     * Returns: String
     */
    private String changeLetters (String s, String newLetters, char c) {

        // converts 'c' to letters in the user's name (or 'hangman' if playing as guest)
        for (int i = 0, j = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                s = swapChar(s, newLetters.charAt(j), i);
                j++;
            }
            if (j == newLetters.length()) {
                j = 0;
            }
        }
        return s;
    }
}

/**
 * Name: Sound (subclass of Thread)
 * Purpose: Controls the sound effects
 */
class Sound extends Thread
{
    String path; // path of the music file

    /**
     * Name: play
     * Purpose: plays a given sound effect
     * Parameters: String
     * Returns: void
     */
    static void play (String name) {
        Sound sfx = new Sound();
        sfx.path = String.format("src/SoundEffects/%s.wav", name);
        sfx.start();
    }

    /**
     * Name: run (inherited from Thread class)
     * Purpose: starts the thread that plays music in the background
     * Parameters: none
     * Returns: void
     */
    public void run() {
        try {
            File file = new File (path);

            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(file));
            clip.start();

            Thread.sleep(clip.getMicrosecondLength()/1000);
        } catch (Exception e) {e.printStackTrace();}
    }
}
